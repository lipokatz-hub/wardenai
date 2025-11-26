package com.wardenai.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for splitting long messages into chunks for Minecraft chat.
 *
 * Minecraft chat has limited display width, and long messages can be hard to read.
 * This class intelligently splits messages at word boundaries to maintain readability.
 *
 * CHUNKING STRATEGY:
 * ==================
 * 1. Try to split at word boundaries (spaces) before maxLength
 * 2. If no space found (very long word), hard-split at maxLength
 * 3. Preserve leading/trailing whitespace in chunks
 * 4. Handle edge cases (empty strings, single words, etc.)
 *
 * All methods are static for easy access throughout the plugin.
 */
public class ResponseChunker {

    // Default maximum length for a chat message chunk
    private static final int DEFAULT_MAX_LENGTH = 256;

    // Private constructor to prevent instantiation
    private ResponseChunker() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Split a message into chunks of specified maximum length.
     * Attempts to split at word boundaries for better readability.
     *
     * Example:
     *   chunkMessage("The quick brown fox jumps over the lazy dog", 20)
     *   → ["The quick brown fox", "jumps over the lazy", "dog"]
     *
     * @param message Message to chunk
     * @param maxLength Maximum length per chunk (must be > 0)
     * @return List of message chunks (never null, may be empty)
     */
    public static List<String> chunkMessage(String message, int maxLength) {
        List<String> chunks = new ArrayList<>();

        // Validate inputs
        if (message == null || message.isEmpty()) {
            return chunks; // Return empty list
        }

        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive, got: " + maxLength);
        }

        // If message fits in one chunk, return as-is
        if (message.length() <= maxLength) {
            chunks.add(message);
            return chunks;
        }

        // Split message into chunks using smart algorithm
        String remaining = message;
        while (remaining.length() > 0) {
            String chunk = extractNextChunk(remaining, maxLength);
            chunks.add(chunk);
            remaining = remaining.substring(chunk.length());
        }

        return chunks;
    }

    /**
     * Split a message into chunks using the default maximum length (256).
     *
     * @param message Message to chunk
     * @return List of message chunks
     */
    public static List<String> chunkMessage(String message) {
        return chunkMessage(message, DEFAULT_MAX_LENGTH);
    }

    /**
     * Extract the next chunk from a message, attempting to split at word boundaries.
     *
     * ALGORITHM:
     * ==========
     * 1. If remaining text fits in maxLength, return it all
     * 2. Otherwise, find the last space before maxLength
     * 3. If space found, split there (preserves words)
     * 4. If no space found, hard-split at maxLength (unavoidable)
     *
     * @param text Text to extract chunk from
     * @param maxLength Maximum chunk length
     * @return Next chunk (trimmed if split at word boundary)
     */
    private static String extractNextChunk(String text, int maxLength) {
        // If entire text fits, return it
        if (text.length() <= maxLength) {
            return text;
        }

        // Try to find last space before maxLength
        String candidate = text.substring(0, maxLength);
        int lastSpace = candidate.lastIndexOf(' ');

        if (lastSpace > 0) {
            // Found a space - split there for better readability
            // Trim to remove the space itself
            return text.substring(0, lastSpace).trim();
        } else {
            // No space found - must hard-split at maxLength
            // This happens with very long words or URLs
            return text.substring(0, maxLength);
        }
    }

    /**
     * Calculate how many chunks a message will produce.
     *
     * Useful for:
     * - Determining if chunking is needed
     * - Calculating total delay for sending all chunks
     * - Logging/debugging
     *
     * @param message Message to analyze
     * @param maxLength Maximum chunk length
     * @return Number of chunks (0 if message is null/empty)
     */
    public static int getChunkCount(String message, int maxLength) {
        if (message == null || message.isEmpty()) {
            return 0;
        }

        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive, got: " + maxLength);
        }

        return chunkMessage(message, maxLength).size();
    }

    /**
     * Calculate how many chunks a message will produce using default max length.
     *
     * @param message Message to analyze
     * @return Number of chunks
     */
    public static int getChunkCount(String message) {
        return getChunkCount(message, DEFAULT_MAX_LENGTH);
    }

    /**
     * Check if a message needs to be chunked.
     *
     * @param message Message to check
     * @param maxLength Maximum chunk length
     * @return true if message exceeds maxLength, false otherwise
     */
    public static boolean needsChunking(String message, int maxLength) {
        if (message == null) {
            return false;
        }

        return message.length() > maxLength;
    }

    /**
     * Check if a message needs chunking using default max length.
     *
     * @param message Message to check
     * @return true if message needs chunking
     */
    public static boolean needsChunking(String message) {
        return needsChunking(message, DEFAULT_MAX_LENGTH);
    }

    /**
     * Truncate a message to a maximum length with an ellipsis.
     *
     * This is useful as a fallback for extremely long messages that would
     * produce too many chunks.
     *
     * Example:
     *   truncate("The quick brown fox jumps over the lazy dog", 20)
     *   → "The quick brown f..."
     *
     * @param message Message to truncate
     * @param maxLength Maximum length (including ellipsis)
     * @return Truncated message with "..." suffix if truncated
     */
    public static String truncate(String message, int maxLength) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive, got: " + maxLength);
        }

        if (message.length() <= maxLength) {
            return message;
        }

        // Reserve 3 characters for "..."
        int truncateAt = Math.max(0, maxLength - 3);
        return message.substring(0, truncateAt) + "...";
    }

    /**
     * Get the default maximum chunk length.
     *
     * @return Default max length (256)
     */
    public static int getDefaultMaxLength() {
        return DEFAULT_MAX_LENGTH;
    }
}
