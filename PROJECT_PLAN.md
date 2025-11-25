# WardenAI - Minecraft Plugin Project Plan

## Overview
WardenAI is a Minecraft Java Edition plugin for Paper 1.21.10 that provides players with an in-game AI assistant. Players can chat with "wardenai", an AI agent powered by Groq LLM that helps navigate the game, provides advice, and has comprehensive knowledge about Minecraft's story, characters, and commands.

## Project Objectives
1. Create a Paper plugin that integrates Groq LLM API
2. Provide an in-game chat interface for players to interact with the AI
3. Enhance AI responses with a Minecraft-specific knowledge base
4. Handle API errors, rate limits, and token limits gracefully
5. Build as a JAR file compatible with Paper 1.21.10
6. Ensure thread-safe async execution to prevent server lag

## Technical Stack

### Build Tool
- **Maven** - Standard build tool for Minecraft plugins

### Target Platform
- **Paper 1.21.10** (Minecraft Java Edition server)
- **Java 21** (Paper 1.21.10 requirement - NOT compatible with older Java versions)

### Dependencies

#### 1. Paper API (Core Framework)
**CRITICAL**: Paper API is NOT on Maven Central. Must add Paper's repository.

```xml
<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.1-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### 2. Groq4j (Primary API Client)
Unofficial community library for Groq API. **Maven Central coordinates**:

```xml
<dependency>
    <groupId>io.github.kornkutan</groupId>
    <artifactId>groq4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Note**: This is an unofficial, community-maintained library. Groq does not verify its security. We will implement an OkHttp fallback as a **priority backup**, not just "if it doesn't work."

Repository: https://github.com/kornkutan/groq4j

#### 3. OkHttp (Fallback HTTP Client)
**Priority dependency** for direct Groq API calls if groq4j has issues:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

