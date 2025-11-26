# Privacy Notice for WardenAI

## Introduction

WardenAI is a Minecraft plugin that provides an AI assistant powered by Groq's large language models. This privacy notice explains what data is collected, how it's used, and your rights regarding your data.

## Data We Collect

### Automatically Collected

When you use the `/wai` command, the following data is sent to the Groq API:

1. **Your Minecraft Username**
   - Example: "Steve asks: How do I craft a diamond sword?"
   - Purpose: Provides context for the AI to personalize responses
   - Retention: Not stored by WardenAI (transient, request only)

2. **Your Sanitized Message**
   - What it is: Your question/message with color codes and control characters removed
   - Example: "How do I craft a diamond sword?"
   - Purpose: The actual question you want answered
   - Retention: Not stored by WardenAI (transient, request only)

3. **System Prompt** (if configured)
   - What it is: Server-specific instructions and knowledge base
   - Example: "You are WardenAI, a helpful Minecraft assistant..."
   - Purpose: Customizes AI behavior for your server
   - Source: Configured by server administrators
   - Retention: Loaded from config file, not tied to individual players

### NOT Collected

WardenAI does **NOT** collect, store, or transmit:

- ❌ IP addresses
- ❌ Player UUIDs (except temporarily for cooldowns, cleared on restart)
- ❌ Chat logs or message history
- ❌ Player locations or inventories
- ❌ Other players' information
- ❌ Server IP addresses or ports
- ❌ Passwords or authentication tokens

## How Data Is Used

### By WardenAI Plugin

- **Cooldown Management**: Stores your UUID + timestamp temporarily to enforce rate limits
- **Request Processing**: Temporarily holds your message during API request
- **Logging**: May log your username in debug mode (disabled by default)
- **No Persistent Storage**: Messages are never saved to disk

### By Groq API (Third-Party Service)

WardenAI sends your data to Groq's API for processing. Groq's use of data is governed by their own privacy policy:

- **Groq Privacy Policy**: https://groq.com/privacy-policy/
- **Purpose**: Generate AI responses to your questions
- **Retention**: See Groq's policy for data retention details
- **Location**: Data processed by Groq's infrastructure (see their policy for locations)

**Important**: We do not control Groq's data handling. Review their privacy policy independently.

## Data Storage

### WardenAI Local Storage

| Data Type | Storage Location | Retention | Purpose |
|-----------|------------------|-----------|---------|
| Cooldown timers | Memory (RAM) | Until server restart | Rate limiting |
| Configuration | config.yml | Indefinite | Plugin settings |
| Knowledge base | knowledge-base.txt | Indefinite | AI context |
| Logs | Console/logs | Per server settings | Debugging |

**No Database**: WardenAI does not use a database. All data is either in memory or in configuration files.

### Data Transmission

- **Protocol**: HTTPS (TLS encrypted)
- **Endpoint**: https://api.groq.com/openai/v1/chat/completions
- **Frequency**: Every time you use `/wai` command
- **Encryption**: End-to-end encryption via TLS

## Your Rights

### Access and Control

You have the following rights regarding your data:

1. **Right to Know**: Ask server admins what data is logged
2. **Right to Opt-Out**: Don't use `/wai` command if you don't want data sent to Groq
3. **Right to Deletion**: Contact server admins to remove debug logs (if enabled)
4. **Right to Rectification**: Usernames are pulled directly from Minecraft (change in-game)

### How to Exercise Your Rights

Contact your server administrator. WardenAI does not have a direct support channel for player data requests.

## Children's Privacy (COPPA/GDPR)

**Age Restrictions**:
- WardenAI does not knowingly collect data from children under 13 (COPPA)
- Server operators must comply with applicable age restrictions (COPPA, GDPR, etc.)
- Parental consent may be required depending on jurisdiction

**For Server Operators**:
If your server allows players under 13:
1. Obtain verifiable parental consent before allowing `/wai` usage
2. Consider disabling WardenAI for underage players
3. Add clear notices about data transmission to third parties

## Data Security

