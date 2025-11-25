# WardenAI - Development TODO List

This document tracks all tasks for implementing the WardenAI Minecraft plugin.

## Legend
- [ ] Not Started
- [x] Completed
- [~] In Progress
- [!] Blocked

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

### 1.2 Build Configuration
- [ ] Create `pom.xml` with:
  - [ ] Paper API dependency (1.21.10)
  - [ ] Groq4j library dependency
  - [ ] OkHttp dependency (fallback)
  - [ ] Gson dependency
  - [ ] Maven Shade plugin for fat JAR
  - [ ] Java 21 compiler configuration

### 1.3 Plugin Metadata
- [ ] Create `plugin.yml` with:
  - [ ] Plugin name: WardenAI
  - [ ] Version: 1.0.0
  - [ ] Main class: com.wardenai.WardenAI
  - [ ] API version: 1.21
  - [ ] Commands: /wai, /wardenai
  - [ ] Permissions: wardenai.use
  - [ ] Description and author info

### 1.4 Configuration Files
- [ ] Create `config.yml` template with:
  - [ ] Groq API key placeholder
  - [ ] Model configuration
  - [ ] Temperature and max tokens
  - [ ] Custom messages (prefix, errors, etc.)
- [ ] Create `knowledge-base.txt` with placeholder text

---

## Phase 2: Core Plugin Implementation

### 2.1 Main Plugin Class (WardenAI.java)
- [ ] Create WardenAI class extending JavaPlugin
- [ ] Implement `onEnable()` method:
  - [ ] Log plugin startup
  - [ ] Load configuration
  - [ ] Initialize GroqService
  - [ ] Initialize KnowledgeBaseService
  - [ ] Register commands
  - [ ] Register event listeners (if needed)
- [ ] Implement `onDisable()` method:
  - [ ] Cleanup resources
  - [ ] Log plugin shutdown
- [ ] Add configuration loading method
- [ ] Add configuration validation

### 2.2 Configuration Management
- [ ] Add method to load config.yml
- [ ] Add method to get Groq API key
- [ ] Add method to get model settings
- [ ] Add method to get custom messages
- [ ] Add validation for required config fields
- [ ] Add default config generation if missing

---

## Phase 3: Command System

### 3.1 WaiCommand Handler (WaiCommand.java)
- [ ] Create WaiCommand class implementing CommandExecutor
- [ ] Implement `onCommand()` method:
  - [ ] Check if sender is a player
  - [ ] Check permissions (wardenai.use)
  - [ ] Validate command arguments (not empty)
  - [ ] Join arguments into single message
  - [ ] Show "thinking" message to player
  - [ ] Call GroqService asynchronously
  - [ ] Display response to player
  - [ ] Handle errors gracefully
- [ ] Add input validation:
  - [ ] Minimum message length (e.g., 1 character)
  - [ ] Maximum message length (e.g., 500 characters)
  - [ ] Sanitize input for API safety

### 3.2 Command Registration
- [ ] Register /wai command in WardenAI.java
- [ ] Register /wardenai alias
- [ ] Link command to WaiCommand executor
- [ ] Add tab completion (optional enhancement)

---

## Phase 4: Groq API Integration

### 4.1 GroqService Class (GroqService.java)
- [ ] Create GroqService class
- [ ] Add constructor with API key parameter
- [ ] Implement Groq client initialization:
  - [ ] Try using groq4j library first
  - [ ] Fallback to OkHttp if library fails
- [ ] Implement `sendMessage()` method:
  - [ ] Accept player message and knowledge base
  - [ ] Construct prompt with system context
  - [ ] Create API request
  - [ ] Send request to Groq API
  - [ ] Parse response
  - [ ] Return AI response text
- [ ] Add error handling for:
  - [ ] Invalid API key (401)
  - [ ] Rate limiting (429)
  - [ ] Token limit exceeded (400)
  - [ ] Network timeouts
  - [ ] JSON parsing errors
  - [ ] General API errors

### 4.2 Prompt Construction
- [ ] Create method to build system prompt
- [ ] Include knowledge base content
- [ ] Add role definition for wardenai character
- [ ] Format player message as user role
- [ ] Ensure proper JSON structure

