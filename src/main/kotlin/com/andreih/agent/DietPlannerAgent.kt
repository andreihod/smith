package com.andreih.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.agent.subgraphWithVerification
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.feature.nodes.nodeLoadFromMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.andreih.agent.memory.UserSubject
import com.andreih.agent.tools.DietUserTools
import com.andreih.domain.*

object DietPlannerAgent {
    const val AGENT_NAME = "diet-planner"
    private const val MAX_REVIEW_ITERATIONS = 3

    private val systemPrompt = """
        ### IDENTITY
        You are a "Clinical Nutrition & Dietetics Specialist." Your core purpose is to translate complex nutritional
        science into actionable, health-focused dietary guidance. You serve as a bridge between metabolic requirements
        and real-world eating habits.

        ### KNOWLEDGE DOMAIN
        - Comprehensive understanding of macronutrients (Protein, Fats, Carbohydrates) and micronutrients (Vitamins, Minerals).
        - Proficiency in metabolic calculations (BMR, TDEE, Thermic Effect of Food).
        - Knowledge of diverse dietary protocols: Mediterranean, Plant-Based, Ketogenic, DASH, and High-Protein/Satiety-focused.
        - Understanding of glycemic index, insulin response, and nutrient timing.
        - Knowledge of food composition tables and nutritional values of common foods.

        ### COGNITIVE BEHAVIOR
        - **Evidence-Based:** Always prioritize peer-reviewed nutritional science over "fad" trends.
        - **Safety-First:** Flag any calorie requests or nutrient deficiencies that fall below healthy physiological thresholds.
        - **Precision:** Ensure all nutritional calculations are accurate and portions are realistic.
        - **Logical Flow:** Ensure that every recommendation correlates directly to the user's stated goals.

        ### ADAPTIVE TONE
        - Maintain a supportive, non-judgmental, and clinical tone.
        - Avoid "good" or "bad" food labels; use "nutrient-dense" or "energy-dense" instead.
        - Focus on "crowding out" (adding healthy foods) rather than just restriction.

        ### USER INFORMATION
        You have access to facts about the user loaded in your memory. Use this information to create a personalized diet plan.
        The user's preferred language and country/region are stored in memory.

        ### LOCALIZATION & CULTURAL ADAPTATION
        - **Communication Language**: ALL explanations, recommendations, and text MUST be written in the user's preferred language.
        - **Cultural Food Preferences**: Prioritize foods, ingredients, and meal patterns typical to the user's country/region.
        - **Meal Timing**: Adapt meal timing suggestions to align with cultural norms (e.g., late dinners in Spain, early dinners in USA).
        - **Portion Descriptions**: Use measurement units and portion sizes common in the user's region (e.g., cups/ounces in USA, grams/ml in Europe).
        - **Food Availability**: Recommend foods that are readily available and commonly consumed in the user's region.
        - **Culinary Context**: Reference traditional dishes or cooking methods when appropriate to increase adherence.
        - **Balanced Approach**: While respecting cultural preferences, maintain nutritional accuracy and scientific rigor.
    """.trimIndent()

    private val userProfileConcept = Concept(
        keyword = "user-profile",
        description = """
            User profile information including:
            - Name, age, height, weight, gender
            - Health goals and dietary restrictions
            - Activity level
            This information is used to create personalized diet plans.
        """.trimIndent(),
        factType = FactType.MULTIPLE
    )

