package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.repository.SQLPunishmentRepository;
import com.friendsmp.singhamcore.service.PunishmentService;
import com.friendsmp.singhamcore.database.DatabaseManager;

import java.util.Collection;
import java.util.UUID;

public class PunishmentManager {

    private final PunishmentService service;

    public PunishmentManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.service = new PunishmentService(plugin, new SQLPunishmentRepository(databaseManager));
    }

    public java.util.concurrent.CompletableFuture<Integer> loadActivePunishments() {
        return service.loadActivePunishments();
    }

    public java.util.concurrent.CompletableFuture<Void> createPunishment(java.util.UUID playerUuid, String playerName, com.friendsmp.singhamcore.punishments.PunishmentType type,
                                                                         String moderator, String reason, long duration,
                                                                         java.time.Instant expiresAt, String ipAddress, boolean active) {
        Punishment punish = new Punishment(0L, playerUuid, playerName, type, moderator, reason, duration, java.time.Instant.now(), expiresAt, ipAddress, active);
        return service.createPunishment(punish);
    }

    public boolean isPlayerPunished(UUID uuid) {
        return service.isPlayerPunished(uuid);
    }

    public void expirePunishment(Punishment punishment) {
        service.expirePunishment(punishment);
    }

    public Punishment getActivePunishment(UUID uuid) {
        return service.getActivePunishment(uuid);
    }

    public Collection<Punishment> getActivePunishments() {
        return service.getActivePunishments();
    }

    public java.util.concurrent.CompletableFuture<Void> revokePunishment(java.util.UUID staffUuid, java.util.UUID targetUuid) {
        return service.findAndRevoke(staffUuid, targetUuid);
    }

    public java.util.concurrent.CompletableFuture<Void> revokePunishment(java.util.UUID staffUuid, java.util.UUID targetUuid, PunishmentType... allowedTypes) {
        return service.findAndRevoke(staffUuid, targetUuid, allowedTypes);
    }
}