### 4.3 Response Handling
- [ ] Parse JSON response from Groq
- [ ] Extract message content
- [ ] Handle streaming responses (if implemented)
- [ ] Handle partial responses
- [ ] Validate response format

### 4.4 Error Messages
- [ ] Detect token limit error from API
- [ ] Return specific error for "no tokens" scenario
- [ ] Return generic error for other failures
- [ ] Log detailed errors for debugging

---

## Phase 5: Knowledge Base System

### 5.1 KnowledgeBaseService Class (KnowledgeBaseService.java)
- [ ] Create KnowledgeBaseService class
- [ ] Implement `loadKnowledgeBase()` method:
  - [ ] Read knowledge-base.txt from resources
  - [ ] Handle file not found
  - [ ] Store content in memory
- [ ] Implement `getKnowledgeBase()` method:
  - [ ] Return cached knowledge base content
- [ ] Add reload functionality (future enhancement)

### 5.2 Knowledge Base File
- [ ] Create knowledge-base.txt in resources
- [ ] Add initial placeholder: "minecraft is a fun game"
- [ ] Document format and usage in comments
- [ ] Add examples of what to include (future)

### 5.3 Integration
- [ ] Load knowledge base on plugin startup
- [ ] Inject knowledge base into Groq prompts
- [ ] Test that KB content appears in responses

---

## Phase 6: Utilities & Formatting

### 6.1 MessageFormatter Class (MessageFormatter.java)
- [ ] Create MessageFormatter utility class
- [ ] Implement method to format plugin messages:
  - [ ] Add color codes
  - [ ] Add plugin prefix
  - [ ] Wrap long messages
- [ ] Implement method to format AI responses:
  - [ ] Add proper spacing
  - [ ] Handle multi-line responses
  - [ ] Add color coding for readability
- [ ] Add method to format error messages

### 6.2 Async Task Handling
- [ ] Create async task wrapper for API calls
- [ ] Use Bukkit's scheduler for async execution
- [ ] Ensure main thread for player messaging
- [ ] Add timeout handling

---

## Phase 7: Error Handling & Logging

### 7.1 Exception Handling
- [ ] Add try-catch blocks for all API calls
- [ ] Handle IOException for network errors
- [ ] Handle JsonSyntaxException for parsing
- [ ] Handle NullPointerException safely
- [ ] Add specific handling for Groq API errors

### 7.2 Logging System
- [ ] Use plugin logger for all logs
- [ ] Log plugin startup/shutdown
- [ ] Log configuration loading
- [ ] Log API call attempts (debug level)
- [ ] Log API errors (warning level)
- [ ] Log critical failures (severe level)
- [ ] Don't log API keys or sensitive data

### 7.3 User-Facing Error Messages
- [ ] Create friendly error messages for players
- [ ] Map API errors to user messages
- [ ] Add specific message for token exhaustion
- [ ] Add retry suggestions where appropriate

---

## Phase 8: Testing & Quality Assurance

### 8.1 Unit Tests (Optional but Recommended)
- [ ] Test configuration loading
- [ ] Test knowledge base loading
- [ ] Test prompt construction
- [ ] Test message formatting
- [ ] Mock API responses for testing

### 8.2 Integration Testing
- [ ] Test plugin on Paper 1.21.10 server
- [ ] Test /wai command with valid API key
- [ ] Test command permissions
- [ ] Test with missing config.yml
- [ ] Test with invalid API key
- [ ] Test with network disconnected
- [ ] Test with very long messages
- [ ] Test with special characters in input
- [ ] Test concurrent requests from multiple players

### 8.3 Performance Testing
- [ ] Test memory usage
- [ ] Test with 10+ concurrent players
- [ ] Check for memory leaks
- [ ] Verify async execution doesn't block server

### 8.4 Edge Cases
- [ ] Empty message
- [ ] Message with only spaces
- [ ] Message with Unicode/emoji
- [ ] Player disconnect during API call
- [ ] API timeout
- [ ] Malformed API response

---

## Phase 9: Documentation

### 9.1 Code Documentation
- [ ] Add JavaDoc comments to all public methods
- [ ] Add class-level documentation
- [ ] Document configuration options
- [ ] Add inline comments for complex logic

