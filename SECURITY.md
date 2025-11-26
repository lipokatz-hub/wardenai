# Security Policy

## Overview

WardenAI takes security and privacy seriously. This document outlines the security measures implemented in the plugin and best practices for secure deployment.

## Security Features

### 1. Input Sanitization

**Location**: `WaiCommand.java:182-201`

All player input is sanitized before being sent to the Groq API:

- **Color Code Stripping**: Removes Minecraft color codes (§ and &) to prevent injection
- **Control Character Removal**: Strips control characters except newlines
- **Whitespace Normalization**: Replaces multiple spaces with single space
- **Trimming**: Removes leading/trailing whitespace

**Implementation**:
```java
private String sanitizeInput(String input) {
    String sanitized = ChatColor.stripColor(input);
    sanitized = sanitized.replaceAll("&[0-9a-fk-or]", "");
    sanitized = sanitized.replaceAll("\\p{Cntrl}", " ");
    sanitized = sanitized.replaceAll("\\s+", " ");
    return sanitized.trim();
}
```

**Protection Against**:
- Color code injection
- Control character exploits
- Whitespace manipulation
- Format string attacks

### 2. API Key Security

**Location**: `WardenAI.java:96-147`

Comprehensive API key validation and protection:

- **Startup Validation**: Plugin refuses to start with placeholder API key
- **Configuration Check**: Validates API key is present and not default
- **Logging Protection**: API keys are NEVER logged to console or files
- **Error Sanitization**: Stack traces sanitized to remove API keys

**Best Practices for Server Admins**:

1. **File Permissions**: Set restrictive permissions on config.yml
   ```bash
   chmod 600 plugins/WardenAI/config.yml
   chown minecraft:minecraft plugins/WardenAI/config.yml
   ```

2. **Never Commit**: Keep config.yml out of version control
   - Template: `config.yml` (with placeholder)
   - Production: `config-local.yml` (with real key, gitignored)

3. **Rotate Keys**: Regularly rotate API keys from https://console.groq.com/

### 3. Permission System

**Location**: `plugin.yml:20-25` and `WaiCommand.java:57-62`

Three-tier permission system:

| Permission | Default | Purpose |
|------------|---------|---------|
| `wardenai.use` | `true` | Base command access |
| `wardenai.bypass.cooldown` | `op` | Skip cooldown checks |
| `wardenai.admin` | `op` | Future admin commands |

**Permission Checks**:
- Performed before any API calls
- Clear error messages for denied access
- Fail-safe: deny by default

### 4. Rate Limiting

**Location**: `CooldownManager.java:1-155`

Per-player cooldown system prevents abuse:

- **Configurable Duration**: Default 10 seconds, adjustable in config
- **Bypass Permission**: Ops can bypass cooldowns
- **Thread-Safe**: Uses ConcurrentHashMap for concurrent access
- **Memory Efficient**: Automatic cleanup of expired cooldowns
- **Error Protection**: Cooldown NOT set on API errors (allows retry)

**Protection Against**:
- API spam/abuse
- Rate limit exhaustion
- Denial of service attacks
- Resource exhaustion

### 5. Thread Safety

**Location**: `GroqService.java:103-168`

Critical async/sync pattern prevents server crashes:

- **Async API Calls**: Network I/O on separate thread
- **Sync Callbacks**: Bukkit API access only on main thread
- **Timeout Protection**: 30-second timeout on all requests
- **Concurrent Collections**: ConcurrentHashMap for active requests
- **Resource Cleanup**: Timeout tasks cancelled on completion

**Protection Against**:
- Server freezing
- Thread deadlocks
- Memory leaks
- Resource exhaustion

### 6. Error Handling

**Location**: `GroqService.java:333-373` and `WaiCommand.java:212-233`

Comprehensive error handling prevents information disclosure:

- **User-Friendly Messages**: Technical errors mapped to config messages
- **No Stack Traces**: Players never see stack traces
- **Sanitized Logging**: Errors logged without sensitive data
- **Graceful Degradation**: Plugin continues working after errors

**HTTP Error Mapping**:
```
401 Unauthorized     → "Not configured, contact admin"
429 Rate Limited     → "Too busy, try again later"
400 Bad Request      → "Message too long"
503 Service Error    → "Service unavailable"
Timeout              → "Request timed out"
```

## Privacy Protection

### Data Collection

**What WardenAI Sends to Groq API**:
- Player's Minecraft username
- Sanitized player message
- System prompt (personality + knowledge base)

**What WardenAI NEVER Sends**:
- Player IP addresses
- Player UUIDs
- Server IP or port
- Other players' information
- Server configuration details

### Logging Practices

**Standard Mode** (debug: false):
- ✅ Plugin lifecycle events (startup, shutdown)
- ✅ Configuration validation results
- ✅ Error summaries (no stack traces)
- ❌ Player messages
- ❌ API requests/responses
- ❌ API keys

