# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.andreih.SomeTestClass"

# Run the application
./gradlew run
```

## Project Overview

Smith is a Kotlin/JVM AI agent application built with the Koog Agents framework. It implements a Clinical Nutrition & Dietetics Specialist that collects user health information via CLI and generates personalized diet plans using Google Gemini.

**See `koog-example/` for Koog framework implementation examples** (tool calling, routing, streaming, memory, MCP integration, Spring Boot, Compose Multiplatform).

## Architecture

- **Main entry point**: `src/main/kotlin/Main.kt`
- **LLM Provider**: Google Gemini 2.5 Flash (configured via `application.conf` which is gitignored)

**Data Flow:**
1. CLI collects user assessment (name, age, height, weight, gender, health goal, dietary restriction, activity level)
2. Facts are stored in memory via `InMemoryProvider` with `UserSubject`
3. Assessment is formatted into a prompt and sent to the agent
4. Agent generates personalized diet plan with BMR/TDEE calculations and meal suggestions

**Key Components:**
- `UserAssessment` - Data class with user profile information
- `UserSubject` - Custom `MemorySubject` for user facts
- `InMemoryProvider` - Implements `AgentMemoryProvider` for in-memory fact storage
- `collectUserAssessmentFromCLI()` - CLI-based enum selection for user input
- `formatAssessmentForPrompt()` - Converts assessment to LLM prompt

## Koog Agents Framework

Documentation: https://docs.koog.ai/

**Strategy Pattern:**
```kotlin
strategy = strategy<String, String>("strategy-name") {
    val nodeSendToLLM by nodeLLMRequest()

    edge(nodeStart forwardTo nodeSendToLLM)
    edge(nodeSendToLLM forwardTo nodeFinish onAssistantMessage { true })
}
```

**Memory System:**
- `MemorySubject` - Abstract class for organizing facts (extend to create custom subjects)
- `MemoryScope` - Sealed interface: `Agent(name)`, `Feature(id)`, `Product(name)`, `CrossProduct`
- `SingleFact` - Data class: `SingleFact(concept, timestamp, value)`
- `Concept` - Data class: `Concept(keyword, description, factType)`
- `AgentMemoryProvider` - Interface with `save()`, `load()`, `loadAll()`, `loadByDescription()`

**Installing AgentMemory:**
```kotlin
AIAgent(...) {
    install(AgentMemory) {
        memoryProvider = myProvider
        agentName = "agent-name"
        featureName = "feature-name"
        organizationName = "org-name"
        productName = "product-name"
    }
}
```

**Common Strategy Patterns:**
- Simple LLM call: `nodeStart → nodeLLMRequest → nodeFinish`
- With tools: Add `nodeExecuteTool`, `nodeLLMSendToolResult`, handle `onToolCall { true }`

## Configuration

API keys are stored in `src/main/resources/application.conf` (gitignored). Required format:
```hocon
provider {
  gemini_api_key = "your-key-here"
}
```

## Dependencies

- Kotlin 2.2.x with JVM 21
- Koog Agents 0.6.0 (`ai.koog:koog-agents`)
- Koog Memory (`ai.koog:agents-features-memory-jvm:0.6.0`)
- Typesafe Config for configuration
- kotlinx-serialization-json for JSON handling
