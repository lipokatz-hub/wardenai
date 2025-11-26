# WardenAI - Development TODO List

This document tracks all tasks for implementing the WardenAI Minecraft plugin.

## Legend
- [ ] Not Started
- [x] Completed
- [~] In Progress
- [!] Blocked
- [⚠] Critical - Must not skip or defer

---

## Phase 1: Project Setup & Structure

### 1.1 Maven Project Structure
- [ ] Create `src/main/java/com/wardenai/` directory structure
- [ ] Create `src/main/resources/` directory
- [ ] Create package structure:
  - [ ] `com.wardenai` (main package)
  - [ ] `com.wardenai.commands` (command handlers)
  - [ ] `com.wardenai.services` (API and business logic)
  - [ ] `com.wardenai.utils` (utilities)
- [ ] Create `.gitignore` file (exclude IDE files, target/, config.yml with API keys)

### 1.2 Build Configuration (pom.xml)
- [⚠] **CRITICAL**: Add Paper repository (NOT on Maven Central):
  ```xml
  <repositories>
      <repository>
          <id>papermc</id>
          <url>https://repo.papermc.io/repository/maven-public/</url>
      </repository>
  </repositories>
  ```
- [⚠] Add Paper API dependency with `provided` scope:
  ```xml
  <dependency>
      <groupId>io.papermc.paper</groupId>
      <artifactId>paper-api</artifactId>
      <version>1.21.1-R0.1-SNAPSHOT</version>
      <scope>provided</scope>
  </dependency>
  ```
- [ ] Add Groq4j library dependency (correct coordinates):
  ```xml
  <dependency>
      <groupId>io.github.kornkutan</groupId>
      <artifactId>groq4j</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```
- [⚠] **PRIORITY**: Add OkHttp dependency (not just fallback):
  ```xml
  <dependency>
      <groupId>com.squareup.okhttp3</groupId>
      <artifactId>okhttp</artifactId>
      <version>4.12.0</version>
  </dependency>
  ```
- [ ] Add Gson dependency for JSON parsing:
  ```xml
  <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.10.1</version>
  </dependency>
  ```
- [ ] Configure Maven Shade plugin for fat JAR
- [ ] Configure Java 21 compiler (match Paper requirement)
- [ ] Set project version to 1.0.0-SNAPSHOT

### 1.3 Plugin Metadata (plugin.yml)
- [ ] Create `plugin.yml` in src/main/resources with:
  - [ ] Plugin name: WardenAI
  - [ ] Version: 1.0.0
  - [ ] Main class: com.wardenai.WardenAI
  - [ ] API version: 1.21
  - [ ] Description and author info
  - [ ] Commands section:
    - [ ] `/wai` - Ask WardenAI a question
    - [ ] `/wardenai` - Alias for /wai
  - [ ] Permissions section:
    - [ ] `wardenai.use` - Use the /wai command (default: true)
    - [ ] `wardenai.bypass.cooldown` - Skip cooldown timer (default: op)
    - [ ] `wardenai.admin` - Admin commands (default: op)

### 1.4 Configuration Files
- [⚠] Create `config.yml` template in src/main/resources with:
  - [ ] **Groq section**:
    - [ ] `api-key: "YOUR_GROQ_API_KEY_HERE"`
    - [⚠] **CRITICAL**: `model: "llama-3.3-70b-versatile"` (NOT openai/gpt-oss-20b)
    - [ ] `max-tokens: 8192`
    - [ ] `temperature: 1.0`
    - [ ] `timeout-seconds: 30`
  - [ ] **Limits section** (NEW):
    - [ ] `cooldown-seconds: 10`
    - [ ] `max-message-length: 500`
    - [ ] `min-message-length: 3`
    - [ ] `max-response-length: 2000`
  - [ ] **Messages section**:
    - [ ] `prefix: "&7[&bWardenAI&7]&r"`
    - [ ] `thinking: "WardenAI is thinking..."`
    - [ ] `cooldown: "Please wait {seconds} seconds before asking again."`
    - [ ] Error messages (generic, not-configured, rate-limit, too-long, unavailable, timeout)
    - [ ] `no-tokens` message (original requirement)
  - [ ] **Personality section** (NEW - optional AI customization):
    - [ ] `enabled: true`
    - [ ] `system-prompt` with default personality
  - [ ] **Debug section** (NEW):
    - [ ] `log-api-requests: false`
    - [ ] `log-api-responses: false`
    - [ ] `log-errors: true`
