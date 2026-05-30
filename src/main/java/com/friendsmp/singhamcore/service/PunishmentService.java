package com.friendsmp.singhamcore.service;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.repository.PunishmentRepository;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentService {

    private final SinghamCorePlugin plugin;
    private final PunishmentRepository repository;
    private final Map<UUID, Punishment> activeCache = new ConcurrentHashMap<>();
    private final Map<String, Punishment> activeIpBanCache = new ConcurrentHashMap<>();

    public PunishmentService(SinghamCorePlugin plugin, PunishmentRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Integer> loadActivePunishments() {
        return repository.loadActivePunishments().thenAccept(list -> {
            for (Punishment p : list) {
                if (p.getType() == PunishmentType.IP_BAN && p.getIpAddress() != null) {
                    activeIpBanCache.put(p.getIpAddress(), p);
                } else {
                    activeCache.put(p.getPlayerUuid(), p);
                }
            }
            scheduleExpirationTask();
        }).thenApply(v -> activeCache.size() + activeIpBanCache.size());
    }

    private void scheduleExpirationTask() {
        int refresh = plugin.getConfig().getInt("storage.cache-refresh-seconds", 30);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Instant now = Instant.now();
            activeCache.values().stream()
                    .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                    .toList()
                    .forEach(this::expirePunishment);
            activeIpBanCache.values().stream()
                    .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                    .toList()
                    .forEach(this::expirePunishment);
        }, 20L, refresh * 20L);
    }

    public CompletableFuture<Void> createPunishment(Punishment punishment) {
        if (punishment.isActive()) {
            if (punishment.getType() == PunishmentType.IP_BAN && punishment.getIpAddress() != null) {
                activeIpBanCache.put(punishment.getIpAddress(), punishment);
            } else {
                activeCache.put(punishment.getPlayerUuid(), punishment);
            }
        }
        return repository.save(punishment).thenCompose(v -> {
            com.friendsmp.singhamcore.models.StaffLogEntry entry = new com.friendsmp.singhamcore.models.StaffLogEntry(
                    null,
                    "CREATE_PUNISHMENT",
                    punishment.getPlayerUuid(),
                    punishment.getPlayerName(),
                    punishment.getReason(),
                    Instant.now()
            );
            // Audit log
            plugin.getAuditLogger().log("punishment.create", "Created punishment id=" + punishment.getId(), java.util.Map.of(
                    "player", punishment.getPlayerUuid().toString(),
                    "type", punishment.getType().name(),
                    "moderator", punishment.getModerator()
            ));
            return plugin.getDatabaseManager().saveStaffLogAsync(entry);
        });
    }

    public void expirePunishment(Punishment punishment) {
        punishment.setActive(false);
        if (punishment.getType() == PunishmentType.IP_BAN && punishment.getIpAddress() != null) {
            activeIpBanCache.remove(punishment.getIpAddress());
        } else {
            activeCache.remove(punishment.getPlayerUuid());
        }
        repository.updateActive(punishment.getId(), false);
        plugin.getAuditLogger().log("punishment.expire", "Expired punishment id=" + punishment.getId(), java.util.Map.of(
                "player", punishment.getPlayerUuid().toString(),
                "type", punishment.getType().name()
        ));
    }

    public CompletableFuture<Void> revokePunishment(UUID staffUuid, Punishment punishment) {
        punishment.setActive(false);
        if (punishment.getType() == PunishmentType.IP_BAN && punishment.getIpAddress() != null) {
            activeIpBanCache.remove(punishment.getIpAddress());
        } else {
            activeCache.remove(punishment.getPlayerUuid());
        }
        return repository.updateActiveAndRevoked(punishment.getId(), false, true).thenCompose(v -> {
            com.friendsmp.singhamcore.models.StaffLogEntry entry = new com.friendsmp.singhamcore.models.StaffLogEntry(
                    staffUuid,
                    "REVOKE_PUNISHMENT",
                    punishment.getPlayerUuid(),
                    punishment.getPlayerName(),
                    punishment.getReason(),
                    Instant.now()
            );
            plugin.getAuditLogger().log("punishment.revoke", "Revoked punishment id=" + punishment.getId(), java.util.Map.of(
                    "player", punishment.getPlayerUuid().toString(),
                    "type", punishment.getType().name(),
                    "moderator", staffUuid == null ? "unknown" : staffUuid.toString()
            ));
            return plugin.getDatabaseManager().saveStaffLogAsync(entry);
        });
    }

    public boolean isPlayerPunished(UUID uuid) {
        return activeCache.containsKey(uuid);
    }

    public Punishment getActivePunishment(UUID uuid) {
        return activeCache.get(uuid);
    }

    public Collection<Punishment> getActivePunishments() {
        var combined = new java.util.ArrayList<Punishment>(activeCache.values());
        combined.addAll(activeIpBanCache.values());
        return combined;
    }

    public CompletableFuture<Void> findAndRevoke(UUID staffUuid, UUID targetUuid) {
        Punishment p = activeCache.get(targetUuid);
        if (p != null) {
            return revokePunishment(staffUuid, p);
        }
        return repository.findActiveByPlayerUuid(targetUuid).thenCompose(opt -> {
            if (opt.isPresent()) {
                return revokePunishment(staffUuid, opt.get());
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> findAndRevoke(UUID staffUuid, UUID targetUuid, PunishmentType... allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return findAndRevoke(staffUuid, targetUuid);
        }
        EnumSet<PunishmentType> allowed = EnumSet.noneOf(PunishmentType.class);
        Collections.addAll(allowed, allowedTypes);
        Punishment p = activeCache.get(targetUuid);
        if (p != null && allowed.contains(p.getType())) {
            return revokePunishment(staffUuid, p);
        }
        return repository.findActiveByPlayerUuid(targetUuid).thenCompose(opt -> {
            if (opt.isPresent() && allowed.contains(opt.get().getType())) {
                return revokePunishment(staffUuid, opt.get());
            }
            return CompletableFuture.completedFuture(null);
        });
    }
}
