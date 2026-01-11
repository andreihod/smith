package com.andreih.io.cli

import com.andreih.domain.*
import com.andreih.io.UserInputProvider

class CliUserInput : UserInputProvider {

    override fun collectUserAssessment(): UserAssessment {
        println("\n=== Diet Planner - User Assessment ===\n")

        print("What is your name? ")
        val name = readln()

        print("What is your age? ")
        val age = readln().toIntOrNull() ?: 30

        print("What is your height in cm? ")
        val height = readln().toIntOrNull() ?: 170

        print("What is your current weight in kg? ")
        val weight = readln().toDoubleOrNull() ?: 70.0

        val gender = selectEnum<UserGender>("What is your gender?")
        val healthGoal = selectEnum<UserHealthGoal>("What is your health goal?")
        val dietaryRestriction = selectEnum<UserDietaryRestriction>("Do you have any dietary restrictions?")
        val activityLevel = selectEnum<UserActivityLevel>("What is your activity level?")

        return UserAssessment(
            name = name,
            age = age,
            heightCm = height,
            currentWeightKg = weight,
            gender = gender,
            healthGoal = healthGoal,
            dietaryRestriction = dietaryRestriction,
            activityLevel = activityLevel
        )
    }

    private inline fun <reified T : Enum<T>> selectEnum(prompt: String): T {
        val values = enumValues<T>()
        println(prompt)
        values.forEachIndexed { index, value ->
            println("  ${index + 1}. $value")
        }
        print("Enter choice (1-${values.size}): ")
        val choice = readln().toIntOrNull() ?: 1
        return values.getOrElse(choice - 1) { values.first() }
    }
}