### 9.2 User Documentation
- [ ] Create README.md with:
  - [ ] Installation instructions
  - [ ] Configuration guide
  - [ ] Command usage examples
  - [ ] Troubleshooting section
  - [ ] FAQ
- [ ] Document how to get Groq API key
- [ ] Document knowledge base customization
- [ ] Add example config.yml

### 9.3 Developer Documentation
- [ ] Document build process
- [ ] Document project structure
- [ ] Add contribution guidelines (if open source)
- [ ] Document API integration details

---

## Phase 10: Build & Deployment

### 10.1 Maven Build
- [ ] Run `mvn clean package`
- [ ] Verify JAR is created in target/
- [ ] Test JAR on fresh server
- [ ] Verify all dependencies are shaded
- [ ] Check JAR file size (should be reasonable)

### 10.2 Release Preparation
- [ ] Create release notes
- [ ] Document version 1.0.0 features
- [ ] Create installation guide
- [ ] Test on clean Paper 1.21.10 server

### 10.3 Distribution
- [ ] Place JAR in plugins/ folder
- [ ] Start server and verify plugin loads
- [ ] Test all commands work
- [ ] Generate default config.yml
- [ ] Provide to users

---

## Future Enhancements (Post-MVP)

### Conversation Context
- [ ] Store conversation history per player
- [ ] Include previous messages in API calls
- [ ] Add command to clear history: /wai clear
- [ ] Set maximum history length

### Advanced Features
- [ ] Add GUI-based chat interface (inventory GUI)
- [ ] Implement response streaming for real-time updates
- [ ] Add admin commands: /wai reload, /wai stats
- [ ] Add cooldown system to prevent spam
- [ ] Add rate limiting per player
- [ ] Support multiple knowledge base files
- [ ] Add placeholder support in knowledge base
- [ ] Add per-player permissions for advanced features

### Multi-LLM Support
- [ ] Abstract LLM service interface
- [ ] Add OpenAI provider
- [ ] Add Anthropic Claude provider
- [ ] Add config option to choose provider

### Analytics & Monitoring
- [ ] Track API usage statistics
- [ ] Log popular questions
- [ ] Monitor token consumption
- [ ] Add metrics dashboard (optional)

### Localization
- [ ] Support for multiple languages
- [ ] Translatable messages
- [ ] Language selection per player

---

## Blockers & Dependencies

### External Dependencies
- Groq API access and valid API key
- Paper 1.21.10 server for testing
- Internet connection for API calls

### Technical Dependencies
- Java 21+ installed
- Maven 3.8+ for building
- groq4j library availability on Maven Central

### Knowledge Requirements
- Paper/Spigot plugin development
- Groq API documentation
- Async programming in Bukkit
- Maven build system

---

## Progress Tracking

**Current Phase**: Phase 1 - Project Setup
**Overall Completion**: 0%

### Milestone Checklist
- [ ] Phase 1: Project Setup Complete
- [ ] Phase 2: Core Plugin Complete
- [ ] Phase 3: Commands Complete
- [ ] Phase 4: API Integration Complete
- [ ] Phase 5: Knowledge Base Complete
- [ ] Phase 6: Utilities Complete
- [ ] Phase 7: Error Handling Complete
- [ ] Phase 8: Testing Complete
- [ ] Phase 9: Documentation Complete
- [ ] Phase 10: Build & Deploy Complete

---

## Notes

### Technical Decisions Made
1. Using Maven instead of Gradle (both acceptable)
2. Using groq4j library as primary API client
3. Non-streaming responses for simpler implementation
4. Command-based interface (/wai) instead of key binding
5. Async API calls to prevent server lag

### Open Questions
- None currently (all questions answered by user)

### Resources
- [Paper API Docs](https://jd.papermc.io/paper/1.21/)
- [Groq API Docs](https://console.groq.com/docs/overview)
- [groq4j GitHub](https://github.com/kornkutan/groq4j)
- [Bukkit Async Tasks](https://www.spigotmc.org/wiki/scheduler-programming/)

---

**Last Updated**: 2025-11-25
**Maintained By**: @lipokatz-hub