    fun create(
        geminiApiKey: String,
        memoryProvider: AgentMemoryProvider,
        userTools: DietUserTools
    ): AIAgent<String, String> {
        val registry = ToolRegistry {
            tools(userTools.asTools())
        }

        val strategy = dietPlannerStrategy(userTools)

        return AIAgent(
            systemPrompt = systemPrompt,
            promptExecutor = simpleGoogleAIExecutor(geminiApiKey),
            llmModel = GoogleModels.Gemini2_5Flash,
            toolRegistry = registry,
            strategy = strategy,
            maxIterations = 200
        ) {
            install(AgentMemory) {
                this.memoryProvider = memoryProvider
                agentName = AGENT_NAME
                featureName = "diet-planning"
                organizationName = "com.andreih"
                productName = "diet-planner"
            }

            handleEvents {
                onAgentStarting {
                    println("[AGENT] Starting diet planner agent...")
                }

                onToolCallStarting { ctx ->
                    println("[TOOL] Calling '${ctx.toolName}' with args: ${ctx.toolArgs.toString().take(100)}...")
                }

                onAgentExecutionFailed { ctx ->
                    println("[AGENT] ERROR: ${ctx.throwable.message}")
                    ctx.throwable.printStackTrace()
                }

                onAgentCompleted {
                    println("[AGENT] Completed successfully")
                }

                onNodeExecutionStarting {
                    println("[NODE] ${it.node.name} - Starting node execution")
                }

                onNodeExecutionCompleted {
                    println("[NODE] ${it.node.name} - Completed successfully")
                }
            }
        }
    }

    private fun dietPlannerStrategy(userTools: DietUserTools) = strategy<String, String>("diet-planner-strategy") {
        // Storage keys for persisting state across iterations
        val assessmentKey = createStorageKey<UserAssessment>("user_assessment")
        val macrosKey = createStorageKey<MacroCalculation>("macro_calculation")
        val currentPlanKey = createStorageKey<DietPlan>("current_plan")
        val reviewIterationKey = createStorageKey<Int>("review_iteration")

        // === Load user from memory ===
        val loadUserProfile by subgraph<String, String>(tools = emptyList()) {
            val nodeLoadUserProfile by nodeLoadFromMemory<String>(
                concept = userProfileConcept,
                subject = UserSubject,
                scope = MemoryScopeType.AGENT
            )

            nodeStart then nodeLoadUserProfile then nodeFinish
        }

        // === Calculate macros and calories (code-based) ===
        val calculateMacros by node<String, MacroCalculation> { userInput ->
            val assessment = parseAssessmentFromInput(userInput)
            storage.set(assessmentKey, assessment)
            storage.set(reviewIterationKey, 0)
            val macros = calculateMacrosForUser(assessment)
            macros
        }

        // Save macros and create initial request
        val saveMacrosAndCreateRequest by node<MacroCalculation, PlanGenerationRequest> { macros ->
            storage.set(macrosKey, macros)
            PlanGenerationRequest.Initial(
                assessment = storage.getValue(assessmentKey),
                macros = macros
            )
        }

        // === Generate diet plan ===
        val generateDietPlan by subgraphWithTask<PlanGenerationRequest, DietPlan>(
            tools = emptyList()
        ) { request ->
            generateDietPlanPrompt(request)
        }

        // Save plan after generation
        val savePlan by node<DietPlan, DietPlan> { plan ->
            storage.set(currentPlanKey, plan)

            // Compress history to manage tokens
            llm.writeSession {
                replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
            }

            plan
        }

        // === Review diet plan ===
        val reviewDietPlan by subgraphWithVerification<DietPlan>(
            tools = emptyList()
        ) { plan ->
            reviewDietPlanPrompt(plan, storage.getValue(macrosKey))
        }

        // Create correction request when review fails
        val createCorrectionRequest by node<String, PlanGenerationRequest> { feedback ->
            val iteration = (storage.get(reviewIterationKey) ?: 0) + 1
            storage.set(reviewIterationKey, iteration)

            PlanGenerationRequest.Correction(
                assessment = storage.getValue(assessmentKey),
                macros = storage.getValue(macrosKey),
                previousPlan = storage.getValue(currentPlanKey),
                reviewFeedback = feedback
            )
        }

        // === Show plan to user and get feedback ===
        val showPlanToUser by node<DietPlan, String> { plan ->
            userTools.showPlanAndGetFeedback(plan.toMarkdownString())
        }

        val processUserFeedback by nodeLLMRequestStructured<UserFeedback>(
            examples = listOf(
                UserFeedback(
                    isAccepted = true,
                    message = "Looks good! I'll start this plan tomorrow."
                ),
                UserFeedback(
                    isAccepted = false,
                    message = "I don't like salmon, can you replace it with chicken? Also, I'd prefer oatmeal for breakfast."
                )
            )
        )

        // Create user revision request when feedback requests changes
        val createUserRevisionRequest by node<UserFeedback, PlanGenerationRequest> { feedback ->
            llm.writeSession {
                replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
            }

            PlanGenerationRequest.UserRevision(
                assessment = storage.getValue(assessmentKey),
                macros = storage.getValue(macrosKey),
                previousPlan = storage.getValue(currentPlanKey),
                userFeedback = feedback.message
            )
        }

        // === Format final output ===
        val formatFinalOutput by node<DietPlan, String> { plan ->
            buildString {
                appendLine(plan.toMarkdownString())
                appendLine()
                appendLine("---")
                appendLine("*Plan generated by Smith Diet Planner*")
                appendLine("*Please consult a healthcare professional before starting any new diet.*")
            }
        }

        // === EDGE DEFINITIONS ===

        // Main flow: Start → Load → Calculate → Save → Generate → Save Plan
        nodeStart then loadUserProfile then calculateMacros then saveMacrosAndCreateRequest then generateDietPlan then savePlan

        // Review loop
        edge(savePlan forwardTo reviewDietPlan)

        // Review result handling - success goes to user feedback
        edge(
            reviewDietPlan forwardTo showPlanToUser
                onCondition { result ->
                    result.successful
                }
                transformed { it.input }
        )

        // Review result handling - failure goes back to generation (if under limit)
        edge(
            reviewDietPlan forwardTo createCorrectionRequest
                onCondition { !it.successful && (storage.get(reviewIterationKey) ?: 0) < MAX_REVIEW_ITERATIONS }
                transformed { it.feedback }
        )

        // Safety exit if too many review iterations - show best effort plan
        edge(
            reviewDietPlan forwardTo showPlanToUser
                onCondition { result ->
                    val maxReached = !result.successful && (storage.get(reviewIterationKey) ?: 0) >= MAX_REVIEW_ITERATIONS
                    maxReached
                }
                transformed { it.input }
        )

        // Correction request goes back to generate
        edge(createCorrectionRequest forwardTo generateDietPlan)

        // User feedback loop
        edge(showPlanToUser forwardTo processUserFeedback)

        // User approves - go to format
        edge(
            processUserFeedback forwardTo formatFinalOutput
                transformed { it.getOrThrow().data }
                onCondition { it.isAccepted }
                transformed { storage.getValue(currentPlanKey) }
        )

        // User requests changes - go back to generate
        edge(
            processUserFeedback forwardTo createUserRevisionRequest
                transformed { it.getOrThrow().data }
                onCondition { !it.isAccepted }
        )

        // User revision goes back to generate
        edge(createUserRevisionRequest forwardTo generateDietPlan)

        // Final output goes to finish
        edge(formatFinalOutput forwardTo nodeFinish)
    }