**Debug Mode** (debug: true):
- ⚠️ Logs API requests and responses
- ⚠️ Includes player names in API logs
- ⚠️ Shows request/response details
- ❌ Still NEVER logs API keys

**Configuration**:
```yaml
debug:
  log-api-requests: false   # Logs API calls (includes player names)
  log-api-responses: false  # Logs API responses
  log-errors: true          # Logs error details
```

### Data Retention

- **In-Memory Only**: No persistent storage of player messages
- **Cooldown Data**: UUID + timestamp only, cleared on server restart
- **Knowledge Base**: Optional file, admin-controlled content
- **No Database**: Plugin does not use or require a database

## Security Best Practices for Admins

### 1. Configuration Security

```bash
# Set restrictive file permissions
chmod 600 plugins/WardenAI/config.yml
chown minecraft:minecraft plugins/WardenAI/config.yml

# Verify permissions
ls -la plugins/WardenAI/config.yml
# Should show: -rw------- (600)
```

### 2. API Key Management

- **Generate**: Get API key from https://console.groq.com/
- **Store Securely**: Use config.yml with chmod 600
- **Rotate Regularly**: Change API keys every 3-6 months
- **Monitor Usage**: Check Groq console for unusual activity
- **Revoke if Compromised**: Immediately revoke and replace exposed keys

### 3. Permission Configuration

**Recommended Setup**:
```yaml
permissions:
  wardenai.use:
    default: true              # All players can use /wai
  wardenai.bypass.cooldown:
    default: op                # Only ops bypass cooldown
  wardenai.admin:
    default: op                # Only ops for admin commands
```

**Restrictive Setup** (private servers):
```yaml
permissions:
  wardenai.use:
    default: false             # Explicitly grant to groups
  wardenai.bypass.cooldown:
    default: false
  wardenai.admin:
    default: false
```

### 4. Network Security

- **Firewall**: Ensure outbound HTTPS (443) to api.groq.com is allowed
- **TLS**: Plugin uses HTTPS only, no plaintext HTTP
- **DNS**: Verify DNS resolution for api.groq.com works correctly
- **Proxy**: If using proxy, ensure it supports HTTPS/TLS

### 5. Monitoring

**What to Monitor**:
- Console logs for ERROR/SEVERE messages
- Groq console for API usage and limits
- Player reports of unexpected behavior
- Server performance (TPS, memory)

**Warning Signs**:
- `401 Unauthorized` - API key compromised or invalid
- `429 Rate Limited` - Excessive usage or abuse
- Multiple timeout errors - Network issues
- High memory usage - Potential memory leak (report bug)

## Vulnerability Reporting

If you discover a security vulnerability in WardenAI, please report it responsibly:

1. **Do NOT** open a public GitHub issue
2. **Do NOT** disclose the vulnerability publicly
3. **Contact**: Report to the repository maintainer privately
4. **Provide**: Detailed description, steps to reproduce, potential impact
5. **Timeline**: We aim to respond within 48 hours and patch within 7 days

## Security Checklist

Use this checklist when deploying WardenAI:

- [ ] API key stored in config.yml with chmod 600
- [ ] config.yml owner is the Minecraft server user
- [ ] API key is NOT the placeholder value
- [ ] API key is NOT committed to version control
- [ ] Debug logging is disabled in production
- [ ] Permissions configured according to server policy
- [ ] Cooldown duration set appropriately (10+ seconds)
- [ ] Knowledge base reviewed for sensitive information
- [ ] Server logs monitored for errors
- [ ] Groq API usage monitored for abuse

## Third-Party Services

WardenAI sends data to the following third-party service:

**Groq API** (https://api.groq.com)
- **Purpose**: LLM inference for AI responses
- **Data Sent**: Player name, sanitized message, system prompt
- **Data Retention**: See Groq's privacy policy
- **TLS/HTTPS**: Yes, enforced
- **Privacy Policy**: https://groq.com/privacy-policy/

**Inform Your Players**:
Add a notice to your server rules or MOTD:
```
This server uses WardenAI, an AI assistant powered by Groq.
Messages sent to /wai are sent to an external API for processing.
Do not share personal or sensitive information with WardenAI.
```

## Compliance Notes

- **GDPR**: Player names are considered personal data
- **COPPA**: Do not use on servers with players under 13 without parental consent
- **Terms of Service**: Ensure compliance with Groq's Terms of Service
- **Data Processing Agreement**: Review if operating in the EU

## Updates and Patches

- **Check Regularly**: Monitor GitHub for security updates
- **Update Promptly**: Apply security patches within 48 hours
- **Backup First**: Always backup before updating
- **Test Updates**: Test on staging server before production

## Audit Log

This security policy covers WardenAI version 1.0.0-SNAPSHOT.

Last Updated: 2025-11-26
Last Security Audit: 2025-11-26
