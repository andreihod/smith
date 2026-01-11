package com.andreih

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.xml.xml
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    MUSCLE_GAI,
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

fun main() = runBlocking {
    val config = ConfigFactory.load()
    val geminiApiKey = config.getString("provider.gemini_api_key")

    val registry = ToolRegistry {
        tool(SayToUser)
        tool(AskUser)
    }

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
            
            ### OPERATIONAL CONSTRAINT
            You act as a component of a larger system. Accept input data (user metrics) as truth and provide outputs
            optimized for the specific step in the workflow (whether that is assessment, calculation, or planning).
        """.trimIndent(),
        promptExecutor = simpleGoogleAIExecutor(geminiApiKey),
        llmModel = GoogleModels.Gemini2_5Flash,
        toolRegistry = registry,
        strategy = strategy<String, String>("diet-planner-strategy") {
            val nodeExecuteTool by nodeExecuteTool()

            val userAssessmentStep by subgraphWithTask<String, UserAssessment>(
                tools = registry.tools
            ) { initialMessage ->
                xml {
                    tag("instructions") {
                        """
                        Clarify a user assessment with missing information. make sure every information is given.
                        Keep asking the user the information until it's clear.
                        """.trimIndent()
                    }

                    tag("initial_user_message") {
                        +initialMessage
                    }
                }
            }

            val generateDietPlanStep by subgraphWithTask<UserAssessment, String>(
                tools = registry.tools
            ) { assessment ->
                xml {
                    tag("instructions") {
                        """
                        You are "Optima-Diet," a highly knowledgeable, empathetic, and detail-oriented Certified Digital
                        Nutritionist. Your goal is to design evidence-based, personalized meal plans that align with a
                        user's health goals, dietary restrictions, and lifestyle preferences.
                        
                        Given the user assessment, elaborate in text form the user's diet.
                        """.trimIndent()
                    }

                    tag("assessment") {
                        Json.encodeToString(assessment)
                    }
                }
            }

            edge(nodeStart forwardTo userAssessmentStep)
            edge(userAssessmentStep forwardTo nodeExecuteTool onToolCall { true })
            edge(nodeExecuteTool forwardTo userAssessmentStep onAssistantMessage { true })

            edge(userAssessmentStep forwardTo generateDietPlanStep)

            edge(generateDietPlanStep forwardTo nodeFinish
                    transformed { it }
                    onAssistantMessage { true }
            )
        }
    )

    println("Welcome to your Clinical Nutrition & Dietetics Specialist, tell me about yourself and your goals")
    val userInput = readln()
    val result = agent.run(userInput)

    println(result)
}