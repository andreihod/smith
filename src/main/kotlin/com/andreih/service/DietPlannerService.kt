package com.andreih.service

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import ai.koog.agents.memory.providers.AgentMemoryProvider
import com.andreih.agent.DietPlannerAgent
import com.andreih.agent.memory.InMemoryProvider
import com.andreih.agent.memory.UserSubject
import com.andreih.agent.tools.DietUserTools
import com.andreih.domain.UserAssessment
import com.andreih.io.OutputProvider
import com.andreih.io.UserInputProvider

class DietPlannerService(
    private val geminiApiKey: String,
    private val inputProvider: UserInputProvider,
    private val outputProvider: OutputProvider
) {
    private val memoryProvider: AgentMemoryProvider = InMemoryProvider()

    suspend fun run() {
        val assessment = inputProvider.collectUserAssessment()

        outputProvider.showMessage("\n=== Generating your personalized diet plan... ===\n")

        saveAssessmentToMemory(assessment)

        // Create user interaction callback for the agent to get feedback
        val showMessageAndGetFeedback: suspend (String) -> String = { message ->
            outputProvider.showResult(message)
            outputProvider.showMessage("\n" + "=".repeat(50))
            outputProvider.showMessage("Do you approve this diet plan?")
            outputProvider.showMessage("Type 'yes' to approve, or describe any changes you'd like:")
            outputProvider.showMessage("=".repeat(50))
            print("> ")
            readlnOrNull() ?: "yes"
        }

        val userTools = DietUserTools(showMessageAndGetFeedback)
        val agent = DietPlannerAgent.create(geminiApiKey, memoryProvider, userTools)
        val prompt = DietPlannerAgent.formatAssessmentForPrompt(assessment)
        val result = agent.run(prompt)

        outputProvider.showResult(result)
    }

    private suspend fun saveAssessmentToMemory(assessment: UserAssessment) {
        val scope = MemoryScope.Agent(DietPlannerAgent.AGENT_NAME)
        val subject = UserSubject
        val timestamp = System.currentTimeMillis()

        val userProfileConcept = Concept(
            keyword = "user-profile",
            description = "User profile information including name, age, height, weight, gender, health goals, dietary restrictions, activity level, language, and country",
            factType = FactType.MULTIPLE
        )

        listOf(
            "The user's name is ${assessment.name}",
            "The user's preferred language is ${assessment.language.displayName}",
            "The user is from ${assessment.country.displayName}",
            "The user is ${assessment.age} years old",
            "The user is ${assessment.heightCm} cm tall",
            "The user weighs ${assessment.currentWeightKg} kg",
            "The user's gender is ${assessment.gender.name.lowercase()}",
            "The user's health goal is ${assessment.healthGoal.name.replace("_", " ").lowercase()}",
            "The user's dietary restriction is ${assessment.dietaryRestriction.name.replace("_", " ").lowercase()}",
            "The user's activity level is ${assessment.activityLevel.name.replace("_", " ").lowercase()}"
        ).forEach { value ->
            memoryProvider.save(
                fact = SingleFact(
                    concept = userProfileConcept,
                    timestamp = timestamp,
                    value = value
                ),
                subject = subject,
                scope = scope
            )
        }
    }
}
