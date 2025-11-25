package com.wardenai.services;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player cooldowns to prevent spam and handle rate limiting.
 *
 * THREAD SAFETY:
 * ==============
 * This class is thread-safe and can be safely accessed from:
 * - Main thread (command execution, cooldown checks)
 * - Async threads (if needed for concurrent operations)
 *
 * Uses ConcurrentHashMap for thread-safe map operations without explicit locking.
 *
 * COOLDOWN MECHANISM:
 * ===================
 * 1. Player uses /wai command
 * 2. Check hasCooldown(player) - returns true if still on cooldown
 * 3. If on cooldown, show "Please wait X seconds" message
 * 4. If not on cooldown, process request
 * 5. After successful API response, call setCooldown(player)
 * 6. Player must wait before next request
 *
 * BYPASS PERMISSION:
 * ==================
 * Players with "wardenai.bypass.cooldown" permission can skip cooldowns.
 * Useful for admins and testing.
 *
 * Implementation will be completed in Phase 5.
 */
public class CooldownManager {

    // Thread-safe map: Player UUID -> Last use timestamp (milliseconds)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final int cooldownSeconds;

    public CooldownManager(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * Check if a player is currently on cooldown.
     * Thread-safe: Can be called from any thread.
     *
     * @param player Player to check
     * @return true if player is on cooldown and should wait, false if ready
     */
    public boolean hasCooldown(Player player) {
        // Check bypass permission first
        if (player.hasPermission("wardenai.bypass.cooldown")) {
            return false; // Admins/OPs can bypass cooldown
        }

        // Get last use timestamp (thread-safe read)
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) {
            return false; // Never used before, no cooldown
        }

        // Calculate elapsed time since last use
        long elapsedMillis = System.currentTimeMillis() - lastUse;
        long cooldownMillis = cooldownSeconds * 1000L;

        // Return true if still on cooldown
        return elapsedMillis < cooldownMillis;
    }

    /**
     * Set cooldown for a player (marks current time as last use).
     * Thread-safe: Can be called from any thread.
     *
     * Should be called AFTER successful API response, not before.
     * This prevents cooldown consumption on errors.
     *
     * @param player Player to set cooldown for
     */
    public void setCooldown(Player player) {
        // Thread-safe write to ConcurrentHashMap
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Get remaining cooldown time in seconds.
     * Thread-safe: Can be called from any thread.
     *
     * @param player Player to check
     * @return Remaining seconds (0 if no cooldown), rounded up
     */
    public int getRemainingCooldown(Player player) {
        // Check bypass permission
        if (player.hasPermission("wardenai.bypass.cooldown")) {
            return 0;
        }

        // Get last use timestamp (thread-safe read)
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) {
            return 0; // Never used before
        }

        // Calculate remaining time
        long elapsedMillis = System.currentTimeMillis() - lastUse;
        long cooldownMillis = cooldownSeconds * 1000L;
        long remainingMillis = cooldownMillis - elapsedMillis;

        if (remainingMillis <= 0) {
            return 0; // Cooldown expired
        }

        // Round up to nearest second
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    /**
     * Remove expired cooldowns to prevent memory leak.
     * Thread-safe: ConcurrentHashMap supports safe iteration during modification.
     *
     * This should be called periodically (e.g., every 5 minutes) to clean up old entries.
     * In Phase 5, we'll add a scheduled task for automatic cleanup.
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;

        // Thread-safe: ConcurrentHashMap iterator supports removal during iteration
        cooldowns.entrySet().removeIf(entry -> {
            long elapsedMillis = now - entry.getValue();
            return elapsedMillis > cooldownMillis; // Remove if expired
        });
    }

    /**
     * Clear all cooldowns (cleanup on plugin disable).
     * Thread-safe: ConcurrentHashMap.clear() is thread-safe.
     */
    public void cleanup() {
        cooldowns.clear();
    }

    /**
     * Get the number of active cooldowns (for debugging/stats).
     * Thread-safe: ConcurrentHashMap.size() is thread-safe.
     *
     * @return Number of players currently in cooldown map
     */
    public int getActiveCooldowns() {
        return cooldowns.size();
    }
}
