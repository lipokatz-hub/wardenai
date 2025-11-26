package com.wardenai.utils;

import org.bukkit.ChatColor;

/**
 * Utility class for formatting messages with color codes and prefixes.
 *
 * Provides consistent message formatting across the plugin with:
 * - Color code translation (& to §)
 * - Prefix handling
 * - Error message formatting
 *
 * All methods are static for easy access throughout the plugin.
 */
public class MessageFormatter {

    // Private constructor to prevent instantiation
    private MessageFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Format a message with a prefix and translate color codes.
     *
     * Example:
     *   formatMessage("&7[&bWardenAI&7]", "&aHello!")
     *   → "§7[§bWardenAI§7] §aHello!"
     *
     * @param prefix Message prefix (will be translated)
     * @param message Message content (will be translated)
     * @return Formatted message with translated color codes
     */
    public static String formatMessage(String prefix, String message) {
        if (prefix == null) prefix = "";
        if (message == null) message = "";

        String translatedPrefix = translateColorCodes(prefix);
        String translatedMessage = translateColorCodes(message);

        // Add space between prefix and message if both exist
        if (!translatedPrefix.isEmpty() && !translatedMessage.isEmpty()) {
            return translatedPrefix + " " + translatedMessage;
        } else if (!translatedPrefix.isEmpty()) {
            return translatedPrefix;
        } else {
            return translatedMessage;
        }
    }

    /**
     * Format an error message with a standard red error prefix.
     *
     * Example:
     *   formatError("Something went wrong!")
     *   → "§c✖ §7Something went wrong!"
     *
     * @param error Error message to format
     * @return Formatted error message with red prefix
     */
    public static String formatError(String error) {
        if (error == null || error.isEmpty()) {
            return ChatColor.RED + "✖ " + ChatColor.GRAY + "An unknown error occurred";
        }

        return ChatColor.RED + "✖ " + ChatColor.GRAY + translateColorCodes(error);
    }

    /**
     * Format a success message with a standard green checkmark prefix.
     *
     * Example:
     *   formatSuccess("Command executed successfully!")
     *   → "§a✔ §7Command executed successfully!"
     *
     * @param message Success message to format
     * @return Formatted success message with green prefix
     */
    public static String formatSuccess(String message) {
        if (message == null || message.isEmpty()) {
            return ChatColor.GREEN + "✔ " + ChatColor.GRAY + "Success";
        }

        return ChatColor.GREEN + "✔ " + ChatColor.GRAY + translateColorCodes(message);
    }

    /**
     * Format a warning message with a standard yellow warning prefix.
     *
     * Example:
     *   formatWarning("This action cannot be undone")
     *   → "§e⚠ §7This action cannot be undone"
     *
     * @param message Warning message to format
     * @return Formatted warning message with yellow prefix
     */
    public static String formatWarning(String message) {
        if (message == null || message.isEmpty()) {
            return ChatColor.YELLOW + "⚠ " + ChatColor.GRAY + "Warning";
        }

        return ChatColor.YELLOW + "⚠ " + ChatColor.GRAY + translateColorCodes(message);
    }

    /**
     * Translate alternate color codes (& format) to Minecraft color codes (§ format).
     *
     * Supports all standard Minecraft color codes:
     * - Colors: &0-&9, &a-&f
     * - Formatting: &k (obfuscated), &l (bold), &m (strikethrough), &n (underline), &o (italic)
     * - Reset: &r
     *
     * Uses Bukkit's ChatColor.translateAlternateColorCodes for reliability.
     *
     * @param text Text containing & color codes
     * @return Text with translated § color codes
     */
    public static String translateColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Use Bukkit's built-in method for reliable translation
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Strip all color codes from a message (both § and & formats).
     *
     * Useful for:
     * - Sanitizing player input before sending to API
     * - Measuring actual message length without color codes
     * - Logging without color formatting
     *
     * @param text Text containing color codes
     * @return Text with all color codes removed
     */
    public static String stripColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Strip § format (Minecraft native)
        String stripped = ChatColor.stripColor(text);

        // Strip & format (alternate codes)
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");

        return stripped;
    }

    /**
     * Get the actual length of a message without color codes.
     *
     * Example:
     *   getActualLength("&aHello &bWorld")
     *   → 11 (not 17)
     *
     * @param text Text potentially containing color codes
     * @return Length of text without color codes
     */
    public static int getActualLength(String text) {
        if (text == null) {
            return 0;
        }

        return stripColorCodes(text).length();
    }

    /**
     * Center a message in chat (assumes 65-character width).
     *
     * Example:
     *   centerMessage("Welcome to WardenAI")
     *   → "                   Welcome to WardenAI"
     *
     * @param message Message to center
     * @return Centered message with leading spaces
     */
    public static String centerMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        final int CENTER_PX = 154; // Minecraft chat width in pixels (approximate)
        final int CHAT_WIDTH = 65; // Character width approximation

        int messageLength = getActualLength(message);
        int spaces = (CHAT_WIDTH - messageLength) / 2;

        if (spaces <= 0) {
            return message; // Message is already too long
        }

        StringBuilder centered = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            centered.append(" ");
        }
        centered.append(message);

        return centered.toString();
    }
}
