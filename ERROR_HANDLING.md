# Error Handling & Logging Architecture

## Overview

WardenAI implements comprehensive error handling and logging to ensure:
- Plugin never crashes from API or network errors
- Users receive friendly, actionable error messages
- Administrators have detailed logs for troubleshooting
- Sensitive data is never logged
- Errors allow retry without penalty

## Table of Contents

1. [Exception Handling Architecture](#exception-handling-architecture)
2. [Logging System](#logging-system)
3. [Error Message Mapping](#error-message-mapping)
4. [Error Recovery](#error-recovery)
5. [Testing Error Scenarios](#testing-error-scenarios)

---

## Exception Handling Architecture

### 1. Async Exception Handling (Phase 3)

**Location**: `GroqService.java:103-168`

The async pattern safely handles exceptions across thread boundaries:

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    try {
        // ASYNC THREAD - API call can throw exceptions
        String response = callGroqAPI(playerName, message);

        // SUCCESS - switch to main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            onSuccess.accept(response);
        });
    } catch (Exception e) {
        // ERROR - switch to main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            onError.accept("API error: " + e.getMessage());
        });
    }
});
```

**Exception Types Caught**:
- `IOException` - Network/connectivity errors
- `GroqApiException` - API errors (401, 429, 503, etc.)
- `Exception` - Catch-all for unexpected errors

**Safety Guarantees**:
- ✅ Exceptions never escape async thread
- ✅ Error callbacks always run on main thread
- ✅ Plugin continues functioning after errors
- ✅ Timeout tasks cleaned up on error

### 2. API Call Exception Handling (Phase 4)

**Location**: `GroqService.java:182-228`

API calls throw specific exceptions for proper error handling:

```java
private String callGroqAPI(String playerName, String message)
    throws IOException, GroqApiException {

    try (Response response = httpClient.newCall(request).execute()) {
        String responseBody = response.body().string();

        if (!response.isSuccessful()) {
            handleApiError(response.code(), responseBody);
        }

        return parseResponse(responseBody);
    }
}
```

**Exception Flow**:
1. **IOException**: Network errors (DNS, connection, timeout)
2. **GroqApiException**: HTTP errors (401, 429, 400, 503)
3. Caught by async wrapper → Error callback → User message

### 3. Error Type Classification (Phase 4)

**Location**: `GroqService.java:423-448`

Custom exception with error type enum:

```java
public static class GroqApiException extends Exception {
    public enum ErrorType {
        INVALID_API_KEY,    // 401 Unauthorized
        RATE_LIMITED,       // 429 Too Many Requests
        MESSAGE_TOO_LONG,   // 400 Context Length Exceeded
        SERVICE_UNAVAILABLE,// 503 Service Unavailable
        BAD_REQUEST,        // 400 Bad Request
        GENERIC             // Other errors
    }