- [ ] Create `knowledge-base.txt` in src/main/resources
  - [ ] Add placeholder text: "minecraft is a fun game"
  - [ ] Add comment: "# This file is optional. Plugin will work without it."

---

## Phase 2: Core Plugin Development & Configuration

### 2.1 Main Plugin Class (WardenAI.java)
- [ ] Create WardenAI class extending JavaPlugin
- [ ] Add private fields for services:
  - [ ] GroqService groqService
  - [ ] KnowledgeBaseService knowledgeBaseService
  - [ ] CooldownManager cooldownManager
- [ ] Implement `onEnable()` method:
  - [ ] Log plugin startup message
  - [ ] Save default config if missing (`saveDefaultConfig()`)
  - [ ] Load and validate configuration
  - [ ] Initialize KnowledgeBaseService (handle missing file gracefully)
  - [ ] Initialize CooldownManager
  - [ ] Initialize GroqService with config values
  - [ ] Register commands (WaiCommand)
  - [ ] Log successful initialization
- [ ] Implement `onDisable()` method:
  - [ ] Cleanup cooldown manager
  - [ ] Cleanup resources
  - [ ] Log plugin shutdown message
- [ ] Add getter methods for services (for command access)

### 2.2 Configuration Management
- [ ] Create method `loadConfiguration()`:
  - [ ] Reload config from disk
  - [ ] Validate required fields (API key, model)
  - [ ] Log warning if API key is still placeholder
  - [ ] Return boolean success status
- [ ] Create getter methods for config values:
  - [ ] `getGroqApiKey()` - get API key
  - [ ] `getGroqModel()` - get model name
  - [ ] `getMaxTokens()` - get max tokens
  - [ ] `getTemperature()` - get temperature
  - [ ] `getTimeout()` - get timeout in seconds
  - [ ] `getCooldownSeconds()` - get cooldown duration
  - [ ] `getMaxMessageLength()` - get max message length
  - [ ] `getMinMessageLength()` - get min message length
  - [ ] `getMessage(String key)` - get message from config
- [ ] Add validation:
  - [ ] Check API key is not empty or placeholder
  - [ ] Check model name is not openai/gpt-oss-20b (warn if so)
  - [ ] Validate numeric values are positive
- [ ] Add method to check if debug logging is enabled

---

## Phase 3: Async Infrastructure (CRITICAL - Establish Early)

**⚠ This is NOT optional polish - it's required architecture. Must establish before API integration.**

### 3.1 Async Pattern Documentation
- [ ] Document the correct async pattern in code comments:
  ```java
  // CORRECT: API call on async thread, Bukkit API on main thread
  // WRONG: Calling player.sendMessage() from async thread = CRASH
  ```
- [ ] Add reference to Bukkit scheduler documentation

### 3.2 Async Utility Methods (in GroqService)
- [⚠] Create `sendMessageAsync()` method signature:
  ```java
  public void sendMessageAsync(Player player, String message,
                                Consumer<String> onSuccess,
                                Consumer<String> onError)
  ```
- [⚠] Implement proper thread switching:
  - [ ] Use `Bukkit.getScheduler().runTaskAsynchronously()` for API call
  - [ ] Use `Bukkit.getScheduler().runTask()` for callbacks (main thread)
  - [ ] Ensure all Bukkit API calls happen on main thread
- [ ] Add timeout handling (30 seconds default):
  - [ ] Create timeout task that cancels if too slow
  - [ ] Call error callback if timeout occurs
- [ ] Test async execution:
  - [ ] Verify server TPS stays at 20 during API calls
  - [ ] Verify no "async entity access" errors in console

### 3.3 Thread Safety
- [ ] Ensure all shared data structures are thread-safe
- [ ] Use proper synchronization for cooldown manager
- [ ] Document which methods are called from which threads

---

## Phase 4: Groq API Integration (With Async from Start)