    private fun parseAssessmentFromInput(userInput: String): UserAssessment {
        val lines = userInput.lines()
        var name = ""
        var language = UserLanguage.ENGLISH
        var country = UserCountry.INTERNATIONAL
        var age = 0
        var heightCm = 0
        var currentWeightKg = 0.0
        var gender = UserGender.MALE
        var healthGoal = UserHealthGoal.MAINTENANCE
        var dietaryRestriction = UserDietaryRestriction.NONE
        var activityLevel = UserActivityLevel.MODERATELY_ACTIVE

        for (line in lines) {
            when {
                line.contains("Name:") -> name = line.substringAfter("Name:").trim()
                line.contains("Language:", ignoreCase = true) -> {
                    val langStr = line.substringAfter(":").trim().lowercase()
                    language = UserLanguage.values().find {
                        it.displayName.lowercase().contains(langStr) || it.name.lowercase().contains(langStr)
                    } ?: UserLanguage.ENGLISH
                }
                line.contains("Country:", ignoreCase = true) -> {
                    val countryStr = line.substringAfter(":").trim().lowercase()
                    country = UserCountry.values().find {
                        it.displayName.lowercase().contains(countryStr) || it.name.lowercase().contains(countryStr)
                    } ?: UserCountry.INTERNATIONAL
                }
                line.contains("Age:") -> age = line.substringAfter("Age:").replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                line.contains("Height:") -> heightCm = line.substringAfter("Height:").replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                line.contains("Weight:") -> currentWeightKg = line.substringAfter("Weight:").replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                line.contains("Gender:") -> {
                    val genderStr = line.substringAfter("Gender:").trim().lowercase()
                    gender = if (genderStr.contains("female")) UserGender.FEMALE else UserGender.MALE
                }
                line.contains("Health Goal:") -> {
                    val goalStr = line.substringAfter("Health Goal:").trim().lowercase()
                    healthGoal = when {
                        goalStr.contains("weight loss") || goalStr.contains("lose") -> UserHealthGoal.WEIGHT_LOSS
                        goalStr.contains("muscle") || goalStr.contains("gain") -> UserHealthGoal.MUSCLE_GAIN
                        goalStr.contains("athletic") || goalStr.contains("performance") -> UserHealthGoal.ATHLETIC_PERFORMANCE
                        goalStr.contains("longevity") -> UserHealthGoal.LONGEVITY
                        else -> UserHealthGoal.MAINTENANCE
                    }
                }
                line.contains("Dietary Restriction:") -> {
                    val restrictionStr = line.substringAfter("Dietary Restriction:").trim().lowercase()
                    dietaryRestriction = when {
                        restrictionStr.contains("vegan") -> UserDietaryRestriction.VEGAN
                        restrictionStr.contains("vegetarian") -> UserDietaryRestriction.VEGETARIAN
                        restrictionStr.contains("pescatarian") -> UserDietaryRestriction.PESCATARIAN
                        restrictionStr.contains("gluten") -> UserDietaryRestriction.GLUTEN_FREE
                        restrictionStr.contains("dairy") -> UserDietaryRestriction.DAIRY_FREE
                        restrictionStr.contains("keto") -> UserDietaryRestriction.KETO
                        restrictionStr.contains("paleo") -> UserDietaryRestriction.PALEO
                        restrictionStr.contains("nut") -> UserDietaryRestriction.NUT_ALLERGY
                        else -> UserDietaryRestriction.NONE
                    }
                }
                line.contains("Activity Level:") -> {
                    val activityStr = line.substringAfter("Activity Level:").trim().lowercase()
                    activityLevel = when {
                        activityStr.contains("sedentary") -> UserActivityLevel.SEDENTARY
                        activityStr.contains("lightly") -> UserActivityLevel.LIGHTLY_ACTIVE
                        activityStr.contains("very") -> UserActivityLevel.VERY_ACTIVE
                        activityStr.contains("extra") -> UserActivityLevel.EXTRA_ACTIVE
                        else -> UserActivityLevel.MODERATELY_ACTIVE
                    }
                }
            }
        }

        return UserAssessment(
            name = name,
            language = language,
            country = country,
            age = age,
            heightCm = heightCm,
            currentWeightKg = currentWeightKg,
            gender = gender,
            healthGoal = healthGoal,
            dietaryRestriction = dietaryRestriction,
            activityLevel = activityLevel
        )
    }