#### 4. Gson (JSON Parsing)
```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### API Integration

#### Groq LLM API Configuration
- **API Endpoint**: https://api.groq.com/openai/v1/chat/completions
- **Authentication**: API key (Bearer token)
- **Model Selection** (Default): `llama-3.3-70b-versatile`

**IMPORTANT - Model Choice**:
- ❌ **DO NOT USE**: `openai/gpt-oss-20b` - This is a Trust & Safety content moderation model, NOT a general assistant
- ✅ **RECOMMENDED**:
  - `llama-3.3-70b-versatile` - Best quality for complex Minecraft questions and detailed explanations
  - `llama-3.1-8b-instant` - Faster responses, good for simple Q&A, lower cost
- Make model configurable so server admins can choose based on their needs

**Rate Limiting**:
- Groq free tier has strict rate limits
- Must implement per-player cooldowns (default: 10 seconds)
- Must handle 429 (rate limit exceeded) responses gracefully

**Response Mode**:
- Non-streaming for simpler implementation
- Entire response received before sending to player

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
│       │           │   ├── GroqService.java    # Groq API client (async)
│       │           │   ├── CooldownManager.java # Rate limiting
│       │           │   └── KnowledgeBaseService.java # KB loader
│       │           └── utils/
│       │               ├── MessageFormatter.java # Chat formatting
│       │               └── ResponseChunker.java  # Split long messages
│       └── resources/
│           ├── plugin.yml      # Plugin metadata
│           ├── config.yml      # Configuration template
│           └── knowledge-base.txt # Minecraft knowledge base (optional)
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
- Initializes services (GroqService, KnowledgeBaseService, CooldownManager)
- Loads and validates configuration
- Provides plugin instance for async task scheduling

#### 2. Command Handler (WaiCommand.java)
- Implements CommandExecutor
- Handles `/wai <message>` command
- Validates player permissions
- Checks cooldown status
- Validates message length (min/max)
- Sends player queries to GroqService asynchronously
- Displays chunked responses to players
- Handles errors with user-friendly messages

#### 3. Groq API Service (GroqService.java)
**CRITICAL: Thread-Safe Async Execution**

This service MUST use proper async patterns from day 1. Calling Bukkit API from async threads causes server crashes.

```java
public void sendMessageAsync(Player player, String message,
                              Consumer<String> onSuccess,
                              Consumer<String> onError) {
    // Run API call on async thread (doesn't block server)
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        try {
            String response = callGroqAPI(message);
            // Return to main thread for Bukkit API calls
            Bukkit.getScheduler().runTask(plugin, () ->
                onSuccess.accept(response)
            );
        } catch (Exception e) {
            // Return to main thread for error handling
            Bukkit.getScheduler().runTask(plugin, () ->
                onError.accept(e.getMessage())
            );
        }
    });
}
```

**Responsibilities**:
- Manages Groq API communication (HTTP requests)
- Constructs API requests with knowledge base context
- Includes player name in prompt for personalized responses
- Handles API responses and error codes
- Implements proper async/sync thread switching
- Timeout handling (suggest 30 seconds)

**Error Handling - Specific Groq Error Codes**:
- **401 Unauthorized**: Invalid API key → "WardenAI is not configured. Please contact server admin."
- **429 Too Many Requests**: Rate limited → "WardenAI is busy. Please wait a moment and try again."
- **400 Bad Request** (context_length_exceeded): → "Your question is too long. Please shorten it."
- **503 Service Unavailable**: → "WardenAI is temporarily unavailable. Please try again later."
- **Timeout**: → "WardenAI took too long to respond. Please try again."
- **Network errors**: → "WardenAI cannot connect to the service. Please try again later."

#### 4. Cooldown Manager (CooldownManager.java)
**NEW - Rate Limiting Service**

Prevents spam and manages Groq API rate limits:
- Per-player cooldown tracking (HashMap<UUID, Long>)
- Configurable cooldown duration (default: 10 seconds)
- Permission bypass: `wardenai.bypass.cooldown`
- Cleanup of old cooldown entries (memory management)
- Thread-safe operations

#### 5. Knowledge Base Service (KnowledgeBaseService.java)
- Loads knowledge-base.txt on plugin startup
- **Handles missing file gracefully** (don't crash plugin)
- Caches knowledge base content in memory
- Provides knowledge base text for prompt enhancement
- Logs warning if KB file is missing
- Continues with empty/default KB if file not found

**Knowledge Base is OPTIONAL** - plugin must work without it.

#### 6. Message Utilities

**MessageFormatter.java**:
- Formats plugin messages with color codes
- Adds plugin prefix to messages
- Formats error messages consistently
- Handles Minecraft color code translation

**ResponseChunker.java**:
- Splits long AI responses into multiple messages
- Minecraft chat line limit: ~256 characters
- Adds slight delay between chunks (200ms) for readability
- Configurable max response length
- Smart splitting (avoid breaking mid-word)

#### 7. Configuration (config.yml)

```yaml
# Groq API Configuration
groq:
  # Get your API key from https://console.groq.com/
  api-key: "YOUR_GROQ_API_KEY_HERE"

  # Model Selection:
  # - llama-3.3-70b-versatile (recommended - best quality)
  # - llama-3.1-8b-instant (faster, simpler questions)
  # WARNING: DO NOT use openai/gpt-oss-20b (content moderation model)
  model: "llama-3.3-70b-versatile"

  # API Parameters
  max-tokens: 8192
  temperature: 1.0
  timeout-seconds: 30

# Rate Limiting & Protection
limits:
  cooldown-seconds: 10          # Time between player requests
  max-message-length: 500       # Maximum player question length
  min-message-length: 3         # Minimum player question length
  max-response-length: 2000     # Truncate very long AI responses

# Player-Facing Messages
messages:
  prefix: "&7[&bWardenAI&7]&r"
  thinking: "WardenAI is thinking..."
  cooldown: "Please wait {seconds} seconds before asking again."

  # Error Messages
  error-generic: "Sorry, I encountered an error. Please try again later."
  error-not-configured: "WardenAI is not configured. Please contact a server administrator."
  error-rate-limit: "WardenAI is busy. Please wait a moment and try again."
  error-too-long: "Your question is too long. Please shorten it."
  error-unavailable: "WardenAI is temporarily unavailable. Please try again later."
  error-timeout: "WardenAI took too long to respond. Please try again."

  # Original requirement - token exhaustion
  no-tokens: "I'm sorry, but I can no longer help you in the game. The API token credit has been exhausted."