### 4.1 GroqService Class (GroqService.java)
- [ ] Create GroqService class
- [ ] Add private fields:
  - [ ] JavaPlugin plugin (for scheduler access)
  - [ ] String apiKey
  - [ ] String model
  - [ ] int maxTokens
  - [ ] double temperature
  - [ ] int timeout
- [ ] Add constructor accepting plugin instance and config values
- [⚠] Implement `sendMessageAsync()` with proper async pattern (from Phase 3)

### 4.2 Groq API Client Implementation
- [ ] **Try groq4j library first**:
  - [ ] Initialize groq4j client with API key
  - [ ] Test basic API call
  - [ ] Handle library-specific exceptions
- [⚠] **PRIORITY: Implement OkHttp fallback** (don't wait for library to fail):
  - [ ] Create `callGroqWithOkHttp()` method
  - [ ] Build HTTP POST request to https://api.groq.com/openai/v1/chat/completions
  - [ ] Set headers: Authorization: Bearer {api-key}, Content-Type: application/json
  - [ ] Build JSON request body with Gson
  - [ ] Parse JSON response with Gson
  - [ ] Extract message content
- [ ] Add method to choose between groq4j and OkHttp (config option or auto-fallback)

### 4.3 Prompt Construction
- [ ] Create `buildPrompt()` method:
  - [ ] Accept player name, player message, knowledge base content
  - [ ] Build messages array:
    - [ ] System message with personality (if enabled in config)
    - [ ] System message with knowledge base (if available)
    - [ ] User message with player name: "Player {name} asks: {question}"
  - [ ] Return messages array for API call
- [ ] Test prompt includes all context correctly

### 4.4 Error Handling - Specific Groq Error Codes
- [⚠] Map HTTP status codes to user-friendly errors:
  - [ ] **401 Unauthorized**: "WardenAI is not configured. Please contact server admin."
  - [ ] **429 Too Many Requests**: "WardenAI is busy. Please wait a moment and try again."
  - [ ] **400 Bad Request** (check for "context_length_exceeded"): "Your question is too long. Please shorten it."
  - [ ] **503 Service Unavailable**: "WardenAI is temporarily unavailable. Please try again later."
  - [ ] **Timeout**: "WardenAI took too long to respond. Please try again."
  - [ ] **Network errors** (IOException): "WardenAI cannot connect to the service. Please try again later."
- [ ] Parse error response JSON to get detailed error messages
- [ ] Log detailed errors for admins (if debug enabled)
- [ ] Return user-friendly errors to players (never expose API details)

### 4.5 Response Parsing
- [ ] Parse JSON response structure:
  ```json
  {
    "choices": [
      {
        "message": {
          "content": "AI response here"
        }
      }
    ]
  }
  ```
- [ ] Extract content from first choice
- [ ] Validate response is not empty
- [ ] Trim whitespace from response
- [ ] Return response text

### 4.6 Testing
- [ ] Test with valid API key and real Groq API
- [ ] Test with invalid API key (expect 401)
- [ ] Test with very long message (expect 400)
- [ ] Test with network disconnected (expect timeout/network error)
- [ ] Test async execution doesn't block server

---

## Phase 5: Command Handler & Cooldown System

### 5.1 CooldownManager Service (CooldownManager.java) - NEW
- [ ] Create CooldownManager class
- [ ] Add private field: `HashMap<UUID, Long> cooldowns` (player UUID -> last use timestamp)
- [ ] Implement `setCooldown(Player player)` method:
  - [ ] Store current timestamp for player UUID
- [ ] Implement `getRemainingCooldown(Player player)` method:
  - [ ] Check if player has cooldown entry
  - [ ] Calculate remaining time (cooldown duration - elapsed time)
  - [ ] Return 0 if cooldown expired, otherwise remaining seconds
- [ ] Implement `hasCooldown(Player player)` method:
  - [ ] Return true if remaining cooldown > 0
  - [ ] Check for bypass permission (`wardenai.bypass.cooldown`)
- [ ] Implement cleanup method:
  - [ ] Remove expired cooldowns (prevent memory leak)
  - [ ] Run periodically (every 5 minutes)
- [ ] Make thread-safe (synchronize map access)

### 5.2 WaiCommand Handler (WaiCommand.java)
- [ ] Create WaiCommand class implementing CommandExecutor
- [ ] Add private fields:
  - [ ] WardenAI plugin instance
  - [ ] GroqService groqService
  - [ ] KnowledgeBaseService knowledgeBaseService
  - [ ] CooldownManager cooldownManager
- [ ] Add constructor accepting plugin instance

### 5.3 Command Execution Logic
- [ ] Implement `onCommand()` method with validation chain:

  **Step 1: Sender Validation**
  - [ ] Check if sender is a Player (not console)
  - [ ] If console, return error message

  **Step 2: Permission Check**
  - [ ] Check player has `wardenai.use` permission
  - [ ] If not, return permission error

  **Step 3: Arguments Validation**
  - [ ] Check args.length > 0 (player provided a message)
  - [ ] Join args into single message string
  - [ ] If empty, return usage message

  **Step 4: Message Length Validation**
  - [ ] Get min/max length from config
  - [ ] Check message length is >= min length
  - [ ] Check message length is <= max length
  - [ ] If invalid, return appropriate error message

  **Step 5: Input Sanitization** (Security)
  - [ ] Strip Minecraft color codes from player input
  - [ ] Remove potentially harmful characters
  - [ ] Trim whitespace

  **Step 6: Cooldown Check**
  - [ ] Check if player has active cooldown
  - [ ] If yes, get remaining seconds
  - [ ] Send cooldown message (replace {seconds} placeholder)
  - [ ] Return early (don't make API call)

  **Step 7: Send "Thinking" Message**
  - [ ] Get thinking message from config
  - [ ] Format with MessageFormatter
  - [ ] Send to player

  **Step 8: Call Groq API Asynchronously**
  - [ ] Get knowledge base content (may be empty)
  - [ ] Call groqService.sendMessageAsync() with:
    - [ ] Player instance
    - [ ] Sanitized message
    - [ ] Success callback
    - [ ] Error callback

  **Step 9: Success Callback** (runs on main thread)
  - [ ] Receive AI response text
  - [ ] Chunk response if > 256 characters (use ResponseChunker)
  - [ ] Send chunks to player with delays
  - [ ] Set player cooldown

  **Step 10: Error Callback** (runs on main thread)
  - [ ] Receive error message
  - [ ] Format error with MessageFormatter
  - [ ] Send to player
  - [ ] Do NOT set cooldown on error (allow retry)

### 5.4 Command Registration
- [ ] Register command in WardenAI.onEnable():
  ```java
  getCommand("wai").setExecutor(new WaiCommand(this));
  getCommand("wardenai").setExecutor(new WaiCommand(this));
  ```
- [ ] Verify commands are defined in plugin.yml
- [ ] Test command works in-game

---

## Phase 6: Knowledge Base Integration

### 6.1 KnowledgeBaseService Class (KnowledgeBaseService.java)
- [ ] Create KnowledgeBaseService class
- [ ] Add private field: `String knowledgeBaseContent`
- [ ] Add constructor accepting plugin instance

### 6.2 Knowledge Base Loading (with Graceful Failure)
- [⚠] Implement `loadKnowledgeBase()` method:
  - [ ] Try to read knowledge-base.txt from plugin data folder
  - [ ] If file doesn't exist, try to read from plugin resources
  - [ ] If still not found:
    - [ ] Log WARNING (not ERROR): "knowledge-base.txt not found, continuing without it"
    - [ ] Set knowledgeBaseContent to empty string
    - [ ] **DO NOT crash plugin** - knowledge base is optional
  - [ ] If found, read entire file into string
  - [ ] Store in memory (cache)
  - [ ] Log INFO: "Knowledge base loaded successfully ({size} characters)"

### 6.3 Knowledge Base Access
- [ ] Implement `getKnowledgeBase()` method:
  - [ ] Return cached knowledge base content (may be empty)
- [ ] Implement `hasKnowledgeBase()` method:
  - [ ] Return true if content is not empty
- [ ] Implement `reload()` method (for future /wai reload command):
  - [ ] Re-read file from disk
  - [ ] Update cached content

### 6.4 Integration with Groq API
- [ ] In GroqService prompt construction:
  - [ ] Check if knowledge base is available
  - [ ] If yes, include in system message
  - [ ] If no, skip KB section (still work normally)
- [ ] Test plugin works with missing knowledge-base.txt
- [ ] Test plugin works with knowledge-base.txt present
- [ ] Test KB content appears in AI responses

---

## Phase 7: Message Formatting & Response Chunking

### 7.1 MessageFormatter Class (MessageFormatter.java)
- [ ] Create MessageFormatter utility class
- [ ] Implement `formatMessage(String prefix, String message)` method:
  - [ ] Combine prefix and message
  - [ ] Translate color codes (& to §)
  - [ ] Return formatted string
- [ ] Implement `formatError(String error)` method:
  - [ ] Add error prefix (red color)
  - [ ] Format consistently
- [ ] Implement `translateColorCodes(String text)` method:
  - [ ] Replace & with § for Minecraft colors
  - [ ] Support all standard color codes (&a, &b, etc.)

### 7.2 ResponseChunker Class (ResponseChunker.java) - NEW
- [ ] Create ResponseChunker utility class
- [ ] Implement `chunkMessage(String message, int maxLength)` method:
  - [ ] Split message into chunks of maxLength (default 256)
  - [ ] Try to split at word boundaries (don't break mid-word)
  - [ ] Return List<String> of chunks
- [ ] Implement smart splitting algorithm:
  - [ ] Find last space before maxLength
  - [ ] If no space found, hard-split at maxLength
  - [ ] Continue until entire message is chunked

### 7.3 Response Sending with Chunking
- [ ] In WaiCommand success callback:
  - [ ] Get max line length from config (default 256)
  - [ ] Check if response length > max line length
  - [ ] If yes, chunk the response
  - [ ] Send each chunk with delay:
    ```java
    for (int i = 0; i < chunks.size(); i++) {
        final String chunk = chunks.get(i);
        final int delay = i * 4; // 200ms per chunk (4 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            player.sendMessage(chunk), delay);
    }
    ```
  - [ ] If no, send single message

### 7.4 Testing
- [ ] Test short response (< 256 chars) - single message
- [ ] Test long response (> 256 chars) - multiple chunks
- [ ] Test very long response (> 1000 chars) - truncate if needed
- [ ] Verify chunks are sent with delays
- [ ] Verify color codes work correctly

---

## Phase 8: Security Hardening & Input Validation

### 8.1 Input Sanitization
- [ ] Create `sanitizeInput(String input)` method in WaiCommand:
  - [ ] Strip all Minecraft color codes (§, &)
  - [ ] Remove control characters
  - [ ] Trim leading/trailing whitespace
  - [ ] Replace multiple spaces with single space
  - [ ] Limit special characters (keep alphanumeric, basic punctuation)
- [ ] Apply sanitization before sending to API
- [ ] Test with various malicious inputs

### 8.2 Privacy Protection
- [ ] **Never log player messages** unless debug mode enabled
- [ ] In debug mode, add clear warning in config
- [ ] **Never log API keys** in any circumstance
- [ ] Sanitize error messages before logging:
  - [ ] Remove API keys from stack traces
  - [ ] Remove player names from logs (use UUID instead)
- [ ] Add privacy notice in README (data sent to external API)

### 8.3 Permission System
- [ ] Verify all permissions work:
  - [ ] `wardenai.use` - base usage (default: true)
  - [ ] `wardenai.bypass.cooldown` - skip cooldown (default: op)
  - [ ] `wardenai.admin` - admin commands (default: op)
- [ ] Test permission denial messages
- [ ] Test cooldown bypass for ops

### 8.4 API Key Security
- [ ] Add check in onEnable(): if API key is still placeholder, log SEVERE warning
- [ ] Don't send requests if API key is placeholder
- [ ] Return configuration error to players
- [ ] Document in README: set file permissions on config.yml (chmod 600)
- [ ] Add config.yml to .gitignore template

### 8.5 Rate Limiting
- [ ] Cooldown system prevents player spam ✓ (already implemented in Phase 5)
- [ ] Consider global rate limit (max requests per minute server-wide):
  - [ ] Optional: Add queue system if needed
  - [ ] For now, per-player cooldown is sufficient

---

## Phase 9: Error Handling & Logging

### 9.1 Exception Handling
- [ ] Wrap all API calls in try-catch blocks
- [ ] Handle specific exceptions:
  - [ ] IOException - network errors
  - [ ] JsonSyntaxException - malformed JSON
  - [ ] NullPointerException - missing data
  - [ ] TimeoutException - request timeout
  - [ ] Generic Exception - catch-all
- [ ] Never let exceptions crash the plugin
- [ ] Always log exceptions (use plugin logger)

### 9.2 Logging System
- [ ] Use appropriate log levels:
  - [ ] **INFO**: Plugin startup/shutdown, successful operations
  - [ ] **WARNING**: Missing files, API rate limits, invalid config
  - [ ] **SEVERE**: Invalid API key, critical failures, initialization errors
  - [ ] **FINE** (debug): API requests/responses (only if debug enabled)
- [ ] Create logging methods:
  - [ ] `logInfo(String message)`
  - [ ] `logWarning(String message)`
  - [ ] `logError(String message, Throwable throwable)`
  - [ ] `logDebug(String message)` - only logs if debug enabled
- [ ] **Never log sensitive data**:
  - [ ] No API keys
  - [ ] No player messages (unless debug explicitly enabled)
  - [ ] No raw player input

### 9.3 User-Facing Error Messages
- [ ] Map all technical errors to friendly messages:
  - [ ] Network error → "Cannot connect to service"
  - [ ] Invalid API key → "Not configured, contact admin"
  - [ ] Rate limit → "Too busy, try again"
  - [ ] Timeout → "Took too long, try again"
- [ ] Get error messages from config.yml (customizable)
- [ ] Format errors consistently with MessageFormatter
- [ ] Test all error scenarios and verify messages

### 9.4 Error Recovery
- [ ] On error, player can retry (don't consume cooldown)
- [ ] On network error, suggest retry
- [ ] On configuration error, direct to admin
- [ ] On rate limit, suggest waiting

---

## Phase 10: Testing & Quality Assurance

### 10.1 Unit Tests (Optional but Recommended)
- [ ] Test configuration loading:
  - [ ] Valid config
  - [ ] Missing fields (should use defaults)
  - [ ] Invalid values
- [ ] Test knowledge base loading:
  - [ ] File exists
  - [ ] File missing (should not crash)
  - [ ] Empty file
- [ ] Test input sanitization:
  - [ ] Color codes removed
  - [ ] Special characters handled
  - [ ] Length validation
- [ ] Test response chunking:
  - [ ] Short message (no chunking)
  - [ ] Long message (chunked)
  - [ ] Very long message
- [ ] Test cooldown manager:
  - [ ] Set and check cooldown
  - [ ] Bypass permission
  - [ ] Cleanup old entries

### 10.2 Integration Testing on Paper Server
- [⚠] **Test with valid Groq API key**:
  - [ ] Plugin loads successfully
  - [ ] /wai command responds with AI answers
  - [ ] Knowledge base content appears in responses
  - [ ] Cooldown system works
  - [ ] Long responses are chunked

- [⚠] **Test error scenarios**:
  - [ ] Invalid API key in config → appropriate error message
  - [ ] No API key → configuration error
  - [ ] Network disconnected → connection error
  - [ ] Very long question → validation error
  - [ ] Player on cooldown → cooldown message

- [ ] **Test edge cases**:
  - [ ] Empty message → usage error
  - [ ] Message with only spaces → validation error
  - [ ] Message with color codes → sanitized before API
  - [ ] Message with Unicode/emoji → handled correctly
  - [ ] Player disconnects during API call → no crash
  - [ ] Multiple players using simultaneously → all work correctly

### 10.3 Performance Testing
- [ ] Test server TPS during API calls:
  - [ ] Should stay at 20 TPS (async working correctly)
  - [ ] No lag spikes when AI responds
- [ ] Test with multiple concurrent players:
  - [ ] 5+ players use /wai simultaneously
  - [ ] All receive responses
  - [ ] No race conditions or deadlocks
- [ ] Test memory usage:
  - [ ] Plugin doesn't leak memory
  - [ ] Knowledge base cached properly
  - [ ] Cooldown map cleaned up
- [ ] Run plugin for 1+ hour:
  - [ ] Check for memory leaks
  - [ ] Verify stability
  - [ ] Check logs for errors

### 10.4 Security Testing
- [ ] Test permission system:
  - [ ] Player without wardenai.use cannot use command
  - [ ] Operator can bypass cooldown
- [ ] Test input sanitization:
  - [ ] Color codes stripped
  - [ ] No injection attacks possible
- [ ] Test logging:
  - [ ] API keys never logged
  - [ ] Player messages not logged (unless debug)
  - [ ] Errors logged properly

### 10.5 Configuration Testing
- [ ] Test with missing config.yml:
  - [ ] Default config generated
  - [ ] Plugin works with defaults
- [ ] Test with missing knowledge-base.txt:
  - [ ] Warning logged
  - [ ] Plugin works without it
- [ ] Test with invalid config values:
  - [ ] Negative numbers handled
  - [ ] Missing required fields handled
  - [ ] Wrong model name warned

---

## Phase 11: Documentation & Build

### 11.1 Code Documentation
- [ ] Add JavaDoc to all public classes
- [ ] Add JavaDoc to all public methods:
  - [ ] @param descriptions
  - [ ] @return descriptions
  - [ ] @throws descriptions
- [ ] Add class-level documentation explaining purpose
- [ ] Add inline comments for complex logic:
  - [ ] Async thread switching
  - [ ] Prompt construction
  - [ ] Error handling
- [ ] Document thread safety requirements

### 11.2 User Documentation (README.md)
- [ ] Create comprehensive README.md:

  **Installation**
  - [ ] Download JAR
  - [ ] Place in plugins folder
  - [ ] Restart server
  - [ ] Configure API key

  **Configuration**
  - [ ] How to get Groq API key (with link)
  - [ ] Explain each config option
  - [ ] Model selection guide (llama-3.3-70b-versatile vs llama-3.1-8b-instant)
  - [ ] Cooldown customization
  - [ ] Knowledge base customization

  **Usage**
  - [ ] Command examples
  - [ ] Permission setup
  - [ ] Cooldown bypass for admins

  **Troubleshooting**
  - [ ] "WardenAI is not configured" → check API key
  - [ ] "WardenAI is busy" → rate limit, increase cooldown
  - [ ] Plugin doesn't load → check Java 21, Paper version
  - [ ] Common error messages and solutions

  **FAQ**
  - [ ] Is my data sent to external API? (Yes, Groq)
  - [ ] Is knowledge base required? (No, optional)
  - [ ] Can I customize AI personality? (Yes, config.yml)
  - [ ] What's the rate limit? (Depends on Groq tier)

### 11.3 Developer Documentation
- [ ] Document build process:
  - [ ] Prerequisites (Java 21, Maven 3.8+)
  - [ ] Clone repository
  - [ ] Run `mvn clean package`
  - [ ] Find JAR in target/
- [ ] Document project structure
- [ ] Document async architecture (critical for contributors)
- [ ] Document Groq API integration
- [ ] Add contribution guidelines (if open source)

### 11.4 Build Process
- [ ] Run `mvn clean package`
- [ ] Verify JAR created in target/ folder
- [ ] Check JAR size (should be reasonable, < 5MB)
- [ ] Verify all dependencies are shaded correctly:
  - [ ] groq4j included
  - [ ] OkHttp included
  - [ ] Gson included
  - [ ] Paper API NOT included (provided scope)
- [ ] Test JAR on fresh Paper 1.21.10 server:
  - [ ] Plugin loads
  - [ ] Commands work
  - [ ] Config generates
  - [ ] No missing dependencies

### 11.5 Release Preparation
- [ ] Create release notes for version 1.0.0:
  - [ ] List features
  - [ ] Installation instructions
  - [ ] Known limitations
  - [ ] Credits and acknowledgments
- [ ] Create CHANGELOG.md
- [ ] Tag release in git: `v1.0.0`
- [ ] Create GitHub release (if applicable)

---

## Future Enhancements (Post-MVP)

### High Priority
- [ ] `/wai reload` command - reload config and knowledge base without restart
- [ ] Conversation history (remember previous messages per player)
- [ ] `/wai clear` - clear personal conversation history
- [ ] OkHttp fallback verification (ensure both groq4j and OkHttp paths work)
- [ ] Admin statistics: /wai stats (total requests, most active players, etc.)

### Medium Priority
- [ ] Response streaming (show AI "typing" effect, word-by-word)
- [ ] Multiple knowledge base files (per-world or per-gamemode)
- [ ] GUI-based chat interface (inventory menu instead of commands)
- [ ] Customizable AI personality per-world or per-player
- [ ] Player statistics (questions asked, favorite topics)
- [ ] Integration with economy plugins (charge for AI queries)

### Low Priority
- [ ] Multi-language support (translate plugin messages)
- [ ] Alternative LLM providers (OpenAI GPT, Anthropic Claude)
- [ ] Voice-to-text integration (requires client mod)
- [ ] Custom key binding for chat (requires client mod)
- [ ] Integration with quest plugins (AI can give hints)
- [ ] Analytics dashboard (web interface for admins)

---

## Blockers & Dependencies

### External Dependencies
- **Groq API access** - requires valid API key (free tier available)
- **Paper 1.21.10 server** - for testing (download from papermc.io)
- **Internet connection** - for API calls (plugin won't work offline)

### Technical Dependencies
- **Java 21** - NOT compatible with older versions (Paper 1.21.10 requirement)
- **Maven 3.8+** - for building
- **groq4j library** - community library, unofficial (backup: OkHttp)
- **Paper API repository** - must add to pom.xml (not on Maven Central)

### Knowledge Requirements
- Paper/Spigot plugin development (Bukkit API)
- Groq API documentation and usage
- **Async programming in Bukkit** - critical, must understand thread safety
- Maven build system and dependency management
- JSON parsing with Gson
- HTTP client usage (OkHttp)

---

## Progress Tracking

**Current Phase**: Phase 1 - Project Setup
**Overall Completion**: 0%

### Milestone Checklist
- [ ] Phase 1: Project Setup Complete
- [ ] Phase 2: Core Plugin & Config Complete
- [ ] Phase 3: Async Infrastructure Complete (CRITICAL)
- [ ] Phase 4: API Integration Complete
- [ ] Phase 5: Command Handler & Cooldown Complete
- [ ] Phase 6: Knowledge Base Complete
- [ ] Phase 7: Message Formatting & Chunking Complete
- [ ] Phase 8: Security Hardening Complete
- [ ] Phase 9: Error Handling & Logging Complete
- [ ] Phase 10: Testing & QA Complete
- [ ] Phase 11: Documentation & Build Complete

---

## Notes

### Technical Decisions Made
1. ✅ Using Maven (user doesn't care)
2. ✅ Using groq4j library as primary, OkHttp as priority fallback
3. ✅ Non-streaming responses (simpler implementation)
4. ✅ Command-based interface (/wai) not key binding
5. ✅ **Async API calls established in Phase 3** (critical, not optional)
6. ✅ Model: llama-3.3-70b-versatile (NOT openai/gpt-oss-20b)
7. ✅ Cooldown system: 10 seconds default (configurable)
8. ✅ Knowledge base: Optional, plugin works without it
9. ✅ Response chunking: 256 character limit with 200ms delays

### Critical Warnings
- ⚠ **Paper API NOT on Maven Central** - must add repository
- ⚠ **Model selection critical** - openai/gpt-oss-20b is content moderation, NOT assistant
- ⚠ **Async is required architecture** - not optional polish, establish in Phase 3
- ⚠ **Never call Bukkit API from async threads** - causes server crashes
- ⚠ **Knowledge base is optional** - plugin must work without it
- ⚠ **OkHttp is priority dependency** - not just fallback

### Open Questions
- None (all questions answered by user, refined with feasibility assessment)

### Resources
- [Paper API Docs](https://jd.papermc.io/paper/1.21/)
- [Paper Repository](https://repo.papermc.io/repository/maven-public/)
- [Groq API Docs](https://console.groq.com/docs/overview)
- [Groq Models](https://console.groq.com/docs/models)
- [groq4j GitHub](https://github.com/kornkutan/groq4j)
- [Bukkit Async Tasks](https://www.spigotmc.org/wiki/scheduler-programming/)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Adoptium JDK 21](https://adoptium.net/temurin/releases/?version=21)

---

**Last Updated**: 2025-11-25 (Refined with feasibility assessment)
**Maintained By**: @lipokatz-hub
**Status**: Ready for Implementation
