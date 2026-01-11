package com.andreih.domain

import ai.koog.agents.core.tools.annotations.LLMDescription
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
