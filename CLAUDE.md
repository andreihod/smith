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

Smith is a Kotlin/JVM AI agent application built with the Koog Agents framework. It implements a Clinical Nutrition & Dietetics Specialist chatbot that collects user health information and generates personalized diet plans.

## Architecture

The application uses a graph-based agent strategy pattern from Koog Agents:

- **Main entry point**: `src/main/kotlin/Main.kt`
- **LLM Provider**: Google Gemini (configured via `application.conf` which is gitignored)
- **Agent Strategy**: Two-step workflow graph:
  1. `userAssessmentStep` - Collects user information via conversation
  2. `generateDietPlanStep` - Creates diet plan from assessment data

**Key Koog Agents concepts used:**
- `AIAgent` with custom `strategy` DSL
- `subgraphWithTask` for typed input/output subgraphs
- `ToolRegistry` with `SayToUser` and `AskUser` tools
- XML-based prompt construction via `xml { }` DSL

## Configuration

API keys are stored in `src/main/resources/application.conf` (gitignored). Required format:
```hocon
provider {
  gemini_api_key = "your-key-here"
}
```

## Dependencies

- Kotlin 2.2.x with JVM 21
- Koog Agents 0.6.0 (AI agent framework)
- Typesafe Config for configuration
- kotlinx-serialization-json for JSON handling