# AI Personality (optional - customize the AI's character)
personality:
  enabled: true
  system-prompt: |
    You are WardenAI, a helpful and knowledgeable Minecraft assistant.
    You help players with game mechanics, crafting recipes, building tips, and general advice.
    Be friendly, concise, and accurate. Address the player by name when possible.

# Debug Settings (WARNING: May log sensitive player data)
debug:
  log-api-requests: false    # Log prompts sent to Groq
  log-api-responses: false   # Log AI responses
  log-errors: true           # Always log errors (recommended)
```

## User Experience Flow

### Happy Path
1. Player types `/wai How do I craft a diamond sword?`
2. Plugin validates command:
   - Check player has `wardenai.use` permission
   - Check message length (3-500 characters)
   - Check player cooldown (10 seconds since last request)
3. Plugin shows "WardenAI is thinking..." message
4. **Async thread**: Plugin constructs prompt:
   - System prompt with personality (optional)
   - Knowledge base content (if available)
   - Player's name and question
5. **Async thread**: Plugin sends HTTP request to Groq API
6. **Async thread**: API returns response (e.g., 150 characters)
7. **Main thread**: Plugin formats response
8. **Main thread**: Response fits in one message, send to player
9. Player cooldown is set (10 seconds)

### Long Response Flow
1. AI returns 600-character response
2. ResponseChunker splits into 3 messages:
   - Message 1: chars 0-256
   - Message 2: chars 256-512
   - Message 3: chars 512-600
3. Send with 200ms delay between each
4. Player receives full response without overwhelming chat

### Error Scenarios

**Invalid API Key** (401):
- Log: `SEVERE: Groq API key is invalid or missing`
- Player sees: "WardenAI is not configured. Please contact a server administrator."
- Admin action: Update config.yml with valid API key

**Rate Limited** (429):
- Log: `WARNING: Groq API rate limit exceeded`
- Player sees: "WardenAI is busy. Please wait a moment and try again."
- Cooldown is NOT consumed (player can retry after cooldown)

**Message Too Long**:
- Player types 600-character question
- Validation fails before API call
- Player sees: "Your question is too long (max 500 characters). Please shorten it."
- No API call made (save quota)

**Player on Cooldown**:
- Player tries to use `/wai` 5 seconds after last request
- Player sees: "Please wait 5 seconds before asking again."
- No API call made

**Knowledge Base Missing**:
- Plugin starts, knowledge-base.txt not found
- Log: `WARNING: knowledge-base.txt not found, continuing without it`
- Plugin works normally, prompts don't include KB context

## Implementation Phases

### Phase 1: Project Setup
- [ ] Create Maven project structure (src/main/java, src/main/resources)
- [ ] Create pom.xml with **Paper repository** and all dependencies
- [ ] Verify Paper API dependency uses `provided` scope
- [ ] Create plugin.yml with metadata and permissions
- [ ] Create config.yml template with all options
- [ ] Create placeholder knowledge-base.txt (optional file)
- [ ] Add .gitignore (exclude config.yml with API keys)

### Phase 2: Core Plugin Development & Configuration
- [ ] Implement WardenAI main class (extends JavaPlugin)
- [ ] Implement onEnable() - load config, initialize services
- [ ] Implement onDisable() - cleanup resources
- [ ] Create configuration loading with validation
- [ ] Verify required config fields (API key, model)
- [ ] Set default values for missing optional fields
- [ ] Test plugin loads without errors

### Phase 3: Async Infrastructure (CRITICAL - Establish Early)
**This is NOT optional polish - required architecture**

- [ ] Create async utility methods in GroqService
- [ ] Implement proper thread switching pattern:
  - `runTaskAsynchronously` for API calls
  - `runTask` for Bukkit API (player messaging)
- [ ] Add timeout handling (30 seconds default)
- [ ] Test async execution doesn't block server tick
- [ ] Document async patterns for future development

### Phase 4: Groq API Integration (With Async from Start)
- [ ] Implement GroqService class with async methods
- [ ] Try groq4j library for API calls
- [ ] **Priority**: Implement OkHttp fallback (don't wait for library failure)
- [ ] Implement prompt construction:
  - System prompt (personality)
  - Knowledge base injection
  - Player name and question
- [ ] Parse JSON responses
- [ ] Map HTTP status codes to user-friendly errors:
  - 401, 429, 400, 503, timeouts
- [ ] Test with real Groq API (require API key for testing)

### Phase 5: Command Handler & Cooldown System
- [ ] Implement CooldownManager service
- [ ] Implement WaiCommand handler
- [ ] Add permission checks (`wardenai.use`)
- [ ] Add cooldown checks (with bypass permission)
- [ ] Add message length validation (3-500 chars)
- [ ] Call GroqService asynchronously
- [ ] Display responses to players
- [ ] Register commands in plugin.yml and main class

### Phase 6: Knowledge Base Integration
- [ ] Implement KnowledgeBaseService
- [ ] Load knowledge-base.txt from plugin data folder
- [ ] Handle missing file gracefully (log warning, continue)
- [ ] Cache content in memory
- [ ] Inject KB into Groq prompts (if available)
- [ ] Test with and without KB file

### Phase 7: Message Formatting & Response Chunking
- [ ] Implement MessageFormatter utility
- [ ] Implement ResponseChunker utility
- [ ] Split responses > 256 characters
- [ ] Add 200ms delay between chunks
- [ ] Apply color codes to messages
- [ ] Test with various response lengths (short, medium, long)

### Phase 8: Security Hardening & Input Validation
- [ ] Sanitize player input:
  - Strip Minecraft color codes from input
  - Remove potentially harmful characters
  - Enforce length limits
- [ ] Implement privacy protections:
  - Don't log player messages (unless debug mode)
  - Don't log API keys
  - Sanitize error logs
- [ ] Add admin permissions:
  - `wardenai.bypass.cooldown` - skip cooldown
  - `wardenai.admin` - reload command (future)
  - `wardenai.use` - base usage permission

### Phase 9: Error Handling & Logging
- [ ] Implement comprehensive try-catch blocks
- [ ] Map all Groq API errors to user messages
- [ ] Add detailed logging for admins (configurable)
- [ ] Test all error scenarios:
  - Invalid API key
  - Network disconnected
  - Malformed responses
  - Timeout
  - Rate limiting
- [ ] Verify no stack traces shown to players

### Phase 10: Testing & Quality Assurance
- [ ] Test on Paper 1.21.10 server
- [ ] Test with valid Groq API key
- [ ] Test all error scenarios
- [ ] Test concurrent requests (5+ players simultaneously)
- [ ] Test very long questions and responses
- [ ] Test missing knowledge-base.txt
- [ ] Test invalid config.yml
- [ ] Check for memory leaks (run for 1+ hour)
- [ ] Verify async execution (server TPS stays 20)

### Phase 11: Documentation & Build
- [ ] Add JavaDoc to all public methods
- [ ] Create comprehensive README.md
- [ ] Document installation steps
- [ ] Document configuration options
- [ ] Create troubleshooting guide
- [ ] Build JAR with `mvn clean package`
- [ ] Test JAR on fresh server
- [ ] Create release notes

## Development Environment Requirements

### Required Software
- **Java Development Kit 21** (match Paper requirement)
  - Download: https://adoptium.net/temurin/releases/?version=21
  - Verify: `java -version` should show "21.x.x"
- **Maven 3.8+**
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`
- **Paper 1.21.10 Server** (for testing)
  - Download: https://papermc.io/downloads/paper
