package com.wardenai.commands;

import com.wardenai.WardenAI;
import com.wardenai.services.GroqService;
import com.wardenai.utils.ResponseChunker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command handler for /wai command.
 *
 * Implements a comprehensive 10-step validation chain to ensure:
 * - Security (input sanitization, permission checks)
 * - Rate limiting (cooldown system)
 * - User experience (clear error messages, async execution)
 *
 * EXECUTION FLOW:
 * 1. Validate sender is a player
 * 2. Check permissions
 * 3. Validate arguments
 * 4. Validate message length
 * 5. Sanitize input
 * 6. Check cooldown
 * 7. Send "thinking" message
 * 8. Call API asynchronously
 * 9. Success: Send response to player
 * 10. Error: Send friendly error message
 */
public class WaiCommand implements CommandExecutor {

    private final WardenAI plugin;

    public WaiCommand(WardenAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ═══════════════════════════════════════════════════════════════
        // STEP 1: SENDER VALIDATION
        // ═══════════════════════════════════════════════════════════════

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // ═══════════════════════════════════════════════════════════════
        // STEP 2: PERMISSION CHECK
        // ═══════════════════════════════════════════════════════════════

        if (!player.hasPermission("wardenai.use")) {
            player.sendMessage(formatMessage(
                ChatColor.RED + "You don't have permission to use this command."
            ));
            return true;
        }

        // ═══════════════════════════════════════════════════════════════
        // STEP 3: ARGUMENTS VALIDATION
        // ═══════════════════════════════════════════════════════════════

        if (args.length == 0) {
            player.sendMessage(formatMessage(
                ChatColor.YELLOW + "Usage: /wai <message>"
            ));
            player.sendMessage(formatMessage(
                ChatColor.GRAY + "Example: /wai How do I craft a diamond sword?"
            ));
            return true;
        }

        // Join all arguments into a single message
        String message = String.join(" ", args);

        // ═══════════════════════════════════════════════════════════════
        // STEP 4: MESSAGE LENGTH VALIDATION
        // ═══════════════════════════════════════════════════════════════

        int minLength = plugin.getMinMessageLength();
        int maxLength = plugin.getMaxMessageLength();

        if (message.length() < minLength) {
            player.sendMessage(formatMessage(
                ChatColor.RED + "Your message is too short (minimum " + minLength + " characters)."
            ));
            return true;
        }

        if (message.length() > maxLength) {
            player.sendMessage(formatMessage(
                ChatColor.RED + "Your message is too long (maximum " + maxLength + " characters)."
            ));
            player.sendMessage(formatMessage(
                ChatColor.GRAY + "Please shorten your question and try again."
            ));
            return true;
        }

        // ═══════════════════════════════════════════════════════════════
        // STEP 5: INPUT SANITIZATION (Security)
        // ═══════════════════════════════════════════════════════════════

        String sanitizedMessage = sanitizeInput(message);

        // ═══════════════════════════════════════════════════════════════
        // STEP 6: COOLDOWN CHECK
        // ═══════════════════════════════════════════════════════════════

        if (plugin.getCooldownManager().hasCooldown(player)) {
            int remainingSeconds = plugin.getCooldownManager().getRemainingCooldown(player);

            // Get cooldown message from config and replace placeholder
            String cooldownMsg = plugin.getMessage("cooldown")
                    .replace("{seconds}", String.valueOf(remainingSeconds));

            player.sendMessage(formatMessage(ChatColor.YELLOW + cooldownMsg));
            return true;
        }

        // ═══════════════════════════════════════════════════════════════
        // STEP 7: SEND "THINKING" MESSAGE
        // ═══════════════════════════════════════════════════════════════

        String thinkingMsg = plugin.getMessage("thinking");
        player.sendMessage(formatMessage(ChatColor.GRAY + thinkingMsg));

        // ═══════════════════════════════════════════════════════════════
        // STEP 8: CALL GROQ API ASYNCHRONOUSLY
        // ═══════════════════════════════════════════════════════════════

        plugin.getGroqService().sendMessageAsync(
                player,
                sanitizedMessage,

                // ═══════════════════════════════════════════════════════
                // STEP 9: SUCCESS CALLBACK (runs on main thread)
                // ═══════════════════════════════════════════════════════

                (response) -> {
                    // Check if response needs chunking (default 256 chars per chunk)
                    int maxChunkLength = 256;

                    if (ResponseChunker.needsChunking(response, maxChunkLength)) {
                        // Split response into chunks at word boundaries
                        List<String> chunks = ResponseChunker.chunkMessage(response, maxChunkLength);

                        // Send each chunk with a delay for readability
                        for (int i = 0; i < chunks.size(); i++) {
                            final String chunk = chunks.get(i);
                            final int chunkNumber = i;
                            final int totalChunks = chunks.size();

                            // Calculate delay: 4 ticks (200ms) per chunk
                            final long delay = i * 4L;

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                // Format each chunk with prefix
                                String prefix = ChatColor.AQUA + "WardenAI" + ChatColor.GRAY + " [" + (chunkNumber + 1) + "/" + totalChunks + "]:";
                                player.sendMessage(formatMessage(prefix + " " + ChatColor.WHITE + chunk));
                            }, delay);
                        }
                    } else {
                        // Single message - send immediately
                        player.sendMessage(formatMessage(ChatColor.AQUA + "WardenAI" + ChatColor.GRAY + ": " + ChatColor.WHITE + response));
                    }

                    // Set cooldown AFTER successful response
                    // This prevents cooldown consumption on errors
                    plugin.getCooldownManager().setCooldown(player);
                },

