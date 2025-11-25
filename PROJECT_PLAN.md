# WardenAI - Minecraft Plugin Project Plan

## Overview
WardenAI is a Minecraft Java Edition plugin for Paper 1.21.10 that provides players with an in-game AI assistant. Players can chat with "wardenai", an AI agent powered by Groq LLM that helps navigate the game, provides advice, and has comprehensive knowledge about Minecraft's story, characters, and commands.

## Project Objectives
1. Create a Paper plugin that integrates Groq LLM API
2. Provide an in-game chat interface for players to interact with the AI
3. Enhance AI responses with a Minecraft-specific knowledge base
4. Handle API errors and token limit gracefully
5. Build as a JAR file compatible with Paper 1.21.10

## Technical Stack

### Build Tool
- **Maven** - Standard build tool for Minecraft plugins

### Target Platform
- **Paper 1.21.10** (Minecraft Java Edition server)
- Java 21+ (Paper 1.21.10 requirement)

### Dependencies
1. **Paper API** - Core plugin development framework
2. **Groq4j** - Community Java library for Groq API (available on Maven Central)
   - Repository: https://github.com/kornkutan/groq4j
   - Fallback: Direct HTTP calls using OkHttp if library doesn't work
3. **OkHttp** - HTTP client for API calls (if needed)
4. **Gson** - JSON parsing for API responses

### API Integration
- **Groq LLM API**
- Model: `openai/gpt-oss-20b`
- Configuration: Non-streaming for simpler implementation
- Authentication: API key stored in config.yml

## Architecture

### Project Structure
```
wardenai/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── wardenai/
│       │           ├── WardenAI.java           # Main plugin class
│       │           ├── commands/
│       │           │   └── WaiCommand.java     # Command handler
│       │           ├── services/
│       │           │   ├── GroqService.java    # Groq API client
│       │           │   └── KnowledgeBaseService.java # KB loader
│       │           └── utils/
│       │               └── MessageFormatter.java # Chat formatting
│       └── resources/
│           ├── plugin.yml      # Plugin metadata
│           ├── config.yml      # Configuration template
│           └── knowledge-base.txt # Minecraft knowledge base
├── pom.xml                     # Maven build configuration
├── PROJECT_PLAN.md            # This file
├── TODO.md                    # Task list
└── instructions.md            # Original requirements
```

### Core Components

#### 1. Main Plugin Class (WardenAI.java)
- Extends JavaPlugin
- Handles plugin lifecycle (onEnable, onDisable)
- Registers commands and event listeners
- Initializes services (GroqService, KnowledgeBaseService)
- Loads configuration

#### 2. Command Handler (WaiCommand.java)
- Implements CommandExecutor
- Handles `/wai <message>` command
- Validates player permissions
- Sends player queries to GroqService
- Displays responses to players

#### 3. Groq API Service (GroqService.java)
- Manages Groq API communication
- Constructs API requests with knowledge base context
- Handles API responses
- Implements error handling:
  - Network failures
  - Invalid API keys
  - Token limit exceeded
  - Rate limiting

#### 4. Knowledge Base Service (KnowledgeBaseService.java)
- Loads knowledge-base.txt on plugin startup
- Caches knowledge base content in memory
- Provides knowledge base text for prompt enhancement

#### 5. Configuration (config.yml)
```yaml
groq:
  api-key: "YOUR_GROQ_API_KEY_HERE"
  model: "openai/gpt-oss-20b"
  max-tokens: 8192
  temperature: 1.0

messages:
  prefix: "&7[&bWardenAI&7]&r"
  thinking: "WardenAI is thinking..."
  error: "Sorry, I encountered an error. Please try again later."
  no-tokens: "I'm sorry, but I can no longer help you in the game. The API token credit has been exhausted."
```

## User Experience Flow

### Happy Path
1. Player types `/wai How do I craft a diamond sword?`
2. Plugin receives command and validates input
3. Plugin shows "WardenAI is thinking..." message
4. Plugin constructs prompt:
   - System context from knowledge-base.txt
   - Player's question
5. Plugin sends request to Groq API
6. API returns response
7. Plugin formats and displays response to player

### Error Handling
- **Invalid API Key**: Notify player and log error
- **Network Error**: Display friendly error message, suggest retry
- **Token Limit Exceeded**: Display specific message about credit exhaustion
- **Timeout**: Handle gracefully with timeout message

