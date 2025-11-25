package com.wardenai.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages Groq API communication with proper async/sync thread handling.
 *
 * CRITICAL ASYNC PATTERN:
 * ========================
 * Minecraft servers run on a main thread that handles game logic, player actions,
 * and world updates. If we make HTTP requests on the main thread, the entire
 * server will freeze (lag) until the request completes.
 *
 * SOLUTION: Async Execution with Thread Switching
 * ------------------------------------------------
 * 1. API Call: Run on ASYNC thread (doesn't block server)
 *    - Use: Bukkit.getScheduler().runTaskAsynchronously()
 *    - Safe: Network I/O, heavy computation, external API calls
 *
 * 2. Player Interaction: Run on MAIN thread (required by Bukkit)
 *    - Use: Bukkit.getScheduler().runTask()
 *    - Required: player.sendMessage(), entity access, world modifications
 *
 * WRONG (causes crashes):
 *    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
 *        String response = callAPI();
 *        player.sendMessage(response); // CRASH! Async thread accessing Bukkit API
 *    });
 *
 * CORRECT (thread switching):
 *    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
 *        String response = callAPI(); // Async thread - OK
 *        Bukkit.getScheduler().runTask(plugin, () -> {
 *            player.sendMessage(response); // Main thread - OK
 *        });
 *    });
 *
 * This pattern is established in Phase 3 and used throughout the plugin.
 *
 * @see <a href="https://www.spigotmc.org/wiki/scheduler-programming/">Bukkit Scheduler Programming</a>
 */
public class GroqService {

    private final JavaPlugin plugin;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final int timeoutSeconds;

    // Track active requests for timeout handling (thread-safe)
    private final Map<UUID, BukkitTask> activeRequests = new ConcurrentHashMap<>();

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
     * Send a message to Groq API asynchronously with proper thread switching.
     *
     * ASYNC PATTERN IMPLEMENTATION:
     * ==============================
     * This method demonstrates the correct pattern for async operations in Bukkit:
     *
     * 1. Generate unique request ID for timeout tracking
     * 2. Schedule timeout task on main thread (will fire after timeoutSeconds)
     * 3. Run API call on ASYNC thread (doesn't block server)
     * 4. On success: Cancel timeout, switch to MAIN thread, call onSuccess callback
     * 5. On error: Cancel timeout, switch to MAIN thread, call onError callback
     *
     * THREAD SAFETY:
     * - API call runs on async thread pool (safe for I/O)
     * - Callbacks run on main thread (safe for Bukkit API)
     * - activeRequests map is ConcurrentHashMap (thread-safe)
     *
     * @param player Player making the request (used for logging, not accessed on async thread)
     * @param message Player's question to send to AI
     * @param onSuccess Callback for successful response (RUNS ON MAIN THREAD)
     * @param onError Callback for errors (RUNS ON MAIN THREAD)
     */
    public void sendMessageAsync(Player player, String message,
                                   Consumer<String> onSuccess,
                                   Consumer<String> onError) {

        // Generate unique request ID for this player's request
        final UUID requestId = UUID.randomUUID();

        // Phase 3: Timeout Handling
        // Schedule a timeout task that will fire if the API call takes too long
        // This runs on the MAIN thread and will be cancelled if the request completes
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Timeout occurred - clean up and notify player
            activeRequests.remove(requestId);
            plugin.getLogger().warning("Groq API request timed out after " + timeoutSeconds + " seconds");

            // Already on main thread, safe to call callback directly
            onError.accept("Request timed out after " + timeoutSeconds + " seconds");

        }, timeoutSeconds * 20L); // Convert seconds to ticks (20 ticks = 1 second)

        // Track this request for cleanup
        activeRequests.put(requestId, timeoutTask);

        // Phase 3: ASYNC EXECUTION PATTERN
        // Run the API call on an ASYNC thread to avoid blocking the server
        // CRITICAL: No Bukkit API calls allowed in this block!
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // === ASYNC THREAD - Safe for I/O, computation, external APIs ===

                // Phase 4: This is where the actual Groq API call will happen
                // For now (Phase 3), we just simulate the async pattern
                String response = callGroqAPI(player.getName(), message);

                // === Thread Switch: Return to MAIN THREAD for Bukkit API ===
                // CRITICAL: Must switch back to main thread before calling callbacks
                // Callbacks may interact with Bukkit API (player.sendMessage, etc.)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // === MAIN THREAD - Safe for Bukkit API ===

                    // Cancel the timeout task (request completed successfully)
                    BukkitTask timeout = activeRequests.remove(requestId);
                    if (timeout != null) {
                        timeout.cancel();
                    }

                    // Call success callback on main thread (safe for Bukkit API)
                    onSuccess.accept(response);
                });

            } catch (Exception e) {
                // === Error Handling: Return to MAIN THREAD ===
                // Even for errors, we must switch back to main thread for callbacks
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // === MAIN THREAD - Safe for Bukkit API ===

                    // Cancel the timeout task
                    BukkitTask timeout = activeRequests.remove(requestId);
                    if (timeout != null) {
                        timeout.cancel();
                    }

                    // Log error details
                    plugin.getLogger().warning("Groq API error: " + e.getMessage());
                    if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                        e.printStackTrace();
                    }

                    // Call error callback on main thread (safe for Bukkit API)
                    onError.accept("API error: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Call the Groq API (Phase 4 implementation placeholder).
     * This method runs on an ASYNC thread, so it's safe to do network I/O.
     *
     * Phase 3: Returns a stub response for testing async pattern
     * Phase 4: Will implement actual HTTP calls to Groq API
     *
     * @param playerName Player's name for context
     * @param message Player's question
     * @return AI response text
     * @throws Exception if API call fails
     */
    private String callGroqAPI(String playerName, String message) throws Exception {
        // === ASYNC THREAD - Safe for I/O ===

        // Phase 3: Stub implementation for testing async pattern
        plugin.getLogger().info("[ASYNC THREAD] Simulating API call for player: " + playerName);

        // Simulate network delay (will be real API call in Phase 4)
        Thread.sleep(500); // 500ms simulated delay

        // Phase 4: This is where we'll implement:
        // - Build JSON request with Groq4j or OkHttp
        // - Include knowledge base context
        // - Include personality system prompt
        // - Parse JSON response
        // - Extract message content
        // - Handle API error codes (401, 429, 400, 503)

        // For now, return a stub response
        return "[Phase 3 Stub] WardenAI received your question: \"" + message + "\" (API implementation in Phase 4)";
    }

    /**
     * Cancel all active requests (cleanup on plugin disable).
     * This should be called from the main plugin's onDisable() method.
     */
    public void cleanup() {
        // Cancel all timeout tasks
        for (BukkitTask task : activeRequests.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeRequests.clear();

        plugin.getLogger().info("GroqService cleanup: cancelled " + activeRequests.size() + " active requests");
    }
}
