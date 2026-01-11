package com.andreih.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.google.GoogleModels
import com.andreih.domain.UserAssessment

object DietPlannerAgent {
    const val AGENT_NAME = "diet-planner"

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
    """.trimIndent()

    fun create(
        geminiApiKey: String,
        memoryProvider: AgentMemoryProvider
    ): AIAgent<String, String> {
        val registry = ToolRegistry { }

        return AIAgent(
            systemPrompt = systemPrompt,
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
                agentName = AGENT_NAME
                featureName = "diet-planning"
                organizationName = "com.andreih"
                productName = "diet-planner"
            }
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
}