## Implementation Phases

### Phase 1: Project Setup
- [ ] Create Maven project structure
- [ ] Configure pom.xml with dependencies
- [ ] Create plugin.yml
- [ ] Create config.yml template
- [ ] Create knowledge-base.txt placeholder

### Phase 2: Core Plugin Development
- [ ] Implement WardenAI main class
- [ ] Implement configuration loading
- [ ] Create command registration

### Phase 3: Command Implementation
- [ ] Implement WaiCommand handler
- [ ] Add input validation
- [ ] Add permission checks

### Phase 4: API Integration
- [ ] Implement GroqService with groq4j library
- [ ] Test API connectivity
- [ ] Implement prompt construction
- [ ] Handle API responses

### Phase 5: Knowledge Base Integration
- [ ] Implement KnowledgeBaseService
- [ ] Load and cache knowledge base
- [ ] Integrate KB into prompts

### Phase 6: Error Handling & Polish
- [ ] Add comprehensive error handling
- [ ] Implement token limit detection
- [ ] Add logging
- [ ] Format chat messages

### Phase 7: Testing & Build
- [ ] Test on Paper 1.21.10 server
- [ ] Verify all commands work
- [ ] Test error scenarios
- [ ] Build final JAR

## Configuration Instructions for Server Admins

After building the plugin:

1. **Install Plugin**
   - Place `WardenAI-1.0.0.jar` in the `plugins/` folder
   - Start/restart the server

2. **Configure API Key**
   - Navigate to `plugins/WardenAI/config.yml`
   - Replace `YOUR_GROQ_API_KEY_HERE` with your actual Groq API key
   - Get API key from: https://console.groq.com/

3. **Customize Knowledge Base** (Optional)
   - Edit `plugins/WardenAI/knowledge-base.txt`
   - Add Minecraft-specific information, server rules, custom lore, etc.

4. **Set Permissions** (Optional)
   - Permission node: `wardenai.use`
   - Default: All players have access

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/wai <message>` | Ask WardenAI a question | `wardenai.use` |
| `/wardenai <message>` | Alias for `/wai` | `wardenai.use` |

## Future Enhancements (Post-MVP)

- [ ] Conversation history/context (remember previous messages)
- [ ] Configurable response streaming
- [ ] Multiple knowledge base files for different contexts
- [ ] Admin commands to reload config/KB
- [ ] Cooldown system to prevent spam
- [ ] Custom key binding (requires client-side mod)
- [ ] GUI-based chat interface
- [ ] Multi-language support
- [ ] Alternative LLM providers (OpenAI, Anthropic, etc.)

## Security Considerations

1. **API Key Protection**
   - Store API key in config.yml (file permissions: 600)
   - Never log API keys
   - Add config.yml to .gitignore

2. **Input Validation**
   - Sanitize player input before sending to API
   - Limit message length to prevent abuse
   - Rate limiting per player

3. **Error Messages**
   - Don't expose internal errors to players
   - Log detailed errors for administrators

## Resources

### Groq Java Libraries
- [groq4j](https://github.com/kornkutan/groq4j) - Primary choice (Maven Central)
- [groq-java-api](https://github.com/FrankleyRocha/groq-java-api) - Alternative

### Documentation
- [Groq API Documentation](https://console.groq.com/docs/overview)
- [Groq Quickstart](https://console.groq.com/docs/quickstart)
- [Paper API Javadocs](https://jd.papermc.io/paper/1.21/index.html)
- [Spigot Plugin Tutorial](https://www.spigotmc.org/wiki/spigot-plugin-development/)

### Development Tools
- Java 21+
- Maven 3.8+
- Paper 1.21.10 server for testing
- IDE: IntelliJ IDEA / Eclipse / VS Code

## Success Criteria

- [ ] Plugin builds successfully as JAR
- [ ] Plugin loads on Paper 1.21.10 without errors
- [ ] `/wai` command responds with AI-generated answers
- [ ] Knowledge base content is included in prompts
- [ ] Token limit error displays appropriate message
- [ ] Configuration is user-friendly for server admins
- [ ] No server crashes or memory leaks

---

**Project Status**: Planning Complete - Ready for Implementation
**Target Completion**: TBD
**Maintainer**: @lipokatz-hub