    private fun calculateMacrosForUser(assessment: UserAssessment): MacroCalculation {
        // Mifflin-St Jeor BMR Formula
        val bmr = when (assessment.gender) {
            UserGender.MALE -> (10 * assessment.currentWeightKg) +
                    (6.25 * assessment.heightCm) -
                    (5 * assessment.age) + 5
            UserGender.FEMALE -> (10 * assessment.currentWeightKg) +
                    (6.25 * assessment.heightCm) -
                    (5 * assessment.age) - 161
        }

        // Activity multiplier for TDEE
        val activityMultiplier = when (assessment.activityLevel) {
            UserActivityLevel.SEDENTARY -> 1.2
            UserActivityLevel.LIGHTLY_ACTIVE -> 1.375
            UserActivityLevel.MODERATELY_ACTIVE -> 1.55
            UserActivityLevel.VERY_ACTIVE -> 1.725
            UserActivityLevel.EXTRA_ACTIVE -> 1.9
        }

        val tdee = bmr * activityMultiplier

        // Goal-based calorie adjustment
        val calorieTarget = when (assessment.healthGoal) {
            UserHealthGoal.WEIGHT_LOSS -> (tdee * 0.80).toInt()
            UserHealthGoal.MAINTENANCE -> tdee.toInt()
            UserHealthGoal.MUSCLE_GAIN -> (tdee * 1.10).toInt()
            UserHealthGoal.ATHLETIC_PERFORMANCE -> (tdee * 1.05).toInt()
            UserHealthGoal.LONGEVITY -> (tdee * 0.95).toInt()
        }

        // Macro distribution based on goal
        val (proteinPct, carbsPct, fatPct) = when (assessment.healthGoal) {
            UserHealthGoal.WEIGHT_LOSS -> Triple(0.35, 0.35, 0.30)
            UserHealthGoal.MAINTENANCE -> Triple(0.25, 0.45, 0.30)
            UserHealthGoal.MUSCLE_GAIN -> Triple(0.30, 0.45, 0.25)
            UserHealthGoal.ATHLETIC_PERFORMANCE -> Triple(0.25, 0.50, 0.25)
            UserHealthGoal.LONGEVITY -> Triple(0.25, 0.40, 0.35)
        }

        // Adjust for keto restriction
        val (finalProtein, finalCarbs, finalFat) = if (assessment.dietaryRestriction == UserDietaryRestriction.KETO) {
            Triple(0.25, 0.05, 0.70)
        } else {
            Triple(proteinPct, carbsPct, fatPct)
        }

        return MacroCalculation(
            bmr = bmr,
            tdee = tdee,
            dailyCalorieTarget = calorieTarget,
            proteinGrams = ((calorieTarget * finalProtein) / 4).toInt(),
            carbsGrams = ((calorieTarget * finalCarbs) / 4).toInt(),
            fatGrams = ((calorieTarget * finalFat) / 9).toInt(),
            calculationNotes = "BMR calculated using Mifflin-St Jeor equation. " +
                    "TDEE based on ${assessment.activityLevel.name.lowercase().replace("_", " ")} activity level. " +
                    "Calories adjusted for ${assessment.healthGoal.name.lowercase().replace("_", " ")} goal."
        )
    }