### Security Measures

WardenAI implements the following security measures:

- ✅ **Input Sanitization**: Removes malicious characters before transmission
- ✅ **Permission System**: Controls who can use the plugin
- ✅ **Rate Limiting**: Prevents spam and abuse
- ✅ **TLS Encryption**: All API calls encrypted in transit
- ✅ **No Persistent Storage**: Messages not saved to disk
- ✅ **API Key Protection**: Server credentials never logged or exposed

### Limitations

- **Third-Party Risk**: Data sent to Groq is subject to their security practices
- **Server Logs**: Debug mode may log messages (server admin control)
- **Network Interception**: Theoretical risk if TLS is compromised (extremely unlikely)

## Changes to This Policy

This privacy notice may be updated to reflect changes in:
- Plugin functionality
- Data collection practices
- Legal requirements
- Third-party service changes

**Current Version**: 1.0.0
**Last Updated**: 2025-11-26
**Effective Date**: 2025-11-26

## International Users

### GDPR (European Union)

If you're in the EU, you have additional rights under GDPR:

- **Right to Access**: Request copies of data held about you
- **Right to Erasure**: Request deletion of your data ("right to be forgotten")
- **Right to Rectification**: Correct inaccurate data
- **Right to Data Portability**: Receive data in machine-readable format
- **Right to Object**: Object to data processing
- **Right to Restrict Processing**: Limit how data is used

**Data Controller**: Your server operator is the data controller
**Data Processor**: Groq Inc. is the data processor

### Other Jurisdictions

- **CCPA (California)**: You may have rights to opt-out of data "sales" (not applicable - no sales)
- **PIPEDA (Canada)**: Rights similar to GDPR
- **LGPD (Brazil)**: Rights similar to GDPR

Consult local laws for specific requirements in your jurisdiction.

## Contact Information

### For WardenAI Plugin Questions

- **GitHub**: https://github.com/[repository-url]/issues
- **Documentation**: README.md and SECURITY.md in plugin files
- **Server Admin**: Contact your Minecraft server administrator

### For Groq API Questions

- **Groq Support**: https://groq.com/contact/
- **Groq Privacy**: privacy@groq.com
- **Groq Policy**: https://groq.com/privacy-policy/

## Transparency Commitment

We believe in transparency. This privacy notice is:
- ✅ Written in plain language
- ✅ Accurate and up-to-date
- ✅ Comprehensive in scope
- ✅ Freely available

If you have questions or concerns about privacy, please contact your server administrator or open a GitHub issue.

## Consent

**By using the `/wai` command, you acknowledge that**:
1. You have read and understood this privacy notice
2. You consent to your username and message being sent to Groq API
3. You understand data is transmitted to a third-party service
4. You accept the risks associated with cloud-based AI services

**If you do not consent**: Simply don't use the `/wai` command. All other Minecraft features remain available.

## Server Operator Responsibilities

Server operators using WardenAI must:

1. **Inform Players**: Display this privacy notice or a summary to players
2. **Obtain Consent**: Ensure players understand data is sent to Groq
3. **Comply with Laws**: Follow applicable privacy laws (GDPR, COPPA, etc.)
4. **Secure Configuration**: Protect config.yml with API keys
5. **Monitor Usage**: Review debug logs for sensitive data exposure
6. **Update Regularly**: Keep WardenAI updated for security patches

**Recommended Notice** (add to server MOTD or rules):
```
This server uses WardenAI AI assistant.
Messages sent to /wai are processed by Groq API.
See [server website]/privacy for full details.
Do not share personal information with WardenAI.
```

## Questions and Concerns

If you have questions about this privacy notice or WardenAI's data practices:

1. **Contact**: Your server administrator first
2. **GitHub**: Open an issue for plugin-specific questions
3. **Groq**: Contact Groq directly for API-related questions

We take privacy seriously and will respond to concerns promptly.

---

**Last Review Date**: 2025-11-26
**Next Scheduled Review**: 2026-11-26 (or sooner if changes occur)
**Policy Version**: 1.0.0
