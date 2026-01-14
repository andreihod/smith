package com.andreih.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface PlanGenerationRequest {
    val assessment: UserAssessment
    val macros: MacroCalculation

    @Serializable
    data class Initial(
        override val assessment: UserAssessment,
        override val macros: MacroCalculation
    ) : PlanGenerationRequest

    @Serializable
    data class Correction(
        override val assessment: UserAssessment,
        override val macros: MacroCalculation,
        val previousPlan: DietPlan,
        val reviewFeedback: String
    ) : PlanGenerationRequest

    @Serializable
    data class UserRevision(
        override val assessment: UserAssessment,
        override val macros: MacroCalculation,
        val previousPlan: DietPlan,
        val userFeedback: String
    ) : PlanGenerationRequest
}
