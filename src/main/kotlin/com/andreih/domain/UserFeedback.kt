package com.andreih.domain

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("User feedback on the generated diet plan")
data class UserFeedback(
    @property:LLMDescription("Whether the user accepts the diet plan")
    val isAccepted: Boolean,
    @property:LLMDescription("User's message or requested changes")
    val message: String
)
