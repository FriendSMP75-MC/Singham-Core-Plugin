package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final SinghamCorePlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Punishment> activePunishments = new ConcurrentHashMap<>();

    public PunishmentManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadActivePunishments() {
        databaseManager.loadActivePunishmentsAsync().thenAccept(punishments -> {
            for (Punishment punishment : punishments) {
                activePunishments.put(punishment.getPlayerUuid(), punishment);
            }
            scheduleExpirationTask();
        });
    }

    private void scheduleExpirationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Instant now = Instant.now();
            activePunishments.values().stream()
                    .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                    .toList()
                    .forEach(this::expirePunishment);
        }, 20L, plugin.getConfig().getInt("storage.cache-refresh-seconds", 30) * 20L);
    }

    public CompletableFuture<Void> createPunishment(UUID playerUuid, String playerName, PunishmentType type,
                                                   String moderator, String reason, long duration,
                                                   Instant expiresAt, String ipAddress, boolean active) {
        Punishment punish = new Punishment(0L, playerUuid, playerName, type, moderator, reason,
                duration, Instant.now(), expiresAt, ipAddress, active);

        if (active) {
            activePunishments.put(playerUuid, punish);
        }
        return databaseManager.savePunishmentAsync(punish);
    }

    public boolean isPlayerPunished(UUID playerUuid) {
        return activePunishments.containsKey(playerUuid);
    }

    public void expirePunishment(Punishment punishment) {
        punishment.setActive(false);
        activePunishments.remove(punishment.getPlayerUuid());
        databaseManager.updatePunishmentActiveAsync(punishment.getId(), false);
    }

    public Punishment getActivePunishment(UUID playerUuid) {
        return activePunishments.get(playerUuid);
    }

    public Collection<Punishment> getActivePunishments() {
        return activePunishments.values();
    }
}
