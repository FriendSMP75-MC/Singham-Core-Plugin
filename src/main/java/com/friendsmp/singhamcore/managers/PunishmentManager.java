package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final SinghamCorePlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Map<PunishmentType, Punishment>> activePunishments = new ConcurrentHashMap<>();
    private final Map<String, Punishment> activeIpBans = new ConcurrentHashMap<>();

    public PunishmentManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadActivePunishments() {
        databaseManager.loadActivePunishmentsAsync().thenAccept(punishments -> {
            Instant now = Instant.now();
            for (Punishment punishment : punishments) {
                if (isExpired(punishment, now)) {
                    expirePunishment(punishment, "SYSTEM");
                    continue;
                }
                if (punishment.getType() == PunishmentType.IP_BAN && punishment.getIpAddress() != null) {
                    activeIpBans.put(punishment.getIpAddress(), punishment);
                    continue;
                }
                if (punishment.getPlayerUuid() == null) {
                    continue;
                }
                activePunishments
                        .computeIfAbsent(punishment.getPlayerUuid(), uuid -> new ConcurrentHashMap<>())
                        .put(punishment.getType(), punishment);
            }
            scheduleExpirationTask();
        });
    }

    private void scheduleExpirationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Instant now = Instant.now();
            activePunishments.values().stream()
                    .flatMap(map -> map.values().stream())
                    .filter(punishment -> isExpired(punishment, now))
                    .toList()
                    .forEach(punishment -> expirePunishment(punishment, "SYSTEM"));
            activeIpBans.values().stream()
                    .filter(punishment -> isExpired(punishment, now))
                    .toList()
                    .forEach(punishment -> expirePunishment(punishment, "SYSTEM"));
        }, 20L, plugin.getConfig().getInt("storage.cache-refresh-seconds", 30) * 20L);
    }

    public CompletableFuture<Void> createPunishment(UUID playerUuid, String playerName, PunishmentType type,
                                                   String moderator, String reason, long duration,
                                                   Instant expiresAt, String ipAddress, boolean active) {
        Punishment punish = new Punishment(0L, playerUuid, playerName, type, moderator, reason,
                duration, Instant.now(), expiresAt, ipAddress, active, null, null);

        if (active) {
            if (type == PunishmentType.IP_BAN && ipAddress != null) {
                activeIpBans.put(ipAddress, punish);
            } else if (playerUuid != null) {
                activePunishments
                        .computeIfAbsent(playerUuid, uuid -> new ConcurrentHashMap<>())
                        .put(type, punish);
            }
        }
        return databaseManager.savePunishmentAsync(punish);
    }

    public boolean hasActivePunishment(UUID playerUuid, PunishmentType... types) {
        Map<PunishmentType, Punishment> map = activePunishments.get(playerUuid);
        if (map == null || map.isEmpty()) {
            return false;
        }
        if (types == null || types.length == 0) {
            return true;
        }
        for (PunishmentType type : types) {
            if (map.containsKey(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIpBanned(String ipAddress) {
        return ipAddress != null && activeIpBans.containsKey(ipAddress);
    }

    public Punishment getActivePunishment(UUID playerUuid, PunishmentType type) {
        Map<PunishmentType, Punishment> map = activePunishments.get(playerUuid);
        return map == null ? null : map.get(type);
    }

    public Collection<Punishment> getActivePunishments(UUID playerUuid) {
        Map<PunishmentType, Punishment> map = activePunishments.get(playerUuid);
        return map == null ? java.util.List.of() : map.values();
    }

    public Punishment getActiveIpBan(String ipAddress) {
        return ipAddress == null ? null : activeIpBans.get(ipAddress);
    }

    public Collection<Punishment> getAllActivePunishments() {
        return activePunishments.values().stream().flatMap(map -> map.values().stream()).toList();
    }

    public void expirePunishment(Punishment punishment, String revokedBy) {
        if (punishment == null || !punishment.isActive()) {
            return;
        }
        punishment.setActive(false);
        punishment.setRevokedAt(Instant.now());
        punishment.setRevokedBy(revokedBy);
        if (punishment.getType() == PunishmentType.IP_BAN && punishment.getIpAddress() != null) {
            activeIpBans.remove(punishment.getIpAddress());
        } else if (punishment.getPlayerUuid() != null) {
            Map<PunishmentType, Punishment> map = activePunishments.get(punishment.getPlayerUuid());
            if (map != null) {
                map.remove(punishment.getType());
                if (map.isEmpty()) {
                    activePunishments.remove(punishment.getPlayerUuid());
                }
            }
        }
        databaseManager.updatePunishmentActiveAsync(punishment.getId(), false, punishment.getRevokedAt(), punishment.getRevokedBy());
    }

    public CompletableFuture<Boolean> revokePunishments(UUID playerUuid, Set<PunishmentType> types, String revokedBy) {
        Map<PunishmentType, Punishment> map = activePunishments.get(playerUuid);
        if (map == null || map.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        boolean revoked = false;
        for (PunishmentType type : types) {
            Punishment punishment = map.get(type);
            if (punishment != null) {
                expirePunishment(punishment, revokedBy);
                revoked = true;
            }
        }
        return CompletableFuture.completedFuture(revoked);
    }

    public boolean isExpired(Punishment punishment, Instant now) {
        return punishment.getExpiresAt() != null && punishment.getExpiresAt().isBefore(now);
    }

}