- **Groq API Key** (free tier is fine for testing)
  - Get from: https://console.groq.com/

### Recommended IDE
- **IntelliJ IDEA** (Community or Ultimate)
  - Best Minecraft plugin development support
  - Excellent Maven integration
  - Built-in Java 21 support
- Alternatives: Eclipse with Maven plugin, VS Code with Java extensions

### Testing Setup
1. Create local Paper 1.21.10 server
2. Configure server with basic settings
3. Enable debug mode in IDE for breakpoint debugging
4. Use hot-reload plugin (optional) for faster testing
5. Monitor server console for errors
6. Test with multiple player accounts (alt accounts or friends)

## Configuration Instructions for Server Admins

### Initial Setup

1. **Install Plugin**
   - Download `WardenAI-1.0.0.jar`
   - Place in `plugins/` folder of Paper 1.21.10 server
   - Start server (plugin will create default config)
   - Stop server before configuring

2. **Get Groq API Key**
   - Visit https://console.groq.com/
   - Sign up or log in
   - Navigate to API Keys section
   - Create new API key
   - Copy the key (starts with `gsk_...`)

3. **Configure Plugin**
   - Open `plugins/WardenAI/config.yml`
   - Replace `YOUR_GROQ_API_KEY_HERE` with your actual API key
   - Choose model (keep default `llama-3.3-70b-versatile` for best quality)
   - Adjust cooldown if needed (default 10 seconds is recommended)
   - Save file

