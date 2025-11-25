package com.wardenai.services;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * Manages Groq API communication with proper async/sync thread handling.
 * CRITICAL: This service uses async execution to prevent server lag.
 *
 * Implementation will be completed in Phase 4.
 */
public class GroqService {

    private final JavaPlugin plugin;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final int timeoutSeconds;

    public GroqService(JavaPlugin plugin, String apiKey, String model,
                       int maxTokens, double temperature, int timeoutSeconds) {
        this.plugin = plugin;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Stub method - will be implemented in Phase 4 with proper async pattern
     *
     * @param player Player making the request
     * @param message Player's question
     * @param onSuccess Callback for successful response (runs on main thread)
     * @param onError Callback for errors (runs on main thread)
     */
    public void sendMessageAsync(Player player, String message,
                                   Consumer<String> onSuccess,
                                   Consumer<String> onError) {
        // TODO: Implement in Phase 4 with async infrastructure from Phase 3
        plugin.getLogger().warning("GroqService.sendMessageAsync called but not yet implemented");
        onError.accept("GroqService not yet implemented");
    }
}
