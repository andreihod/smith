package com.andreih

import com.andreih.io.cli.CliOutput
import com.andreih.io.cli.CliUserInput
import com.andreih.service.DietPlannerService
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val config = ConfigFactory.load()
    val geminiApiKey = config.getString("provider.gemini_api_key")

    val service = DietPlannerService(
        geminiApiKey = geminiApiKey,
        inputProvider = CliUserInput(),
        outputProvider = CliOutput()
    )

    service.run()
}
