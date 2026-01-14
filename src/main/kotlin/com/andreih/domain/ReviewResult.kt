package com.andreih.domain

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Result of diet plan review verification")
data class ReviewResult(
    @property:LLMDescription("Whether the diet plan meets the calculated macro/calorie targets within tolerance")
    val isCorrect: Boolean,
    @property:LLMDescription("List of issues found if plan is incorrect")
    val issues: List<String>,
    @property:LLMDescription("Suggestions for correction if plan is incorrect")
    val corrections: String
)
