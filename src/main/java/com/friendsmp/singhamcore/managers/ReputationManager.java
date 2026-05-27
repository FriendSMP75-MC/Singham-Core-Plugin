package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.ReputationRecord;
import com.friendsmp.singhamcore.models.ReputationTransaction;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ReputationManager {

    private final DatabaseManager databaseManager;
    private final Map<String, Instant> reputationCooldowns = new ConcurrentHashMap<>();
    private final SinghamCorePlugin plugin;

    public ReputationManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Integer> adjustReputation(UUID playerUuid, int delta) {
        return databaseManager.adjustReputationAsync(playerUuid, delta);
    }

    public CompletableFuture<Integer> adjustReputation(UUID playerUuid, int delta, UUID staffUuid, String reason) {
        int cooldownSeconds = plugin.getConfig().getInt("reputation.cooldown-seconds", 60);
        String key = staffUuid.toString() + ":" + playerUuid.toString();
        Instant now = Instant.now();
        Instant last = reputationCooldowns.get(key);
        if (last != null && now.isBefore(last.plusSeconds(cooldownSeconds))) {
            return CompletableFuture.completedFuture(Integer.MIN_VALUE);
        }
        reputationCooldowns.put(key, now);
        return databaseManager.adjustReputationAsync(playerUuid, delta)
                .thenApply(score -> {
                    databaseManager.saveReputationTransactionAsync(new ReputationTransaction(0, staffUuid, playerUuid, delta, reason, Instant.now()));
                    return score;
                });
    }

    public CompletableFuture<ReputationRecord> fetchReputation(UUID playerUuid) {
        return databaseManager.getReputationRecordAsync(playerUuid);
    }

    public CompletableFuture<List<ReputationTransaction>> loadTransactionHistory(UUID playerUuid, int limit, int offset) {
        return databaseManager.loadReputationTransactionsAsync(playerUuid, limit, offset);
    }

    public CompletableFuture<List<ReputationRecord>> loadLeaderboard(int limit, int offset) {
        return databaseManager.loadReputationLeaderboardAsync(limit, offset);
    }
}
