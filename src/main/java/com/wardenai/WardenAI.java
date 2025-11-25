package com.wardenai;

import com.wardenai.services.CooldownManager;
import com.wardenai.services.GroqService;
import com.wardenai.services.KnowledgeBaseService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * WardenAI - Minecraft AI Assistant Plugin
 * Powered by Groq LLM
 *
 * Main plugin class that handles lifecycle, configuration, and service initialization.
 */
public class WardenAI extends JavaPlugin {

    // Service instances
    private GroqService groqService;
    private KnowledgeBaseService knowledgeBaseService;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        // Log plugin startup
        getLogger().info("========================================");
        getLogger().info("WardenAI v" + getDescription().getVersion());
        getLogger().info("Minecraft AI Assistant powered by Groq");
        getLogger().info("========================================");

        // Save default config if missing
        saveDefaultConfig();

        // Load and validate configuration
        if (!loadConfiguration()) {
            getLogger().severe("Failed to load configuration! Plugin will be disabled.");
            getLogger().severe("Please check your config.yml and fix any errors.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize services in correct order
        try {
            // Phase 6: Initialize Knowledge Base Service (handles missing file gracefully)
            knowledgeBaseService = new KnowledgeBaseService(this);
            knowledgeBaseService.loadKnowledgeBase();

            // Phase 5: Initialize Cooldown Manager
            int cooldownSeconds = getCooldownSeconds();
            cooldownManager = new CooldownManager(cooldownSeconds);
            getLogger().info("Cooldown system initialized (" + cooldownSeconds + " seconds)");

            // Phase 4: Initialize Groq Service
            String apiKey = getGroqApiKey();
            String model = getGroqModel();
            int maxTokens = getMaxTokens();
            double temperature = getTemperature();
            int timeout = getTimeoutSeconds();

            groqService = new GroqService(this, apiKey, model, maxTokens, temperature, timeout);
            getLogger().info("Groq service initialized with model: " + model);

            // Phase 5: Register commands (will be implemented in Phase 5)
            // TODO: Register WaiCommand in Phase 5
            getLogger().warning("Commands not yet registered (Phase 5)");

            getLogger().info("WardenAI enabled successfully!");

        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin services!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("WardenAI shutting down...");

        // Cleanup services
        if (cooldownManager != null) {
            cooldownManager.cleanup();
        }

        getLogger().info("WardenAI disabled successfully.");
    }

    /**
     * Load and validate configuration from config.yml
     *
     * @return true if configuration is valid, false otherwise
     */
    private boolean loadConfiguration() {
        try {
            // Reload config from disk
            reloadConfig();

            // Validate required fields
            String apiKey = getConfig().getString("groq.api-key", "");
            if (apiKey.isEmpty() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
                getLogger().severe("========================================");
                getLogger().severe("GROQ API KEY NOT CONFIGURED!");
                getLogger().severe("Please edit config.yml and set your Groq API key.");
                getLogger().severe("Get your API key from: https://console.groq.com/");
                getLogger().severe("========================================");
                return false;
            }

            // Validate model selection
            String model = getConfig().getString("groq.model", "");
            if (model.equals("openai/gpt-oss-20b")) {
                getLogger().warning("========================================");
                getLogger().warning("WARNING: You are using openai/gpt-oss-20b");
                getLogger().warning("This is a CONTENT MODERATION model, not an assistant!");
                getLogger().warning("Recommended models:");
                getLogger().warning("  - llama-3.3-70b-versatile (best quality)");
                getLogger().warning("  - llama-3.1-8b-instant (faster responses)");
                getLogger().warning("========================================");
                // Don't fail, but warn the user
            }

            // Validate numeric values are positive
            if (getMaxTokens() <= 0) {
                getLogger().warning("max-tokens must be positive, using default 8192");
            }
            if (getCooldownSeconds() < 0) {
                getLogger().warning("cooldown-seconds cannot be negative, using default 10");
            }
            if (getMaxMessageLength() <= 0) {
                getLogger().warning("max-message-length must be positive, using default 500");
            }
            if (getMinMessageLength() < 0) {
                getLogger().warning("min-message-length cannot be negative, using default 3");
            }

            getLogger().info("Configuration loaded successfully");
            return true;

        } catch (Exception e) {
            getLogger().severe("Error loading configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Configuration Getter Methods ====================

    /**
     * Get the Groq API key from configuration
     */
    public String getGroqApiKey() {
        return getConfig().getString("groq.api-key", "");
    }

    /**
     * Get the Groq model name from configuration
     */
    public String getGroqModel() {
        return getConfig().getString("groq.model", "llama-3.3-70b-versatile");
    }

    /**
     * Get the maximum tokens for API responses
     */
    public int getMaxTokens() {
        int tokens = getConfig().getInt("groq.max-tokens", 8192);
        return tokens > 0 ? tokens : 8192;
    }

    /**
     * Get the temperature for API requests
     */
    public double getTemperature() {
        return getConfig().getDouble("groq.temperature", 1.0);
    }

    /**
     * Get the timeout in seconds for API requests
     */
    public int getTimeoutSeconds() {
        return getConfig().getInt("groq.timeout-seconds", 30);
    }

    /**
     * Get the cooldown duration in seconds
     */
    public int getCooldownSeconds() {
        int cooldown = getConfig().getInt("limits.cooldown-seconds", 10);
        return cooldown >= 0 ? cooldown : 10;
    }

    /**
     * Get the maximum message length
     */
    public int getMaxMessageLength() {
        int max = getConfig().getInt("limits.max-message-length", 500);
        return max > 0 ? max : 500;
    }

    /**
     * Get the minimum message length
     */
    public int getMinMessageLength() {
        int min = getConfig().getInt("limits.min-message-length", 3);
        return min >= 0 ? min : 3;
    }

    /**
     * Get the maximum response length
     */
    public int getMaxResponseLength() {
        return getConfig().getInt("limits.max-response-length", 2000);
    }

    /**
     * Get a message from the configuration
     *
     * @param key Message key (without "messages." prefix)
     * @return The configured message, or a default if not found
     */
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "WardenAI: " + key);
    }

    /**
     * Check if debug logging is enabled for API requests
     */
    public boolean isDebugApiRequests() {
        return getConfig().getBoolean("debug.log-api-requests", false);
    }

    /**
     * Check if debug logging is enabled for API responses
     */
    public boolean isDebugApiResponses() {
        return getConfig().getBoolean("debug.log-api-responses", false);
    }

    /**
     * Check if error logging is enabled
     */
    public boolean isDebugErrors() {
        return getConfig().getBoolean("debug.log-errors", true);
    }

    /**
     * Check if personality system prompt is enabled
     */
    public boolean isPersonalityEnabled() {
        return getConfig().getBoolean("personality.enabled", true);
    }

    /**
     * Get the personality system prompt
     */
    public String getPersonalityPrompt() {
        return getConfig().getString("personality.system-prompt", "You are WardenAI, a helpful Minecraft assistant.");
    }

    // ==================== Service Getter Methods ====================

    /**
     * Get the Groq service instance
     */
    public GroqService getGroqService() {
        return groqService;
    }

    /**
     * Get the Knowledge Base service instance
     */
    public KnowledgeBaseService getKnowledgeBaseService() {
        return knowledgeBaseService;
    }

    /**
     * Get the Cooldown Manager instance
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