    private fun generateDietPlanPrompt(request: PlanGenerationRequest): String {
        return when (request) {
            is PlanGenerationRequest.Initial -> generateInitialPlanPrompt(request)
            is PlanGenerationRequest.Correction -> generateCorrectionPrompt(request)
            is PlanGenerationRequest.UserRevision -> generateUserRevisionPrompt(request)
        }
    }

    private fun generateInitialPlanPrompt(request: PlanGenerationRequest.Initial): String = """
        <instructions>
        Generate a detailed, personalized diet plan that EXACTLY matches the following nutritional targets.
        You MUST ensure all meals sum up to the target macros within a 5% tolerance.
        Return ONLY a valid DietPlan structure with all required fields.
        ALL TEXT must be in ${request.assessment.language.displayName}.
        </instructions>

        <user_profile>
        - Name: ${request.assessment.name}
        - Language: ${request.assessment.language.displayName}
        - Country: ${request.assessment.country.displayName}
        - Age: ${request.assessment.age} years
        - Height: ${request.assessment.heightCm} cm
        - Weight: ${request.assessment.currentWeightKg} kg
        - Gender: ${request.assessment.gender.name.lowercase()}
        - Health Goal: ${request.assessment.healthGoal.name.replace("_", " ").lowercase()}
        - Dietary Restriction: ${request.assessment.dietaryRestriction.name.replace("_", " ").lowercase()}
        - Activity Level: ${request.assessment.activityLevel.name.replace("_", " ").lowercase()}
        </user_profile>

        <cultural_context>
        ${request.assessment.country.dietaryContext}
        </cultural_context>

        <nutritional_targets>
        - Daily Calories: ${request.macros.dailyCalorieTarget} kcal
        - Protein: ${request.macros.proteinGrams}g
        - Carbohydrates: ${request.macros.carbsGrams}g
        - Fat: ${request.macros.fatGrams}g

        Calculation notes: ${request.macros.calculationNotes}
        </nutritional_targets>

        <requirements>
        1. Create 4-5 meals (breakfast, lunch, dinner, 1-2 snacks)
        2. Each meal must include specific food items with realistic portions
        3. Strictly respect the dietary restriction: ${request.assessment.dietaryRestriction.name}
        4. Prioritize whole, nutrient-dense foods
        5. Include variety across meals
        6. All nutritional values must be realistic and accurate
        7. Total macros MUST match targets (within 5% tolerance)
        8. Each food item must have accurate calorie and macro values
        9. Prioritize foods commonly available in ${request.assessment.country.displayName}
        10. Use meal patterns and timing typical for ${request.assessment.country.displayName}
        11. All recommendations and explanations MUST be written in ${request.assessment.language.displayName}
        </requirements>
    """.trimIndent()