4. **Customize Knowledge Base** (Optional)
   - Edit `plugins/WardenAI/knowledge-base.txt`
   - Add server-specific information:
     - Custom server rules
     - Special commands or features
     - Server lore or storyline
     - Building guidelines
   - If file is missing, plugin works fine without it

5. **Set Permissions** (Optional)
   - Default: All players can use `/wai`
   - To restrict: Remove `wardenai.use` from default permissions
   - Grant to specific groups/players with permission plugin
   - Admin bypass: `wardenai.bypass.cooldown`

6. **Start Server**
   - Start the Paper server
   - Check console for: `[WardenAI] Plugin enabled successfully`
   - If errors, check config.yml for typos

### Troubleshooting

**"WardenAI is not configured"**
- API key is invalid or missing in config.yml
- Check for typos in API key
- Verify API key is active at https://console.groq.com/

**"WardenAI is busy"**
- Groq API rate limit exceeded
- Free tier has low limits - consider upgrade
- Increase cooldown-seconds to reduce requests

**Plugin doesn't load**
- Check Java version: Must be Java 21
- Check Paper version: Must be 1.21+
- Check console for specific error messages

**Responses are cut off**
- Increase max-response-length in config.yml
- Note: Very long responses split into chunks

## Commands & Permissions

### Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/wai <message>` | Ask WardenAI a question | `wardenai.use` |
| `/wardenai <message>` | Alias for `/wai` | `wardenai.use` |

### Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `wardenai.use` | Use the `/wai` command | `true` (all players) |
| `wardenai.bypass.cooldown` | Skip cooldown timer | `op` (operators) |
| `wardenai.admin` | Admin commands (future) | `op` (operators) |

## Future Enhancements (Post-MVP)

### High Priority
- [ ] `/wai reload` command - reload config without restart
- [ ] Conversation history (remember previous messages per player)
- [ ] `/wai clear` - clear conversation history
- [ ] OkHttp fallback verification (test both paths)

### Medium Priority
- [ ] Response streaming (show AI typing effect)
- [ ] Multiple knowledge base files per-world or per-gamemode
- [ ] GUI-based chat interface (inventory menu)
- [ ] Customizable AI personality per-server
- [ ] Player statistics tracking
- [ ] Usage analytics for admins

### Low Priority
- [ ] Multi-language support (translate messages)
- [ ] Alternative LLM providers (OpenAI, Anthropic Claude)
- [ ] Voice-to-text integration (requires client mod)
- [ ] Custom key binding (requires client mod)
- [ ] Integration with other plugins (economy, quests, etc.)

