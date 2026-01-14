package com.andreih.domain

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("Calculated macro and calorie targets based on user profile")
data class MacroCalculation(
    @property:LLMDescription("User's calculated Basal Metabolic Rate in kcal/day")
    val bmr: Double,
    @property:LLMDescription("Total Daily Energy Expenditure in kcal/day")
    val tdee: Double,
    @property:LLMDescription("Daily calorie target adjusted for user's goal")
    val dailyCalorieTarget: Int,
    @property:LLMDescription("Daily protein target in grams")
    val proteinGrams: Int,
    @property:LLMDescription("Daily carbohydrate target in grams")
    val carbsGrams: Int,
    @property:LLMDescription("Daily fat target in grams")
    val fatGrams: Int,
    @property:LLMDescription("Brief explanation of the calculations")
    val calculationNotes: String
)
