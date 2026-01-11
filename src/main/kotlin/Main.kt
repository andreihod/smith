package com.andreih

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.model.SingleFact
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("User gender options")
enum class UserGender {
    MALE,
    FEMALE
}

@Serializable
@LLMDescription("User health goal")
enum class UserHealthGoal {
    @LLMDescription("Aggressive fat loss with muscle preservation")
    WEIGHT_LOSS,
    @LLMDescription("Maintaining current body composition and energy levels")
    MAINTENANCE,
    @LLMDescription("Hypertrophy focused with a caloric surplus")
    MUSCLE_GAIN,
    @LLMDescription("Optimizing for endurance and recovery")
    ATHLETIC_PERFORMANCE,
    @LLMDescription("Focus on micronutrients and blood sugar stability")
    LONGEVITY
}

@Serializable
@LLMDescription("User dietary restriction options, if any")
enum class UserDietaryRestriction {
    @LLMDescription("None")
    NONE,
    @LLMDescription("No animal products")
    VEGAN,
    @LLMDescription("No meat, but includes dairy/eggs")
    VEGETARIAN,
    @LLMDescription("Vegetarian plus seafood")
    PESCATARIAN,
    @LLMDescription("No wheat, barley, or rye")
    GLUTEN_FREE,
    @LLMDescription("No milk-based products")
    DAIRY_FREE,
    @LLMDescription("High fat, very low carb")
    KETO,
    @LLMDescription("Whole foods, no grains or legumes")
    PALEO,
    @LLMDescription("Strict avoidance of tree nuts and peanuts")
    NUT_ALLERGY
}

@Serializable
@LLMDescription("How much the user exercise")
enum class UserActivityLevel {
    @LLMDescription("Little to no exercise")
    SEDENTARY,
    @LLMDescription("Light exercise 1-3 days/week")
    LIGHTLY_ACTIVE,
    @LLMDescription("Moderate exercise 3-5 days/week")
    MODERATELY_ACTIVE,
    @LLMDescription("Hard exercise 6-7 days/week")
    VERY_ACTIVE,
    @LLMDescription("Physical job or 2x daily training")
    EXTRA_ACTIVE
}

@Serializable
@LLMDescription("All the user's assessment information necessary to create a dietary plan")
data class UserAssessment(
    @property:LLMDescription("Name of the user")
    val name: String,
    @property:LLMDescription("Age of the user, example 33")
    val age: Int,
    @property:LLMDescription("Height of the user in cm, example 175")
    val heightCm: Int,
    @property:LLMDescription("Current weight of the user, for example 79.3 kg")
    val currentWeightKg: Double,
    @property:LLMDescription("Gender of the user, male or female")
    val gender: UserGender,
    @property:LLMDescription("Goal of the user with the dietary plan")
    val healthGoal: UserHealthGoal,
    @property:LLMDescription("Dietary restriction of the user, if any")
    val dietaryRestriction: UserDietaryRestriction,
    @property:LLMDescription("Activity level of the user")
    val activityLevel: UserActivityLevel,
)

private inline fun <reified T : Enum<T>> selectEnum(prompt: String): T {
    val values = enumValues<T>()
    println(prompt)
    values.forEachIndexed { index, value ->
        println("  ${index + 1}. $value")
    }
    print("Enter choice (1-${values.size}): ")
    val choice = readln().toIntOrNull() ?: 1
    return values.getOrElse(choice - 1) { values.first() }
}

fun collectUserAssessmentFromCLI(): UserAssessment {
    println("\n=== Diet Planner - User Assessment ===\n")

    print("What is your name? ")
    val name = readln()

    print("What is your age? ")
    val age = readln().toIntOrNull() ?: 30

    print("What is your height in cm? ")
    val height = readln().toIntOrNull() ?: 170

    print("What is your current weight in kg? ")
    val weight = readln().toDoubleOrNull() ?: 70.0

    val gender = selectEnum<UserGender>("What is your gender?")
    val healthGoal = selectEnum<UserHealthGoal>("What is your health goal?")
    val dietaryRestriction = selectEnum<UserDietaryRestriction>("Do you have any dietary restrictions?")
    val activityLevel = selectEnum<UserActivityLevel>("What is your activity level?")

    return UserAssessment(
        name = name,
        age = age,
        heightCm = height,
        currentWeightKg = weight,
        gender = gender,
        healthGoal = healthGoal,
        dietaryRestriction = dietaryRestriction,
        activityLevel = activityLevel
    )
}

// Custom User memory subject
object UserSubject : MemorySubject() {
    override val name: String = "user"
    override val promptDescription: String = "User information (personal details, health goals, dietary restrictions, etc.)"
    override val priorityLevel: Int = 1
}

// Simple in-memory provider implementation
class InMemoryProvider : AgentMemoryProvider {
    private val facts = mutableListOf<Triple<Fact, MemorySubject, MemoryScope>>()

    override suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope) {
        facts.add(Triple(fact, subject, scope))
    }

    override suspend fun load(concept: Concept, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts
            .filter { it.first.concept.keyword == concept.keyword && it.second == subject }
            .map { it.first }
    }

    override suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts.filter { it.second == subject }.map { it.first }
    }

    override suspend fun loadByDescription(description: String, subject: MemorySubject, scope: MemoryScope): List<Fact> {
        return facts
            .filter { it.second == subject && it.first.concept.description.contains(description, ignoreCase = true) }
            .map { it.first }
    }
}

