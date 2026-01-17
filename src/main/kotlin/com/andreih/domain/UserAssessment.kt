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
@LLMDescription("User's preferred language for communication")
enum class UserLanguage(val displayName: String, val culturalNote: String) {
    @LLMDescription("English language")
    ENGLISH("English", "Common in USA, UK, Canada, Australia"),
    @LLMDescription("Spanish language")
    SPANISH("Spanish", "Common in Spain, Latin America"),
    @LLMDescription("French language")
    FRENCH("French", "Common in France, Quebec, West Africa"),
    @LLMDescription("German language")
    GERMAN("German", "Common in Germany, Austria, Switzerland"),
    @LLMDescription("Italian language")
    ITALIAN("Italian", "Common in Italy"),
    @LLMDescription("Portuguese language")
    PORTUGUESE("Portuguese", "Common in Brazil, Portugal"),
    @LLMDescription("Mandarin Chinese")
    CHINESE_MANDARIN("Mandarin Chinese", "Common in China, Taiwan, Singapore"),
    @LLMDescription("Japanese language")
    JAPANESE("Japanese", "Common in Japan"),
    @LLMDescription("Korean language")
    KOREAN("Korean", "Common in South Korea"),
    @LLMDescription("Arabic language")
    ARABIC("Arabic", "Common in Middle East, North Africa"),
    @LLMDescription("Hindi language")
    HINDI("Hindi", "Common in India"),
    @LLMDescription("Russian language")
    RUSSIAN("Russian", "Common in Russia, Eastern Europe"),
    @LLMDescription("Polish language")
    POLISH("Polish", "Common in Poland"),
    @LLMDescription("Dutch language")
    DUTCH("Dutch", "Common in Netherlands, Belgium"),
    @LLMDescription("Swedish language")
    SWEDISH("Swedish", "Common in Sweden"),
    @LLMDescription("Turkish language")
    TURKISH("Turkish", "Common in Turkey"),
    @LLMDescription("Greek language")
    GREEK("Greek", "Common in Greece, Cyprus"),
    @LLMDescription("Romanian language")
    ROMANIAN("Romanian", "Common in Romania"),
    @LLMDescription("Vietnamese language")
    VIETNAMESE("Vietnamese", "Common in Vietnam"),
    @LLMDescription("Thai language")
    THAI("Thai", "Common in Thailand")
}

@Serializable
@LLMDescription("User's country or region for culturally appropriate food recommendations")
enum class UserCountry(val displayName: String, val dietaryContext: String) {
    @LLMDescription("United States of America")
    USA("United States", "Diverse cuisine, large portions, breakfast culture"),
    @LLMDescription("United Kingdom")
    UK("United Kingdom", "Traditional British meals, tea culture"),
    @LLMDescription("Canada")
    CANADA("Canada", "Mix of American and European influences"),
    @LLMDescription("Australia")
    AUSTRALIA("Australia", "Fresh produce, seafood, multicultural influence"),
    @LLMDescription("Spain")
    SPAIN("Spain", "Mediterranean diet, tapas culture, late dinner times"),
    @LLMDescription("Mexico")
    MEXICO("Mexico", "Corn, beans, chili peppers, multiple small meals"),
    @LLMDescription("Argentina")
    ARGENTINA("Argentina", "High protein, beef-focused, mate tea culture"),
    @LLMDescription("Brazil")
    BRAZIL("Brazil", "Rice and beans staple, tropical fruits, feijoada"),
    @LLMDescription("France")
    FRANCE("France", "Refined cuisine, cheese, wine, structured meal times"),
    @LLMDescription("Germany")
    GERMANY("Germany", "Hearty meals, bread, potatoes, sausages"),
    @LLMDescription("Italy")
    ITALY("Italy", "Mediterranean diet, pasta, olive oil, regional diversity"),
    @LLMDescription("Portugal")
    PORTUGAL("Portugal", "Seafood, olive oil, bacalhau, Mediterranean influence"),
    @LLMDescription("Poland")
    POLAND("Poland", "Hearty soups, pierogi, cabbage, potatoes"),
    @LLMDescription("Netherlands")
    NETHERLANDS("Netherlands", "Dairy products, bread, stamppot, herring"),
    @LLMDescription("Sweden")
    SWEDEN("Sweden", "Fish, root vegetables, rye bread, fika culture"),
    @LLMDescription("China")
    CHINA("China", "Rice, noodles, tea, regional variety, balanced meals"),
    @LLMDescription("Japan")
    JAPAN("Japan", "Rice, fish, fermented foods, small portions, umami"),
    @LLMDescription("South Korea")
    SOUTH_KOREA("South Korea", "Rice, kimchi, banchan, soup with every meal"),
    @LLMDescription("India")
    INDIA("India", "Spices, lentils, rice, regional diversity, vegetarian-friendly"),
    @LLMDescription("Thailand")
    THAILAND("Thailand", "Rice, curries, fish sauce, fresh herbs, balanced flavors"),
    @LLMDescription("Vietnam")
    VIETNAM("Vietnam", "Rice, fresh herbs, fish sauce, noodles, light cooking"),
    @LLMDescription("Greece")
    GREECE("Greece", "Mediterranean diet, olive oil, feta, fresh vegetables"),
    @LLMDescription("Turkey")
    TURKEY("Turkey", "Mediterranean and Middle Eastern fusion, mezze, kebabs"),
    @LLMDescription("Egypt")
    EGYPT("Egypt", "Legumes, bread, rice, Mediterranean and Middle Eastern"),
    @LLMDescription("Saudi Arabia")
    SAUDI_ARABIA("Saudi Arabia", "Rice, lamb, dates, Arabic coffee, halal"),
    @LLMDescription("United Arab Emirates")
    UAE("United Arab Emirates", "International hub, Middle Eastern base, halal"),
    @LLMDescription("Russia")
    RUSSIA("Russia", "Hearty soups, root vegetables, rye bread, pickles"),
    @LLMDescription("Romania")
    ROMANIA("Romania", "Eastern European cuisine, polenta, sour cream"),
    @LLMDescription("South Africa")
    SOUTH_AFRICA("South Africa", "Braai culture, diverse influences, biltong"),
    @LLMDescription("International or no specific country")
    INTERNATIONAL("International", "Global fusion, no specific regional preference")
}

@Serializable
@LLMDescription("All the user's assessment information necessary to create a dietary plan")
data class UserAssessment(
    @property:LLMDescription("Name of the user")
    val name: String,
    @property:LLMDescription("Preferred language for communication")
    val language: UserLanguage,
    @property:LLMDescription("Country or region for culturally appropriate meal recommendations")
    val country: UserCountry,
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
