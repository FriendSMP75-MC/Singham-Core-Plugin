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

    public boolean authenticate(OfflinePlayer player, String pin) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        // Check for lockout
        Instant lockedUntil = lockouts.get(uuid);
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) return false;

        String keyUuid = uuid.toString();
        String keyName = player.getName();
        var cfg = plugin.getConfig();
        if (cfg.isConfigurationSection("staff-auth.pins")) {
            var section = cfg.getConfigurationSection("staff-auth.pins");
            String stored = null;
            if (section.contains(keyUuid)) stored = section.getString(keyUuid);
            else if (keyName != null && section.contains(keyName)) stored = section.getString(keyName);

            if (stored != null) {
                boolean ok;
                if (stored.startsWith("$2a$") || stored.startsWith("$2y$") || stored.startsWith("$2b$")) {
                    ok = BCrypt.checkpw(pin, stored);
                } else {
                    ok = stored.equals(pin);
                    // If stored as plaintext and matches, rehash and persist
                    if (ok) {
                        String hashed = BCrypt.hashpw(pin, BCrypt.gensalt(12));
                        section.set(keyUuid, hashed);
                        plugin.saveConfig();
                    }
                }

                if (ok) {
                    sessions.put(uuid, Instant.now().plusSeconds(cfg.getInt("staff-auth.session-seconds", 300)));
                    failedAttempts.remove(uuid);
                    lockouts.remove(uuid);
                    return true;
                }
            }
        }

        // failed attempt
        int fails = failedAttempts.getOrDefault(uuid, 0) + 1;
        failedAttempts.put(uuid, fails);
        int threshold = plugin.getConfig().getInt("staff-auth.lockout-threshold", 5);
        int lockSeconds = plugin.getConfig().getInt("staff-auth.lockout-seconds", 300);
        if (fails >= threshold) {
            lockouts.put(uuid, Instant.now().plusSeconds(lockSeconds));
            failedAttempts.remove(uuid);
        }
        return false;
    }

    public void revoke(UUID uuid) {
        sessions.remove(uuid);
    }

    public void onDisconnect(UUID uuid) {
        revoke(uuid);
    }
}
