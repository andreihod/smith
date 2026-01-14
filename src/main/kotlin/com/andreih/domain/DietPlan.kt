package com.andreih.domain

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

@Serializable
@LLMDescription("A food item with nutritional information")
data class FoodItem(
    @property:LLMDescription("Name of the food")
    val name: String,
    @property:LLMDescription("Portion size (e.g., '150g', '1 cup')")
    val portion: String,
    @property:LLMDescription("Calories in this portion")
    val calories: Int,
    @property:LLMDescription("Protein in grams")
    val protein: Int,
    @property:LLMDescription("Carbs in grams")
    val carbs: Int,
    @property:LLMDescription("Fat in grams")
    val fat: Int
)

@Serializable
@LLMDescription("A single meal in the diet plan")
data class Meal(
    @property:LLMDescription("Name of the meal (Breakfast, Lunch, Dinner, Snack)")
    val name: String,
    @property:LLMDescription("Suggested time for the meal")
    val time: String,
    @property:LLMDescription("List of food items in this meal")
    val foods: List<FoodItem>,
    @property:LLMDescription("Total calories for this meal")
    val calories: Int,
    @property:LLMDescription("Protein in grams for this meal")
    val protein: Int,
    @property:LLMDescription("Carbs in grams for this meal")
    val carbs: Int,
    @property:LLMDescription("Fat in grams for this meal")
    val fat: Int
)

@Serializable
@LLMDescription("Generated diet plan with meals for the user")
data class DietPlan(
    @property:LLMDescription("List of daily meals in the diet plan")
    val meals: List<Meal>,
    @property:LLMDescription("Total calculated calories in the plan")
    val totalCalories: Int,
    @property:LLMDescription("Total protein grams in the plan")
    val totalProtein: Int,
    @property:LLMDescription("Total carbs grams in the plan")
    val totalCarbs: Int,
    @property:LLMDescription("Total fat grams in the plan")
    val totalFat: Int,
    @property:LLMDescription("Additional nutritional recommendations")
    val recommendations: String
) {
    fun toMarkdownString(): String = buildString {
        appendLine("# Personalized Diet Plan")
        appendLine()
        appendLine("## Daily Totals")
        appendLine("- **Calories:** $totalCalories kcal")
        appendLine("- **Protein:** ${totalProtein}g | **Carbs:** ${totalCarbs}g | **Fat:** ${totalFat}g")
        appendLine()

        meals.forEach { meal ->
            appendLine("## ${meal.name} (${meal.time})")
            appendLine("*${meal.calories} kcal | P: ${meal.protein}g | C: ${meal.carbs}g | F: ${meal.fat}g*")
            appendLine()
            meal.foods.forEach { food ->
                appendLine("- **${food.name}** (${food.portion}) - ${food.calories} kcal")
            }
            appendLine()
        }

        appendLine("## Recommendations")
        appendLine(recommendations)
    }
}