## Security Considerations

### API Key Protection
- Store API key in config.yml (file permissions: 600 recommended)
- **Never log API keys** in console or files
- Add config.yml to .gitignore if sharing code
- Rotate API keys periodically
- Use read-only API keys if Groq supports it

### Input Validation & Sanitization
- Strip Minecraft color codes from player input
- Limit special characters (prevent injection attempts)
- Enforce message length limits (3-500 characters)
- Don't send raw player input to logs (privacy)
- Validate all user input before API calls

### Rate Limiting & Abuse Prevention
- Per-player cooldown (10 seconds default)
- Permission-based cooldown bypass (admins only)
- Global request queue (prevent server-wide spam)
- Monitor API usage (detect abuse patterns)
- Consider IP-based limits for shared accounts

### Error Message Safety
- Don't expose internal errors to players
- Don't reveal API endpoints or keys in errors
- Log detailed errors for admins only
- Sanitize stack traces before logging
- Map technical errors to friendly messages

### Privacy Considerations
- Don't log player messages by default
- Make debug logging opt-in
- Inform players their questions are sent to external API
- Consider GDPR compliance for EU servers
- Provide way to opt-out of data collection

## Resources

### Groq Java Libraries
- [groq4j](https://github.com/kornkutan/groq4j) - Primary choice (unofficial)
- [groq-java-api](https://github.com/FrankleyRocha/groq-java-api) - Alternative (unofficial)
- **Note**: Both are community projects, not officially supported by Groq

### Official Documentation
- [Groq API Documentation](https://console.groq.com/docs/overview)
- [Groq API Reference](https://console.groq.com/docs/api-reference)
- [Groq Quickstart](https://console.groq.com/docs/quickstart)
- [Groq Models](https://console.groq.com/docs/models)
- [Paper API Javadocs](https://jd.papermc.io/paper/1.21/index.html)
- [Spigot Plugin Tutorial](https://www.spigotmc.org/wiki/spigot-plugin-development/)
- [Bukkit Scheduler Programming](https://www.spigotmc.org/wiki/scheduler-programming/)

### Development Tools
- [Adoptium JDK 21](https://adoptium.net/temurin/releases/?version=21)
- [Apache Maven](https://maven.apache.org/download.cgi)
- [Paper Downloads](https://papermc.io/downloads/paper)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)

## Success Criteria

### Functional Requirements
- [ ] Plugin builds successfully as JAR
- [ ] Plugin loads on Paper 1.21.10 without errors
- [ ] `/wai` command responds with AI-generated answers
- [ ] Knowledge base content is included in prompts (when file exists)
- [ ] All error codes display appropriate user-friendly messages
- [ ] Cooldown system prevents spam
- [ ] Long responses are properly chunked
- [ ] Async execution doesn't block server (TPS stays ~20)

### Non-Functional Requirements
- [ ] Configuration is user-friendly for server admins
- [ ] No server crashes or memory leaks
- [ ] No Bukkit API calls from async threads
- [ ] Proper error handling for all edge cases
- [ ] Clear documentation for installation and configuration
- [ ] Code is maintainable and well-documented
- [ ] Security best practices followed

### Testing Checklist
- [ ] Tested with valid API key
- [ ] Tested with invalid API key
- [ ] Tested with no API key
- [ ] Tested with missing config.yml
- [ ] Tested with missing knowledge-base.txt
- [ ] Tested with network disconnected
- [ ] Tested with concurrent players (5+)
- [ ] Tested with very long questions
- [ ] Tested with very short questions
- [ ] Tested with special characters
- [ ] Tested cooldown system
- [ ] Tested permission system
- [ ] Tested on Paper 1.21.10 server
- [ ] Tested async execution (no lag spikes)

---

**Project Status**: Planning Complete - Ready for Implementation
**Target Completion**: TBD
**Maintainer**: @lipokatz-hub
**Last Updated**: 2025-11-25 (Refined with feasibility assessment findings)