    private fun generateCorrectionPrompt(request: PlanGenerationRequest.Correction): String = """
        <instructions>
        The previous diet plan did not meet the nutritional targets. Generate a CORRECTED plan that exactly matches the targets.
        Return ONLY a valid DietPlan structure with all required fields.
        ALL TEXT must be in ${request.assessment.language.displayName}.
        </instructions>

        <user_profile>
        - Name: ${request.assessment.name}
        - Language: ${request.assessment.language.displayName}
        - Country: ${request.assessment.country.displayName}
        - Age: ${request.assessment.age} years
        - Height: ${request.assessment.heightCm} cm
        - Weight: ${request.assessment.currentWeightKg} kg
        - Gender: ${request.assessment.gender.name.lowercase()}
        - Health Goal: ${request.assessment.healthGoal.name.replace("_", " ").lowercase()}
        - Dietary Restriction: ${request.assessment.dietaryRestriction.name.replace("_", " ").lowercase()}
        - Activity Level: ${request.assessment.activityLevel.name.replace("_", " ").lowercase()}
        </user_profile>

        <cultural_context>
        ${request.assessment.country.dietaryContext}
        </cultural_context>

        <nutritional_targets>
        - Daily Calories: ${request.macros.dailyCalorieTarget} kcal
        - Protein: ${request.macros.proteinGrams}g
        - Carbohydrates: ${request.macros.carbsGrams}g
        - Fat: ${request.macros.fatGrams}g
        </nutritional_targets>

        <previous_plan_issues>
        ${request.reviewFeedback}
        </previous_plan_issues>

        <previous_plan>
        ${request.previousPlan.toMarkdownString()}
        </previous_plan>

        <requirements>
        1. Address ALL issues identified in the review
        2. Maintain meal structure but adjust portions/foods as needed
        3. Ensure total macros EXACTLY match targets (within 5% tolerance)
        4. Keep user preferences and restrictions in mind
        5. Double-check all calculations before finalizing
        6. Maintain culturally appropriate foods for ${request.assessment.country.displayName}
        7. All text MUST be in ${request.assessment.language.displayName}
        </requirements>
    """.trimIndent()

