package com.andreih.service

import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.SingleFact
import ai.koog.agents.memory.providers.AgentMemoryProvider
import com.andreih.agent.DietPlannerAgent
import com.andreih.agent.memory.InMemoryProvider
import com.andreih.agent.memory.UserSubject
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

        val agent = DietPlannerAgent.create(geminiApiKey, memoryProvider)
        val prompt = DietPlannerAgent.formatAssessmentForPrompt(assessment)
        val result = agent.run(prompt)

        outputProvider.showResult(result)
    }

    private suspend fun saveAssessmentToMemory(assessment: UserAssessment) {
        val scope = MemoryScope.Agent(DietPlannerAgent.AGENT_NAME)
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
}
