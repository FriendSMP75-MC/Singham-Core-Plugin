package com.friendsmp.singhamcore.service;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import org.bukkit.OfflinePlayer;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final SinghamCorePlugin plugin;
    private final Map<UUID, Instant> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lockouts = new ConcurrentHashMap<>();

    public AuthService(SinghamCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAuthenticated(UUID uuid) {
        Instant expiry = sessions.get(uuid);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            sessions.remove(uuid);
            return false;
        }
        return true;
    }

    public boolean sessionExpired(UUID uuid) {
        Instant expiry = sessions.get(uuid);
        return expiry != null && expiry.isBefore(Instant.now());
    }

    public boolean isPinRegistered(OfflinePlayer player) {
        return getStoredPin(player) != null;
    }

    public String validatePinFormat(String pin) {
        if (pin == null) {
            return "invalid";
        }
        int min = plugin.getConfig().getInt("staff-auth.min-length", 4);
        int max = plugin.getConfig().getInt("staff-auth.max-length", 12);
        boolean numericOnly = plugin.getConfig().getBoolean("staff-auth.numeric-only", false);

        if (pin.length() < min || pin.length() > max) {
            return "length";
        }
        if (numericOnly && !pin.matches("\\d+")) {
            return "numeric";
        }
        if (!pin.matches("[A-Za-z0-9]+")) {
            return "format";
        }
        return null;
    }

    public boolean setupPin(OfflinePlayer player, String pin) {
        if (player == null || player.getUniqueId() == null) return false;
        if (isPinRegistered(player)) return false;

        String validation = validatePinFormat(pin);
        if (validation != null) {
            return false;
        }

        String hashed = BCrypt.hashpw(pin, BCrypt.gensalt(12));
        var cfg = plugin.getConfig();
        if (!cfg.isConfigurationSection("staff-auth.pins")) {
            cfg.createSection("staff-auth.pins");
        }
        var section = cfg.getConfigurationSection("staff-auth.pins");
        section.set(player.getUniqueId().toString(), hashed);
        plugin.saveConfig();
        createSession(player.getUniqueId());
        return true;
    }

    public boolean verifyPin(OfflinePlayer player, String pin) {
        if (player == null || player.getUniqueId() == null) return false;
        UUID uuid = player.getUniqueId();
        if (isLocked(uuid)) return false;

        String stored = getStoredPin(player);
        if (stored == null) {
            return false;
        }

        boolean ok = checkPin(pin, stored, player);
        if (ok) {
            createSession(uuid);
            failedAttempts.remove(uuid);
            lockouts.remove(uuid);
            return true;
        }

        recordFailure(uuid);
        return false;
    }

    public boolean isLocked(UUID uuid) {
        Instant lockedUntil = lockouts.get(uuid);
        if (lockedUntil == null) return false;
        if (lockedUntil.isBefore(Instant.now())) {
            lockouts.remove(uuid);
            return false;
        }
        return true;
    }

    private void createSession(UUID uuid) {
        int seconds = plugin.getConfig().getInt("staff-auth.session-seconds", 300);
        sessions.put(uuid, Instant.now().plusSeconds(seconds));
    }

    private String getStoredPin(OfflinePlayer player) {
        var cfg = plugin.getConfig();
        if (!cfg.isConfigurationSection("staff-auth.pins")) {
            return null;
        }
        var section = cfg.getConfigurationSection("staff-auth.pins");
        String uuidKey = player.getUniqueId().toString();
        if (section.contains(uuidKey)) {
            return section.getString(uuidKey);
        }
        String playerName = player.getName();
        if (playerName != null && section.contains(playerName)) {
            return section.getString(playerName);
        }
        return null;
    }

    private boolean checkPin(String pin, String stored, OfflinePlayer player) {
        boolean ok;
        if (stored.startsWith("$2a$") || stored.startsWith("$2y$") || stored.startsWith("$2b$")) {
            ok = BCrypt.checkpw(pin, stored);
        } else {
            ok = stored.equals(pin);
            if (ok) {
                rehashLegacyPin(player, pin);
            }
        }
        return ok;
    }

    private void rehashLegacyPin(OfflinePlayer player, String pin) {
        var cfg = plugin.getConfig();
        var section = cfg.getConfigurationSection("staff-auth.pins");
        if (section == null) {
            return;
        }
        String hashed = BCrypt.hashpw(pin, BCrypt.gensalt(12));
        section.set(player.getUniqueId().toString(), hashed);
        plugin.saveConfig();
    }

    private void recordFailure(UUID uuid) {
        int fails = failedAttempts.getOrDefault(uuid, 0) + 1;
        failedAttempts.put(uuid, fails);
        int threshold = plugin.getConfig().getInt("staff-auth.lockout-threshold", 5);
        int lockSeconds = plugin.getConfig().getInt("staff-auth.lockout-seconds", 300);
        if (fails >= threshold) {
            lockouts.put(uuid, Instant.now().plusSeconds(lockSeconds));
            failedAttempts.remove(uuid);
        }
    }

    public void revoke(UUID uuid) {
        sessions.remove(uuid);
    }

    public void onDisconnect(UUID uuid) {
        revoke(uuid);
    }
}