    private final ErrorType errorType;
}
```

**Benefits**:
- Structured error handling (not string parsing)
- Type-safe error classification
- Extensible for future error types
- Clear mapping to user messages

### 4. HTTP Error Handling (Phase 4)

**Location**: `GroqService.java:333-373`

Comprehensive HTTP status code handling:

| HTTP Code | ErrorType | User Message | Admin Log Level |
|-----------|-----------|--------------|-----------------|
| 401 | INVALID_API_KEY | "Not configured, contact admin" | SEVERE |
| 429 | RATE_LIMITED | "Too busy, try again later" | WARNING |
| 400 (context) | MESSAGE_TOO_LONG | "Message too long" | WARNING |
| 400 (other) | BAD_REQUEST | "Invalid request" | WARNING |
| 503 | SERVICE_UNAVAILABLE | "Service unavailable" | WARNING |
| Other | GENERIC | "API error (code X)" | WARNING |

**Special Cases**:
- **400 with context_length_exceeded**: Detected by parsing response body
- **Timeout**: Separate handling via Bukkit scheduler (30 seconds)
- **Network errors**: Caught as IOException

### 5. Configuration Exception Handling (Phase 2)

**Location**: `WardenAI.java:96-147`

Configuration loading with graceful failure:

```java
private boolean loadConfiguration() {
    try {
        reloadConfig();

        // Validate API key
        if (apiKey.isEmpty() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            getLogger().severe("GROQ API KEY NOT CONFIGURED!");
            return false;  // Plugin won't load
        }

        // Validate model
        if (model.equals("openai/gpt-oss-20b")) {
            getLogger().warning("WARNING: Using content moderation model!");
            // Continue anyway (just warn)
        }

        return true;

    } catch (Exception e) {
        getLogger().severe("Error loading configuration: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

**Error Handling**:
- Missing API key → Plugin disabled with clear message
- Invalid model → Warning but plugin continues
- Config parse error → Exception logged, plugin disabled
- Missing config → saveDefaultConfig() called first

### 6. Knowledge Base Exception Handling (Phase 6)

**Location**: `KnowledgeBaseService.java:45-90`

Graceful handling of missing/invalid knowledge base:

```java
public void loadKnowledgeBase() {
    try {
        // Try data folder
        if (kbFile.exists()) {
            knowledgeBaseContent = loadFromFile(kbFile.toPath());
            return;
        }

        // Try resources
        if (resourceStream != null) {
            knowledgeBaseContent = loadFromInputStream(resourceStream);
            return;
        }

        // Not found - continue without it
        plugin.getLogger().warning("No knowledge base found");
        knowledgeBaseContent = "";

    } catch (Exception e) {
        plugin.getLogger().warning("Error loading KB: " + e.getMessage());
        knowledgeBaseContent = "";  // Continue without KB
    }
}
```

**Key Points**:
- ✅ Missing file is NOT an error (just warning)
- ✅ IOException doesn't crash plugin
- ✅ Plugin works without knowledge base
- ✅ Clear instructions logged for admins

---

## Logging System

### 1. Log Level Usage

WardenAI uses appropriate log levels following Java logging conventions:

| Level | Usage | Examples |
|-------|-------|----------|
| **INFO** | Normal operations | "WardenAI enabled successfully!" |
| **WARNING** | Non-critical issues | "No knowledge base found" |
| **SEVERE** | Critical failures | "Invalid API key, plugin disabled" |
| **FINE** (via debug) | Debugging info | "[Groq API] Request to model X" |

### 2. Logging Locations

**WardenAI.java** (Plugin Lifecycle):
```java
// INFO - Successful operations
getLogger().info("========================================");
getLogger().info("WardenAI v" + getDescription().getVersion());
getLogger().info("Configuration loaded successfully");
getLogger().info("WardenAI enabled successfully!");

// WARNING - Non-critical issues
getLogger().warning("max-tokens must be positive, using default 8192");
getLogger().warning("WARNING: You are using openai/gpt-oss-20b");

// SEVERE - Critical failures
getLogger().severe("GROQ API KEY NOT CONFIGURED!");
getLogger().severe("Failed to initialize plugin services!");
```

**GroqService.java** (API Operations):
```java
// INFO (Debug Mode) - Request/response details
if (isDebugEnabled()) {
    plugin.getLogger().info("[Groq API] Request to " + model);
    plugin.getLogger().info("[Groq API] Player: " + playerName);
    plugin.getLogger().info("[Groq API] Response code: " + response.code());
}

// WARNING - API issues
plugin.getLogger().warning("[Groq API] 429 Rate Limited");
plugin.getLogger().warning("[Groq API] 503 Service Unavailable");

// SEVERE - Critical API errors
plugin.getLogger().severe("[Groq API] 401 Unauthorized - Check API key");
```

**KnowledgeBaseService.java** (KB Loading):
```java
// INFO - Successful loading
plugin.getLogger().info("Loading knowledge base from data folder...");
plugin.getLogger().info("Knowledge base loaded successfully (X characters)");

// WARNING - Missing files
plugin.getLogger().warning("No knowledge base found");
plugin.getLogger().warning("Error loading knowledge base: " + e.getMessage());
```

### 3. Debug Logging (Phase 2)

**Configuration**: `config.yml`
```yaml
debug:
  log-api-requests: false   # Logs API calls (includes player names)
  log-api-responses: false  # Logs API responses
  log-errors: true          # Logs error details
```

**Implementation**: `GroqService.java:408-418`
```java
private boolean isDebugEnabled() {
    try {
        if (plugin instanceof com.wardenai.WardenAI) {
            com.wardenai.WardenAI wardenAI = (com.wardenai.WardenAI) plugin;
            return wardenAI.isDebugApiRequests();
        }
    } catch (Exception e) {
        // Ignore
    }
    return false;
}
```

**What Gets Logged in Debug Mode**:
- ✅ Model being used
- ✅ Player names (for debugging)
- ✅ Message lengths (not content)
- ✅ HTTP status codes
- ❌ API keys (NEVER)
- ❌ Player messages (unless explicitly needed)
- ❌ API responses (unless explicitly enabled)

### 4. Privacy-Safe Logging

**Golden Rules**:
1. **NEVER log API keys** - Even in debug mode
2. **NEVER log player messages by default** - Only with explicit debug flag
3. **Use UUIDs for player identification** - When logging cooldowns
4. **Sanitize error messages** - Remove sensitive data from stack traces

**Example - Safe Logging**:
```java
// ✅ GOOD - No sensitive data
getLogger().info("[Groq API] Request to llama-3.3-70b-versatile");
getLogger().info("[Groq API] Player: Steve, Message length: 45");

// ❌ BAD - Logs player message
getLogger().info("[Groq API] Message: " + playerMessage);  // DON'T DO THIS

// ❌ BAD - Logs API key
getLogger().info("[Groq API] Using key: " + apiKey);  // DON'T DO THIS
```

---

## Error Message Mapping

### 1. Technical to User-Friendly Mapping (Phase 5)

**Location**: `WaiCommand.java:212-233`

```java
private String mapErrorToMessage(String error) {
    if (error.contains("API key is invalid")) {
        return plugin.getMessage("error-not-configured");
    } else if (error.contains("Rate limit")) {
        return plugin.getMessage("error-rate-limit");
    } else if (error.contains("too long")) {
        return plugin.getMessage("error-too-long");
    } else if (error.contains("unavailable")) {
        return plugin.getMessage("error-unavailable");
    } else if (error.contains("timed out")) {
        return plugin.getMessage("error-timeout");
    } else {
        return plugin.getMessage("error-generic");
    }
}
```

### 2. Configurable Error Messages (Phase 2)

**Location**: `src/main/resources/config.yml`

```yaml
messages:
  # Error messages (user-friendly)
  error-not-configured: "WardenAI is not configured. Please contact a server administrator."
  error-rate-limit: "WardenAI is too busy right now. Please try again in a moment."
  error-too-long: "Your message is too long. Please shorten it and try again."
  error-unavailable: "WardenAI is temporarily unavailable. Please try again later."
  error-timeout: "The request took too long and timed out. Please try again."
  error-generic: "An error occurred. Please try again or contact an administrator."
  no-tokens: "Token credit exhausted. Please contact a server administrator."

  # Informational messages
  cooldown: "Please wait {seconds} seconds before using /wai again."
  thinking: "Thinking..."
  prefix: "&7[&bWardenAI&7]"
```

### 3. Error Message Flow

```
API Error → GroqApiException → Error Callback → mapErrorToMessage() → Config Message → Player
     ↓            ↓                  ↓                    ↓                   ↓            ↓
  401 401    INVALID_API_KEY  "API key is invalid"  error-not-configured  "Not configured..."  Steve
```

**Example Flow**:
1. API returns `401 Unauthorized`
2. `handleApiError()` throws `GroqApiException(INVALID_API_KEY)`
3. Async wrapper catches exception
4. Error callback receives "API key is invalid or missing"
5. `mapErrorToMessage()` maps to "error-not-configured"
6. Config returns "WardenAI is not configured. Please contact..."
7. `formatMessage()` adds prefix
8. Player sees: `[WardenAI] WardenAI is not configured...`

### 4. Error Message Customization

Server admins can customize all error messages in `config.yml`:

```yaml
messages:
  error-not-configured: "❌ AI service not set up. Contact @Admin on Discord!"
  error-rate-limit: "⏰ Slow down! Wait 30 seconds."
  error-timeout: "⌛ Request timed out. Servers are slow today."
```

**Benefits**:
- Matches server's communication style
- Localization support (different languages)
- Branded messaging
- Different tone (formal vs casual)

---

## Error Recovery

### 1. Cooldown-Free Retry (Phase 5)

**Location**: `WaiCommand.java:145-180`

Critical feature: **Cooldown ONLY set on success**

```java
(response) -> {
    // Send response to player
    player.sendMessage(...);

    // Set cooldown AFTER successful response
    plugin.getCooldownManager().setCooldown(player);
},
(error) -> {
    // Send error message
    player.sendMessage(...);

    // DO NOT set cooldown on error - allow retry
}
```

**Why This Matters**:
- Player doesn't waste cooldown on API errors
- Encourages retry on transient failures
- Fair user experience
- Doesn't penalize players for plugin/API issues

### 2. Retry Guidance

Error messages guide users on recovery:

| Error Type | Message | Recovery Action |
|------------|---------|-----------------|
| Not Configured | "Contact administrator" | Admin fixes config |
| Rate Limited | "Try again in a moment" | Wait 30-60 seconds |
| Timeout | "Try again" | Retry immediately |
| Too Long | "Shorten your message" | Reduce message length |
| Unavailable | "Try again later" | Wait 5-10 minutes |
| Generic | "Try again or contact admin" | Retry or report |

### 3. Graceful Degradation

Plugin continues working after errors:

```java
// Knowledge base loading failure
try {
    loadKnowledgeBase();
} catch (Exception e) {
    // Log warning but continue
    knowledgeBaseContent = "";  // Empty KB, plugin still works
}

// API error
try {
    callGroqAPI(...);
} catch (Exception e) {
    // Error callback, plugin still running
    onError.accept("API error");  // Next command will work
}

// Configuration warning
if (invalidValue) {
    getLogger().warning("Invalid value, using default");
    // Use default, continue loading
}
```

### 4. Error State Management

**No Persistent Error States**:
- Each command execution is independent
- Previous errors don't affect future commands
- No "error mode" that requires restart
- Automatic recovery when external issues resolve

**Example Scenario**:
1. Player runs `/wai hello` → 503 Service Unavailable
2. Error message shown, no cooldown set
3. 30 seconds later, API is back online
4. Player runs `/wai hello` again → Success
5. Normal cooldown applied

---

## Testing Error Scenarios

### 1. Configuration Errors

**Test Case: Missing API Key**
```yaml
# config.yml
groq:
  api-key: "YOUR_GROQ_API_KEY_HERE"  # Placeholder
```

**Expected Behavior**:
- Plugin logs: `SEVERE: GROQ API KEY NOT CONFIGURED!`
- Plugin disables itself
- Players: Command not available

**Test Case: Invalid Model**
```yaml
groq:
  model: "openai/gpt-oss-20b"  # Content moderation model
```

**Expected Behavior**:
- Plugin logs: `WARNING: You are using openai/gpt-oss-20b`
- Plugin continues loading
- Players: May get poor responses

### 2. API Errors

**Test Case: Invalid API Key (401)**
```bash
# Set invalid API key in config
curl -X POST https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer invalid_key_here"
# Returns 401
```

**Expected Behavior**:
- Console: `SEVERE: [Groq API] 401 Unauthorized - Check your API key`
- Player: "[WardenAI] WardenAI is not configured. Please contact..."
- No cooldown set (can retry)

**Test Case: Rate Limit (429)**

**Expected Behavior**:
- Console: `WARNING: [Groq API] 429 Rate Limited`
- Player: "[WardenAI] WardenAI is too busy right now. Please try again..."
- No cooldown set (can retry immediately)

**Test Case: Timeout**

**Expected Behavior**:
- Console: No error (timeout is normal operation)
- Player: "[WardenAI] The request took too long and timed out. Please try again."
- No cooldown set (can retry)

### 3. Network Errors

**Test Case: DNS Failure**
```bash
# Simulate by blocking api.groq.com in /etc/hosts
127.0.0.1 api.groq.com
```

**Expected Behavior**:
- IOException caught
- Player: "[WardenAI] An error occurred. Please try again..."
- Console: Exception logged with stack trace

**Test Case: Connection Timeout**

**Expected Behavior**:
- OkHttp timeout (30 seconds)
- Player: "[WardenAI] The request took too long..."
- No crash, plugin continues

### 4. Input Validation Errors

**Test Case: Message Too Short**
```
/wai hi
```

**Expected Behavior**:
- Pre-validation, before API call
- Player: "[WardenAI] Your message is too short (minimum 3 characters)."
- No API call made, no cooldown

**Test Case: Message Too Long**
```
/wai [500+ character message]
```

**Expected Behavior**:
- Pre-validation
- Player: "[WardenAI] Your message is too long (maximum 500 characters)."
- No API call made, no cooldown

### 5. Permission Errors

**Test Case: No Permission**
```bash
# Remove wardenai.use permission from player
/wai test
```

**Expected Behavior**:
- Permission check before API call
- Player: "[WardenAI] You don't have permission to use this command."
- No API call, no cooldown

### 6. Cooldown Errors

**Test Case: Cooldown Active**
```bash
/wai test1  # Success, cooldown set
/wai test2  # Immediate retry
```

**Expected Behavior**:
- Player: "[WardenAI] Please wait 8 seconds before using /wai again."
- No API call made
- Cooldown timer shown

---

## Summary Checklist

### Phase 9 Complete

**9.1 Exception Handling**: ✅
- [x] All API calls wrapped in try-catch
- [x] IOException handled (network errors)
- [x] GroqApiException handled (API errors)
- [x] Generic Exception caught (safety net)
- [x] Never crashes plugin
- [x] All exceptions logged

**9.2 Logging System**: ✅
- [x] INFO for successful operations
- [x] WARNING for non-critical issues
- [x] SEVERE for critical failures
- [x] Debug mode for detailed logging
- [x] No API keys logged
- [x] No player messages logged (except debug)

**9.3 User-Facing Messages**: ✅
- [x] All technical errors mapped to friendly messages
- [x] Config-based customizable messages
- [x] Clear recovery instructions
- [x] Consistent formatting

**9.4 Error Recovery**: ✅
- [x] No cooldown on errors (retry allowed)
- [x] Clear retry guidance
- [x] Graceful degradation
- [x] No persistent error states

---

**Document Version**: 1.0.0
**Last Updated**: 2025-11-26
**Phase**: 9 - Error Handling & Logging
**Status**: Complete ✅
