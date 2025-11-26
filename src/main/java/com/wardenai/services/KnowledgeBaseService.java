package com.wardenai.services;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Loads and manages the knowledge base content for AI prompts.
 * Handles missing files gracefully (knowledge base is optional).
 *
 * LOADING STRATEGY:
 * =================
 * 1. Try to load from plugin data folder (plugins/WardenAI/knowledge-base.txt)
 *    - This allows server admins to customize the knowledge base
 * 2. If not found, try to load from plugin resources (bundled in JAR)
 *    - This provides a default knowledge base
 * 3. If still not found, continue without knowledge base
 *    - Plugin works normally, just without extra context
 *
 * The knowledge base is cached in memory for performance.
 */
public class KnowledgeBaseService {

    private final JavaPlugin plugin;
    private String knowledgeBaseContent = "";
    private static final String KB_FILENAME = "knowledge-base.txt";

    public KnowledgeBaseService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load the knowledge base from disk or resources.
     * This method handles missing files gracefully - knowledge base is OPTIONAL.
     *
     * LOAD ORDER:
     * 1. Check plugin data folder (plugins/WardenAI/knowledge-base.txt)
     * 2. Check plugin resources (bundled in JAR)
     * 3. If neither found, continue without knowledge base
     */
    public void loadKnowledgeBase() {
        try {
            // === STEP 1: Try to load from plugin data folder ===
            // This allows server admins to customize without recompiling
            File dataFolder = plugin.getDataFolder();
            File kbFile = new File(dataFolder, KB_FILENAME);

            if (kbFile.exists() && kbFile.isFile()) {
                plugin.getLogger().info("Loading knowledge base from data folder...");
                knowledgeBaseContent = loadFromFile(kbFile.toPath());
                plugin.getLogger().info("Knowledge base loaded successfully (" + knowledgeBaseContent.length() + " characters)");
                return;
            }

            // === STEP 2: Try to load from plugin resources (bundled) ===
            plugin.getLogger().info("Knowledge base not found in data folder, checking resources...");
            InputStream resourceStream = plugin.getResource(KB_FILENAME);

            if (resourceStream != null) {
                plugin.getLogger().info("Loading knowledge base from plugin resources...");
                knowledgeBaseContent = loadFromInputStream(resourceStream);
                plugin.getLogger().info("Knowledge base loaded from resources (" + knowledgeBaseContent.length() + " characters)");

                // Optionally save to data folder for future customization
                saveDefaultKnowledgeBase(kbFile);
                return;
            }

            // === STEP 3: No knowledge base found - continue without it ===
            plugin.getLogger().warning("========================================");
            plugin.getLogger().warning("No knowledge base found");
            plugin.getLogger().warning("Plugin will work normally without it");
            plugin.getLogger().warning("To add custom knowledge:");
            plugin.getLogger().warning("  1. Create: plugins/WardenAI/" + KB_FILENAME);
            plugin.getLogger().warning("  2. Add Minecraft-specific information");
            plugin.getLogger().warning("  3. Reload the plugin");
            plugin.getLogger().warning("========================================");
            knowledgeBaseContent = "";

        } catch (Exception e) {
            // Even if loading fails, don't crash the plugin
            plugin.getLogger().warning("Error loading knowledge base: " + e.getMessage());
            plugin.getLogger().warning("Continuing without knowledge base");
            knowledgeBaseContent = "";
        }
    }

    /**
     * Load content from a file on disk.
     *
     * @param path Path to the file
     * @return File contents as a string
     * @throws IOException if file cannot be read
     */
    private String loadFromFile(Path path) throws IOException {
        return Files.lines(path, StandardCharsets.UTF_8)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    /**
     * Load content from an InputStream (plugin resources).
     *
     * @param inputStream Input stream to read
     * @return Stream contents as a string
     * @throws IOException if stream cannot be read
     */
    private String loadFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining("\n"))
                    .trim();
        }
    }

    /**
     * Save the default knowledge base from resources to the data folder.
     * This creates a customizable copy for server admins.
     *
     * @param targetFile Target file in data folder
     */
    private void saveDefaultKnowledgeBase(File targetFile) {
        try {
            // Ensure parent directory exists
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Save content to file
            Files.write(targetFile.toPath(), knowledgeBaseContent.getBytes(StandardCharsets.UTF_8));
            plugin.getLogger().info("Saved default knowledge base to: " + targetFile.getPath());
            plugin.getLogger().info("You can now customize this file and reload the plugin");

        } catch (Exception e) {
            plugin.getLogger().warning("Could not save default knowledge base: " + e.getMessage());
            // Not a critical error - continue
        }
    }

    /**
     * Get the cached knowledge base content.
     * This method is thread-safe as the content is immutable after loading.
     *
     * @return Knowledge base content (may be empty string if not loaded)
     */
    public String getKnowledgeBase() {
        return knowledgeBaseContent;
    }

    /**
     * Check if a knowledge base is loaded.
     *
     * @return true if knowledge base has content, false otherwise
     */
    public boolean hasKnowledgeBase() {
        return knowledgeBaseContent != null && !knowledgeBaseContent.trim().isEmpty();
    }

    /**
     * Reload the knowledge base from disk.
     * This allows admins to update the KB without restarting the server.
     *
     * This method will be used by a future /wai reload command.
     */
    public void reload() {
        plugin.getLogger().info("Reloading knowledge base...");
        loadKnowledgeBase();
    }
}
