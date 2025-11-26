# WardenAI - Minecraft AI Assistant Plugin

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://papermc.io/)
[![Paper API](https://img.shields.io/badge/Paper-1.21.1-blue.svg)](https://papermc.io/)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A powerful AI assistant for Minecraft servers, powered by Groq's lightning-fast LLM API.

## 🎯 Features

- 🤖 **AI-Powered Assistant** - Chat with an AI in-game using the `/wai` command
- ⚡ **Lightning Fast** - Powered by Groq's optimized LLM infrastructure
- 📚 **Knowledge Base** - Inject custom server knowledge for context-aware responses
- 🎨 **Customizable** - Configurable personality, messages, and behavior
- 🔒 **Secure** - Input sanitization, API key protection, and permission system
- ⏱️ **Rate Limiting** - Per-player cooldowns to prevent abuse
- 📝 **Response Chunking** - Long responses split intelligently at word boundaries
- 🧵 **Thread-Safe** - Async API calls prevent server lag
- 🛡️ **Privacy-Focused** - Minimal data collection, no persistent storage
- 🌍 **Localizable** - All messages configurable in config.yml

## 📋 Table of Contents

- [Requirements](#-requirements)
- [Installation](#-installation)
- [Configuration](#️-configuration)
- [Usage](#-usage)
- [Commands & Permissions](#-commands--permissions)
- [Knowledge Base](#-knowledge-base)
- [Customization](#-customization)
- [Security & Privacy](#-security--privacy)
- [Troubleshooting](#-troubleshooting)
- [Development](#-development)
- [Contributing](#-contributing)
- [License](#-license)

## 🔧 Requirements

- **Server**: Paper 1.21.1+ (or Paper-compatible fork)
- **Java**: Java 21 or higher
- **API Key**: Free Groq API key from [console.groq.com](https://console.groq.com/)

## 📦 Installation

### Step 1: Get a Groq API Key

1. Visit [console.groq.com](https://console.groq.com/)
2. Create a free account
3. Generate an API key
4. Save it securely (you'll need it in Step 3)

### Step 2: Install the Plugin

1. Download `WardenAI-1.0.0.jar` from [Releases](../../releases)
2. Place it in your server's `plugins/` folder
3. Start your server (plugin will generate default config)
4. Stop your server

### Step 3: Configure API Key

1. Open `plugins/WardenAI/config.yml`
2. Replace `YOUR_GROQ_API_KEY_HERE` with your actual API key:
   ```yaml
   groq:
     api-key: "gsk_your_actual_api_key_here"
   ```
3. **Secure the file** (Linux/Mac):
   ```bash
   chmod 600 plugins/WardenAI/config.yml
   ```

### Step 4: Start Server

1. Start your server
2. Plugin should load successfully
3. Test with `/wai hello` in-game

## ⚙️ Configuration

### Complete config.yml

```yaml
# ============================================
# WardenAI Configuration
# ============================================

# Groq API Configuration
groq:
  # Get your free API key from: https://console.groq.com/
  api-key: "YOUR_GROQ_API_KEY_HERE"

  # Model to use (recommended: llama-3.3-70b-versatile)
  # Available models: llama-3.3-70b-versatile, llama-3.1-8b-instant, mixtral-8x7b-32768
  model: "llama-3.3-70b-versatile"

  # Maximum tokens in response (higher = longer responses)
  max-tokens: 8192

  # Temperature (0.0-2.0): Higher = more creative, Lower = more focused
  temperature: 1.0

  # Request timeout in seconds
  timeout-seconds: 30

# Usage Limits
limits:
  # Cooldown between /wai commands (seconds)
  cooldown-seconds: 10

  # Maximum message length (characters)
  max-message-length: 500

  # Minimum message length (characters)
  min-message-length: 3

  # Maximum response length before truncation (characters)
  max-response-length: 2000

# AI Personality
personality:
  # Enable custom personality (recommended: true)
  enabled: true

  # System prompt (defines AI behavior)
  system-prompt: |
    You are WardenAI, a helpful Minecraft assistant on a Paper server.
    Provide concise, accurate, and friendly responses about Minecraft gameplay, crafting, building, and survival.
    Keep responses short (2-3 sentences max) unless more detail is requested.
    Be encouraging and supportive to players.
    If you don't know something, admit it honestly.

# User-Facing Messages (customizable)
messages:
  # Success messages
  thinking: "Thinking..."
  prefix: "&7[&bWardenAI&7]"

  # Error messages
  error-not-configured: "WardenAI is not configured. Please contact a server administrator."
  error-rate-limit: "WardenAI is too busy right now. Please try again in a moment."
  error-too-long: "Your message is too long. Please shorten it and try again."
  error-unavailable: "WardenAI is temporarily unavailable. Please try again later."
  error-timeout: "The request took too long and timed out. Please try again."
  error-generic: "An error occurred. Please try again or contact an administrator."
  no-tokens: "Token credit exhausted. Please contact a server administrator."

  # Cooldown message ({seconds} will be replaced with actual time)
  cooldown: "Please wait {seconds} seconds before using /wai again."

# Debug Settings (WARNING: Logs player names when enabled)
debug:
  # Log API requests (includes player names, message lengths)
  log-api-requests: false

  # Log API responses (includes response text)
  log-api-responses: false

  # Log errors with stack traces
  log-errors: true
```

### Configuration Tips

**Model Selection**:
- `llama-3.3-70b-versatile` - Best quality, balanced speed (recommended)
- `llama-3.1-8b-instant` - Fastest responses, good quality
- `mixtral-8x7b-32768` - Long context, slower

**Temperature**:
- `0.5` - Focused, consistent, predictable
- `1.0` - Balanced creativity (recommended)
- `1.5` - More creative, varied responses

**Cooldown**:
- `5` seconds - Low-traffic servers
- `10` seconds - Medium-traffic (recommended)
- `30` seconds - High-traffic or API quota concerns

## 💬 Usage

### Basic Commands

```bash
# Ask a question
/wai How do I craft a diamond sword?

# Get building tips
/wai What blocks work well for a medieval castle?

# Learn game mechanics
/wai How does enchanting work?

# Get survival advice
/wai Best Y level for diamonds in 1.21?
```

### Response Format

**Short Response** (< 256 characters):
```
[WardenAI]: To craft a diamond sword, you need 2 diamonds and 1 stick. Arrange them vertically in a crafting table.
```

**Long Response** (> 256 characters, chunked):
```
[WardenAI] [1/3]: Medieval castles work best with stone bricks, cobblestone, and dark oak wood...
[WardenAI] [2/3]: Add details with stone brick stairs and slabs for depth...
[WardenAI] [3/3]: Don't forget arrow slits and battlements!
```

## 🎮 Commands & Permissions

### Commands

| Command | Aliases | Description | Usage |
|---------|---------|-------------|-------|
| `/wai <message>` | `/wardenai` | Ask WardenAI a question | `/wai How do I tame a wolf?` |

### Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wardenai.use` | `true` | Use the `/wai` command |
| `wardenai.bypass.cooldown` | `op` | Bypass cooldown timer |
| `wardenai.admin` | `op` | Future admin commands |

### Permission Configuration

**Open Access** (default):
```yaml
permissions:
  wardenai.use:
    default: true
```

**Restricted Access**:
```yaml
permissions:
  wardenai.use:
    default: false  # Grant manually to groups
  wardenai.bypass.cooldown:
    default: op
```

## 📚 Knowledge Base

The knowledge base lets you inject custom information into the AI's context.

### Setup

1. Open `plugins/WardenAI/knowledge-base.txt`
2. Add your custom content:

```
# Server-Specific Information

## Custom Rules
- No griefing in spawn town
- Use /sethome to save your location
- Join our Discord: discord.gg/yourserver

## Custom Features
- Economy: Use /balance, /pay, /shop
- Claims: Use /claim to protect your builds
- Warps: Use /warp [name] to teleport

## Custom Recipes
- Netherite tools require visiting the custom Nether dungeon
- Enchanted golden apples can be crafted (8 gold blocks + 1 apple)

## Server Lore
The server is set in a post-apocalyptic world where...
```

3. Save and reload: `/reload` or restart server

### Knowledge Base Tips

- Keep it concise (AI has token limits)
- Focus on server-specific information
- Use clear formatting (headings, lists)
- Update regularly with new features
- Test responses after changes

## 🎨 Customization

### Custom Personality

Edit the `personality.system-prompt` in config.yml:

**Pirate Theme**:
```yaml
system-prompt: |
  Ye be WardenAI, a swashbucklin' Minecraft assistant!
  Answer questions about Minecraft in pirate speak.
  Keep responses short and full of seafarin' wisdom, arr!
```

**Professional Theme**:
```yaml
system-prompt: |
  You are WardenAI, a professional Minecraft consultant.
  Provide detailed, technical explanations with precision.
  Use formal language and cite game mechanics accurately.
```

**Friendly Theme**:
```yaml
system-prompt: |
  Hey there! I'm WardenAI, your friendly Minecraft buddy! 😊
  I love helping players learn and have fun in Minecraft!
  Let's explore this awesome game together!
```

### Custom Messages

All player-facing messages are in `config.yml`:

```yaml
messages:
  prefix: "&6[&l✨ AI Assistant&6]"
  thinking: "✨ Consulting the ancient texts..."
  cooldown: "⏰ Slow down! Wait {seconds} more seconds."
  error-generic: "💥 Oops! Something went wrong. Try again?"
```

**Color Codes**:
- `&0-&9, &a-&f` - Colors
- `&l` - Bold
- `&o` - Italic
- `&m` - Strikethrough
- `&r` - Reset

## 🔒 Security & Privacy

### Data Collection

**What WardenAI Sends to Groq**:
- Your Minecraft username
- Your sanitized message (color codes removed)
- Server's system prompt and knowledge base

**What WardenAI Does NOT Send**:
- IP addresses
- Player UUIDs (except temp cooldowns)
- Chat history
- Server configuration

### Security Features

✅ **Input Sanitization** - Removes color codes, control characters
✅ **API Key Protection** - Never logged, validated at startup
✅ **Permission System** - Three-tier access control
✅ **Rate Limiting** - Per-player cooldowns
✅ **Thread Safety** - Async API calls, no server lag
✅ **Error Handling** - Never crashes, graceful degradation

### Best Practices

1. **Secure config.yml**:
   ```bash
   chmod 600 plugins/WardenAI/config.yml
   ```

2. **Rotate API keys** every 3-6 months

3. **Monitor logs** for unusual activity

4. **Disable debug mode** in production:
   ```yaml
   debug:
     log-api-requests: false
     log-api-responses: false
   ```

5. **Inform players** about data usage (see [PRIVACY.md](PRIVACY.md))

## 🐛 Troubleshooting

### Plugin Won't Load

**Error**: `GROQ API KEY NOT CONFIGURED!`
- **Fix**: Set real API key in config.yml (not placeholder)

**Error**: `Failed to load configuration`
- **Fix**: Check config.yml syntax with [YAML validator](https://www.yamllint.com/)

**Error**: `Plugin disabled`
- **Fix**: Check console for specific error, see [ERROR_HANDLING.md](ERROR_HANDLING.md)

### Command Not Working

**Error**: `/wai command not found`
- **Fix**: Check plugin loaded with `/plugins`
- **Fix**: Restart server after config changes

**Error**: `You don't have permission`
- **Fix**: Grant `wardenai.use` permission
- **Fix**: Check permission plugin configuration

### API Errors

**Error**: "WardenAI is not configured"
- **Cause**: API key invalid or expired
- **Fix**: Get new key from [console.groq.com](https://console.groq.com/)

**Error**: "WardenAI is too busy"
- **Cause**: Groq API rate limit reached
- **Fix**: Wait 60 seconds, reduce server usage

**Error**: "Request timed out"
- **Cause**: Network issues or slow API
- **Fix**: Check internet connection, try again

### Performance Issues

**Issue**: Server lag when using `/wai`
- **Cause**: Async pattern should prevent this
- **Fix**: Check server TPS with `/tps`, report bug if lag persists

**Issue**: Long response delays
- **Cause**: Groq API busy or network latency
- **Fix**: Try different model (llama-3.1-8b-instant is faster)

### Debug Mode

Enable debug logging for troubleshooting:

```yaml
debug:
  log-api-requests: true
  log-api-responses: true
  log-errors: true
```

Check `logs/latest.log` for detailed information.

**Remember**: Disable debug mode in production (logs player names).

## 🛠️ Development

### Building from Source

**Requirements**:
- Java 21 JDK
- Maven 3.6+
- Git

**Steps**:
```bash
# Clone repository
git clone https://github.com/yourusername/wardenai.git
cd wardenai

# Build with Maven
mvn clean package

# JAR location
ls target/WardenAI-1.0.0.jar
```

### Project Structure

```
wardenai/
├── src/main/
│   ├── java/com/wardenai/
│   │   ├── WardenAI.java           # Main plugin class
│   │   ├── commands/
│   │   │   └── WaiCommand.java     # /wai command handler
│   │   ├── services/
│   │   │   ├── GroqService.java    # Groq API integration
│   │   │   ├── CooldownManager.java # Rate limiting
│   │   │   └── KnowledgeBaseService.java # KB loading
│   │   └── utils/
│   │       ├── MessageFormatter.java # Message formatting
│   │       └── ResponseChunker.java  # Response splitting
│   └── resources/
│       ├── plugin.yml               # Plugin metadata
│       ├── config.yml               # Default config
│       └── knowledge-base.txt       # Default KB
├── pom.xml                          # Maven build config
├── README.md                        # This file
├── SECURITY.md                      # Security policy
├── PRIVACY.md                       # Privacy notice
├── ERROR_HANDLING.md                # Error handling docs
└── TODO.md                          # Implementation tasks
```

### Architecture

**Async Pattern** (prevents server lag):
```
Player Command → Main Thread → Async Thread (API call) → Main Thread (callback) → Player Response
```

**Error Flow**:
```
API Error → Exception → Error Type → Config Message → Formatted Response → Player
```

**Thread Safety**:
- API calls: Async thread (prevents blocking)
- Bukkit API: Main thread only (prevents crashes)
- Cooldowns: ConcurrentHashMap (thread-safe)

## 🤝 Contributing

We welcome contributions! Here's how:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** changes: `git commit -m "Add amazing feature"`
4. **Push** to branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

### Development Guidelines

- Follow existing code style (Java conventions)
- Add comments for complex logic
- Test thoroughly before submitting
- Update documentation if adding features
- No breaking changes without discussion

### Reporting Bugs

Open an issue with:
- Minecraft version
- Paper version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Console logs (with API key redacted)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Groq** - Lightning-fast LLM infrastructure ([groq.com](https://groq.com/))
- **PaperMC** - High-performance Minecraft server ([papermc.io](https://papermc.io/))
- **OkHttp** - HTTP client library ([square.github.io/okhttp](https://square.github.io/okhttp/))
- **Gson** - JSON library ([github.com/google/gson](https://github.com/google/gson))

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Documentation**: [Project Wiki](../../wiki)
- **Security**: See [SECURITY.md](SECURITY.md)
- **Privacy**: See [PRIVACY.md](PRIVACY.md)

## 🚀 Roadmap

Future features under consideration:

- [ ] Multi-language support
- [ ] Conversation history (optional, privacy-aware)
- [ ] Admin commands (/wai reload, /wai stats)
- [ ] PlaceholderAPI integration
- [ ] Per-player knowledge bases
- [ ] Response caching
- [ ] Alternative LLM providers

---

**Made with ❤️ for the Minecraft community**

**Version**: 1.0.0 | **Minecraft**: 1.21.1+ | **Java**: 21+
