package com.wardenai.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    // OkHttp client for API calls (thread-safe, reusable)
    private final OkHttpClient httpClient;

    // Gson for JSON serialization/deserialization
    private final Gson gson;

    // Groq API endpoint
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // Reference to knowledge base service (will be set after construction)
    private KnowledgeBaseService knowledgeBaseService;

    public GroqService(JavaPlugin plugin, String apiKey, String model,
                       int maxTokens, double temperature, int timeoutSeconds) {
        this.plugin = plugin;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;

        // Initialize OkHttp client with timeout
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        // Initialize Gson
        this.gson = new Gson();
    }

    /**
     * Set the knowledge base service reference (called after both services are constructed).
     * This avoids circular dependency during construction.
     */
    public void setKnowledgeBaseService(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
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
     * Call the Groq API using OkHttp.
     * This method runs on an ASYNC thread, so it's safe to do network I/O.
     *
     * Phase 4: Full implementation with OkHttp, Gson, and proper error handling.
     *
     * @param playerName Player's name for context
     * @param message Player's question
     * @return AI response text
     * @throws IOException if network error occurs
     * @throws GroqApiException if API returns an error
     */
    private String callGroqAPI(String playerName, String message) throws IOException, GroqApiException {
        // === ASYNC THREAD - Safe for I/O ===

        // Build the prompt with system context + user message
        JsonArray messages = buildMessages(playerName, message);

        // Build the JSON request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("temperature", temperature);

        String jsonBody = gson.toJson(requestBody);

        // Log the request if debug is enabled
        if (isDebugEnabled()) {
            plugin.getLogger().info("[Groq API] Request to " + model);
            plugin.getLogger().info("[Groq API] Player: " + playerName + ", Message length: " + message.length());
        }

        // Build HTTP request
        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .build();

        // Execute the request (blocking I/O - safe on async thread)
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            // Log the response if debug is enabled
            if (isDebugEnabled() && response.body() != null) {
                plugin.getLogger().info("[Groq API] Response code: " + response.code());
            }

            // Handle non-successful responses
            if (!response.isSuccessful()) {
                handleApiError(response.code(), responseBody);
            }

            // Parse the successful response
            return parseResponse(responseBody);
        }
    }

    /**
     * Build the messages array for the Groq API request.
     * Includes system prompt (personality + knowledge base) and user message.
     *
     * @param playerName Player's name
     * @param playerMessage Player's question
     * @return JsonArray of messages
     */
    private JsonArray buildMessages(String playerName, String playerMessage) {
        JsonArray messages = new JsonArray();

        // Get personality and knowledge base from config/services
        String personality = getPersonalityPrompt();
        String knowledgeBase = getKnowledgeBase();

        // Build system message (personality + knowledge base)
        StringBuilder systemPrompt = new StringBuilder();

        // Add personality if enabled
        if (personality != null && !personality.isEmpty()) {
            systemPrompt.append(personality);
        }

        // Add knowledge base if available
        if (knowledgeBase != null && !knowledgeBase.isEmpty()) {
            if (systemPrompt.length() > 0) {
                systemPrompt.append("\n\n");
            }
            systemPrompt.append("Additional context and knowledge:\n");
            systemPrompt.append(knowledgeBase);
        }

        // Add system message if we have content
        if (systemPrompt.length() > 0) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt.toString());
            messages.add(systemMessage);
        }

        // Add user message with player name
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Player " + playerName + " asks: " + playerMessage);
        messages.add(userMessage);

        return messages;
    }

    /**
     * Parse the Groq API response and extract the message content.
     *
     * @param responseBody JSON response from Groq API
     * @return The AI's response text
     * @throws GroqApiException if response format is invalid
     */
    private String parseResponse(String responseBody) throws GroqApiException {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            // Extract: choices[0].message.content
            if (!jsonResponse.has("choices")) {
                throw new GroqApiException("Invalid response: missing 'choices' field");
            }

            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices.size() == 0) {
                throw new GroqApiException("Invalid response: 'choices' array is empty");
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            if (!firstChoice.has("message")) {
                throw new GroqApiException("Invalid response: missing 'message' field");
            }

            JsonObject messageObj = firstChoice.getAsJsonObject("message");
            if (!messageObj.has("content")) {
                throw new GroqApiException("Invalid response: missing 'content' field");
            }

            String content = messageObj.get("content").getAsString();

            // Trim and validate
            if (content == null || content.trim().isEmpty()) {
                throw new GroqApiException("Invalid response: content is empty");
            }

            return content.trim();

        } catch (Exception e) {
            throw new GroqApiException("Failed to parse API response: " + e.getMessage());
        }
    }

    /**
     * Handle API error responses by throwing appropriate exceptions.
     *
     * Maps HTTP status codes to user-friendly error messages as per Phase 2 plan.
     *
     * @param statusCode HTTP status code
     * @param responseBody Response body (may contain error details)
     * @throws GroqApiException with user-friendly error message
     */
    private void handleApiError(int statusCode, String responseBody) throws GroqApiException {
        String errorMessage;

        switch (statusCode) {
            case 401:
                // Invalid API key
                errorMessage = "API key is invalid or missing";
                plugin.getLogger().severe("[Groq API] 401 Unauthorized - Check your API key in config.yml");
                throw new GroqApiException(errorMessage, GroqApiException.ErrorType.INVALID_API_KEY);

            case 429:
                // Rate limited
                errorMessage = "Rate limit exceeded - too many requests";
                plugin.getLogger().warning("[Groq API] 429 Rate Limited - Slow down requests or upgrade plan");
                throw new GroqApiException(errorMessage, GroqApiException.ErrorType.RATE_LIMITED);

            case 400:
                // Bad request - check for context_length_exceeded
                if (responseBody.contains("context_length_exceeded")) {
                    errorMessage = "Message is too long - context length exceeded";
                    plugin.getLogger().warning("[Groq API] 400 Context Length Exceeded");
                    throw new GroqApiException(errorMessage, GroqApiException.ErrorType.MESSAGE_TOO_LONG);
                } else {
                    errorMessage = "Bad request - invalid parameters";
                    plugin.getLogger().warning("[Groq API] 400 Bad Request: " + responseBody);
                    throw new GroqApiException(errorMessage, GroqApiException.ErrorType.BAD_REQUEST);
                }

            case 503:
                // Service unavailable
                errorMessage = "Groq API is temporarily unavailable";
                plugin.getLogger().warning("[Groq API] 503 Service Unavailable");
                throw new GroqApiException(errorMessage, GroqApiException.ErrorType.SERVICE_UNAVAILABLE);

            default:
                // Generic error
                errorMessage = "API error (code " + statusCode + ")";
                plugin.getLogger().warning("[Groq API] Error " + statusCode + ": " + responseBody);
                throw new GroqApiException(errorMessage, GroqApiException.ErrorType.GENERIC);
        }
    }

    /**
     * Get personality prompt from config (accessed via WardenAI instance).
     * Safe to call from async thread as it only reads configuration.
     */
    private String getPersonalityPrompt() {
        try {
            // Cast plugin to WardenAI to access config methods
            if (plugin instanceof com.wardenai.WardenAI) {
                com.wardenai.WardenAI wardenAI = (com.wardenai.WardenAI) plugin;
                if (wardenAI.isPersonalityEnabled()) {
                    return wardenAI.getPersonalityPrompt();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get personality prompt: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get knowledge base content from service.
     * Safe to call from async thread as it only reads cached content.
     */
    private String getKnowledgeBase() {
        if (knowledgeBaseService != null) {
            return knowledgeBaseService.getKnowledgeBase();
        }
        return null;
    }

    /**
     * Check if debug logging is enabled.
     */
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

    /**
     * Custom exception for Groq API errors with error type classification.
     */
    public static class GroqApiException extends Exception {
        public enum ErrorType {
            INVALID_API_KEY,
            RATE_LIMITED,
            MESSAGE_TOO_LONG,
            SERVICE_UNAVAILABLE,
            BAD_REQUEST,
            GENERIC
        }

        private final ErrorType errorType;

        public GroqApiException(String message) {
            super(message);
            this.errorType = ErrorType.GENERIC;
        }

        public GroqApiException(String message, ErrorType errorType) {
            super(message);
            this.errorType = errorType;
        }

        public ErrorType getErrorType() {
            return errorType;
        }
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
