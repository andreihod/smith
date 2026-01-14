package com.andreih.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class DietUserTools(private val showMessageToUser: suspend (String) -> String) : ToolSet {
    @Tool
    @LLMDescription("Show the diet plan to the user and wait for their feedback. Call this tool to present the plan and get approval or change requests.")
    suspend fun showPlanAndGetFeedback(
        @LLMDescription("The formatted diet plan in markdown to show to the user")
        planMarkdown: String
    ): String {
        return showMessageToUser(planMarkdown)
    }
}
