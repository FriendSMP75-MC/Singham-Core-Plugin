package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.ReputationRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReputationManager {

    private final DatabaseManager databaseManager;

    public ReputationManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Integer> adjustReputation(UUID playerUuid, int delta) {
        return databaseManager.adjustReputationAsync(playerUuid, delta);
    }

    public CompletableFuture<ReputationRecord> fetchReputation(UUID playerUuid) {
        return databaseManager.getReputationRecordAsync(playerUuid);
    }
}
