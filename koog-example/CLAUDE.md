# CLAUDE.md - Koog Examples

This folder contains Koog framework implementation examples. These are reference implementations for learning Koog patterns.

## Project Structure

| Directory | Description |
|-----------|-------------|
| `simple-examples/` | Runnable examples: calculator agents, banking assistant, tool calling, streaming, memory patterns |
| `demo-compose-app/` | Kotlin Multiplatform app with Compose UI (Android, iOS, Desktop) |
| `trip-planning-example/` | Multi-API integration with Google Maps, weather, and MCP |
| `spring-boot-java/` | Spring Boot + Koog integration in Java |
| `spring-boot-kotlin/` | Spring Boot + Koog integration in Kotlin |
| `notebooks/` | Jupyter notebooks for interactive learning |
| `code-agent/` | Code analysis agent example |
| `devoxx-belgium-2025/` | Conference demo materials |

## Running Examples

```bash
# Navigate to specific example
cd simple-examples

# Run with Gradle
./gradlew run

# Or run specific example class
./gradlew run --args="CalculatorAgent"
```

## Environment Variables

Examples require API keys set as environment variables:
```bash
export OPENAI_API_KEY=your_openai_key
export ANTHROPIC_API_KEY=your_anthropic_key
```

## Key Patterns Demonstrated

- **Tool Calling**: Calculator and banking examples show function/tool integration
- **Routing**: Banking assistant demonstrates multi-path agent routing
- **Streaming**: Real-time response streaming patterns
- **Memory**: Persistence and fact storage patterns
- **Observability**: OpenTelemetry, Langfuse, and Weave integrations
- **MCP Integration**: Model Context Protocol in trip-planning example

## Documentation

- Koog Docs: https://docs.koog.ai/
- Examples Guide: https://docs.koog.ai/examples/
- API Reference: https://api.koog.ai/