fun formatAssessmentForPrompt(assessment: UserAssessment): String {
    return """
        Generate a comprehensive personalized diet plan based on the following user profile:

        - Name: ${assessment.name}
        - Age: ${assessment.age} years old
        - Height: ${assessment.heightCm} cm
        - Weight: ${assessment.currentWeightKg} kg
        - Gender: ${assessment.gender.name.lowercase()}
        - Health Goal: ${assessment.healthGoal.name.replace("_", " ").lowercase()}
        - Dietary Restriction: ${assessment.dietaryRestriction.name.replace("_", " ").lowercase()}
        - Activity Level: ${assessment.activityLevel.name.replace("_", " ").lowercase()}

        Please provide a detailed diet plan including daily calorie targets, macronutrient breakdown, and sample meal suggestions.
    """.trimIndent()
}

suspend fun saveUserAssessmentToMemory(assessment: UserAssessment, memoryProvider: AgentMemoryProvider, agentName: String) {
    val scope = MemoryScope.Agent(agentName)
    val subject = UserSubject

    val timestamp = System.currentTimeMillis()

    listOf(
        Triple("user-name", "The name of the user", "The user's name is ${assessment.name}"),
        Triple("user-age", "The age of the user in years", "The user is ${assessment.age} years old"),
        Triple("user-height", "The height of the user in centimeters", "The user is ${assessment.heightCm} cm tall"),
        Triple("user-weight", "The weight of the user in kilograms", "The user weighs ${assessment.currentWeightKg} kg"),
        Triple("user-gender", "The gender of the user", "The user's gender is ${assessment.gender.name.lowercase()}"),
        Triple("user-health-goal", "The health goal of the user", "The user's health goal is ${assessment.healthGoal.name.replace("_", " ").lowercase()}"),
        Triple("user-dietary-restriction", "Any dietary restrictions the user has", "The user's dietary restriction is ${assessment.dietaryRestriction.name.replace("_", " ").lowercase()}"),
        Triple("user-activity-level", "How active the user is", "The user's activity level is ${assessment.activityLevel.name.replace("_", " ").lowercase()}")
    ).forEach { (keyword, description, value) ->
        memoryProvider.save(
            fact = SingleFact(
                concept = Concept(keyword, description, FactType.SINGLE),
                timestamp = timestamp,
                value = value
            ),
            subject = subject,
            scope = scope
        )
    }
}

fun main() = runBlocking {
    val config = ConfigFactory.load()
    val geminiApiKey = config.getString("provider.gemini_api_key")

    // Collect user assessment via CLI
    val assessment = collectUserAssessmentFromCLI()

    println("\n=== Generating your personalized diet plan... ===\n")

    val registry = ToolRegistry { }

    // Create in-memory provider for facts
    val memoryProvider = InMemoryProvider()

    // Save assessment to memory
    saveUserAssessmentToMemory(assessment, memoryProvider, "diet-planner")

    val agent = AIAgent(
        systemPrompt = """
            ### IDENTITY
            You are a "Clinical Nutrition & Dietetics Specialist." Your core purpose is to translate complex nutritional
            science into actionable, health-focused dietary guidance. You serve as a bridge between metabolic requirements
            and real-world eating habits.

            ### KNOWLEDGE DOMAIN
            - Comprehensive understanding of macronutrients (Protein, Fats, Carbohydrates) and micronutrients (Vitamins, Minerals).
            - Proficiency in metabolic calculations (BMR, TDEE, Thermic Effect of Food).
            - Knowledge of diverse dietary protocols: Mediterranean, Plant-Based, Ketogenic, DASH, and High-Protein/Satiety-focused.
            - Understanding of glycemic index, insulin response, and nutrient timing.

            ### COGNITIVE BEHAVIOR
            - **Evidence-Based:** Always prioritize peer-reviewed nutritional science over "fad" trends.
            - **Safety-First:** Flag any calorie requests or nutrient deficiencies that fall below healthy physiological thresholds.
            - **Logical Flow:** Ensure that every recommendation correlates directly to the user's stated goals
            (e.g., if the goal is hypertrophy, prioritize leucine-rich protein sources).

            ### ADAPTIVE TONE
            - Maintain a supportive, non-judgmental, and clinical tone.
            - Avoid "good" or "bad" food labels; use "nutrient-dense" or "energy-dense" instead.
            - Focus on "crowding out" (adding healthy foods) rather than just restriction.

            ### USER INFORMATION
            You have access to facts about the user loaded in your memory. Use this information to create a personalized diet plan.
        """.trimIndent(),
        promptExecutor = simpleGoogleAIExecutor(geminiApiKey),
        llmModel = GoogleModels.Gemini2_5Flash,
        toolRegistry = registry,
        strategy = strategy<String, String>("diet-planner-strategy") {
            val nodeSendToLLM by nodeLLMRequest()

            edge(nodeStart forwardTo nodeSendToLLM)
            edge(nodeSendToLLM forwardTo nodeFinish onAssistantMessage { true })
        }
    ) {
        install(AgentMemory) {
            this.memoryProvider = memoryProvider
            agentName = "diet-planner"
            featureName = "diet-planning"
            organizationName = "com.andreih"
            productName = "diet-planner"
        }
    }

    val result = agent.run(formatAssessmentForPrompt(assessment))

    println(result)
}