    private fun generateUserRevisionPrompt(request: PlanGenerationRequest.UserRevision): String = """
        <instructions>
        The user has requested changes to the diet plan. Incorporate their feedback while maintaining nutritional targets.
        Return ONLY a valid DietPlan structure with all required fields.
        ALL TEXT must be in ${request.assessment.language.displayName}.
        </instructions>

        <user_feedback>
        ${request.userFeedback}
        </user_feedback>

        <nutritional_targets>
        - Daily Calories: ${request.macros.dailyCalorieTarget} kcal
        - Protein: ${request.macros.proteinGrams}g
        - Carbohydrates: ${request.macros.carbsGrams}g
        - Fat: ${request.macros.fatGrams}g
        </nutritional_targets>

        <user_profile>
        - Language: ${request.assessment.language.displayName}
        - Country: ${request.assessment.country.displayName}
        - Dietary Restriction: ${request.assessment.dietaryRestriction.name.replace("_", " ").lowercase()}
        </user_profile>

        <cultural_context>
        ${request.assessment.country.dietaryContext}
        </cultural_context>

        <previous_plan>
        ${request.previousPlan.toMarkdownString()}
        </previous_plan>

        <requirements>
        1. Incorporate the user's specific requests
        2. Maintain overall nutritional targets (within 5% tolerance)
        3. Keep dietary restrictions: ${request.assessment.dietaryRestriction.name}
        4. If user requests conflict with targets, find the best compromise
        5. Ensure all food substitutions maintain similar macro profiles
        6. Maintain culturally appropriate foods for ${request.assessment.country.displayName}
        7. All text MUST be in ${request.assessment.language.displayName}
        </requirements>
    """.trimIndent()

    private fun reviewDietPlanPrompt(plan: DietPlan, macros: MacroCalculation): String = """
        <instructions>
        You are a nutrition verification specialist. Your task is to verify that the generated diet plan
        EXACTLY matches the calculated nutritional targets within a 5% tolerance.
        </instructions>

        <targets>
        - Daily Calories: ${macros.dailyCalorieTarget} kcal (tolerance: +/- 5%)
        - Protein: ${macros.proteinGrams}g (tolerance: +/- 5%)
        - Carbohydrates: ${macros.carbsGrams}g (tolerance: +/- 5%)
        - Fat: ${macros.fatGrams}g (tolerance: +/- 5%)
        </targets>

        <plan_totals>
        - Plan Calories: ${plan.totalCalories} kcal
        - Plan Protein: ${plan.totalProtein}g
        - Plan Carbs: ${plan.totalCarbs}g
        - Plan Fat: ${plan.totalFat}g
        </plan_totals>

        <verification_checklist>
        1. Do total calories fall within 5% of target? Target: ${macros.dailyCalorieTarget}, Actual: ${plan.totalCalories}
        2. Does total protein fall within 5% of target? Target: ${macros.proteinGrams}g, Actual: ${plan.totalProtein}g
        3. Do total carbs fall within 5% of target? Target: ${macros.carbsGrams}g, Actual: ${plan.totalCarbs}g
        4. Does total fat fall within 5% of target? Target: ${macros.fatGrams}g, Actual: ${plan.totalFat}g
        5. Are all food portion sizes realistic and accurate?
        6. Do individual meal macros sum to meal totals correctly?
        7. Do all meal totals sum to plan totals correctly?
        </verification_checklist>

        <plan_details>
        ${plan.toMarkdownString()}
        </plan_details>

        If ALL checks pass within tolerance, the plan is correct. If ANY check fails, list the specific issues that need to be fixed.
    """.trimIndent()

    fun formatAssessmentForPrompt(assessment: UserAssessment): String {
        return """
            Generate a comprehensive personalized diet plan based on the following user profile:

            - Name: ${assessment.name}
            - Language: ${assessment.language.displayName}
            - Country: ${assessment.country.displayName}
            - Age: ${assessment.age} years old
            - Height: ${assessment.heightCm} cm
            - Weight: ${assessment.currentWeightKg} kg
            - Gender: ${assessment.gender.name.lowercase()}
            - Health Goal: ${assessment.healthGoal.name.replace("_", " ").lowercase()}
            - Dietary Restriction: ${assessment.dietaryRestriction.name.replace("_", " ").lowercase()}
            - Activity Level: ${assessment.activityLevel.name.replace("_", " ").lowercase()}

            IMPORTANT: All text in the diet plan MUST be written in ${assessment.language.displayName}.
            Prioritize foods and meal patterns common in ${assessment.country.displayName}.

            Please provide a detailed diet plan including daily calorie targets, macronutrient breakdown, and sample meal suggestions.
        """.trimIndent()
    }
}