                // ═══════════════════════════════════════════════════════
                // STEP 10: ERROR CALLBACK (runs on main thread)
                // ═══════════════════════════════════════════════════════

                (error) -> {
                    // Map error to user-friendly message from config
                    String errorMsg = mapErrorToMessage(error);
                    player.sendMessage(formatMessage(ChatColor.RED + errorMsg));

                    // DO NOT set cooldown on error - allow retry
                }
        );

        return true;
    }

    /**
     * Sanitize player input for security.
     *
     * SECURITY MEASURES:
     * - Strip Minecraft color codes (§, &)
     * - Remove control characters
     * - Trim whitespace
     * - Replace multiple spaces with single space
     *
     * @param input Raw player input
     * @return Sanitized input safe to send to API
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Strip Minecraft color codes (both § and & formats)
        String sanitized = ChatColor.stripColor(input);
        sanitized = sanitized.replaceAll("&[0-9a-fk-or]", "");

        // Remove control characters (except newlines which we'll replace)
        sanitized = sanitized.replaceAll("\\p{Cntrl}", " ");

        // Replace multiple spaces with single space
        sanitized = sanitized.replaceAll("\\s+", " ");

        // Trim leading/trailing whitespace
        sanitized = sanitized.trim();

        return sanitized;
    }

    /**
     * Map API errors to user-friendly messages from config.
     *
     * This method checks if the error is a GroqApiException and maps
     * the error type to the appropriate config message.
     *
     * @param error Error message from GroqService
     * @return User-friendly error message
     */
    private String mapErrorToMessage(String error) {
        // Try to determine error type from message content
        // In a future enhancement, we could pass the exception type directly

        if (error.contains("API key is invalid") || error.contains("not configured")) {
            return plugin.getMessage("error-not-configured");
        } else if (error.contains("Rate limit")) {
            return plugin.getMessage("error-rate-limit");
        } else if (error.contains("too long") || error.contains("context length")) {
            return plugin.getMessage("error-too-long");
        } else if (error.contains("temporarily unavailable")) {
            return plugin.getMessage("error-unavailable");
        } else if (error.contains("timed out") || error.contains("timeout")) {
            return plugin.getMessage("error-timeout");
        } else if (error.contains("token credit")) {
            // Original requirement from instructions.md
            return plugin.getMessage("no-tokens");
        } else {
            // Generic error
            return plugin.getMessage("error-generic");
        }
    }

    /**
     * Format a message with the plugin prefix.
     *
     * @param message Message to format
     * @return Formatted message with prefix
     */
    private String formatMessage(String message) {
        String prefix = plugin.getMessage("prefix");
        prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        return prefix + " " + message;
    }
}
