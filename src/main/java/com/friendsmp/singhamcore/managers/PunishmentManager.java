package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
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

    public void loadActivePunishments() {
        service.loadActivePunishments();
    }

    public void createPunishment(Punishment punishment) {
        service.createPunishment(punishment);
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
}
