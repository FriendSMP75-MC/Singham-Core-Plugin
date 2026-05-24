package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.database.AsyncDatabaseExecutor;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.Warning;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLWarningRepository implements WarningRepository {

    private final DatabaseManager databaseManager;

    public SQLWarningRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> addWarning(Warning warning) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(null);
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("INSERT INTO warnings (player_uuid, moderator_uuid, moderator_name, reason, created_at) VALUES (?, ?, ?, ?, ?);") ) {
                stmt.setObject(1, warning.getPlayerUuid());
                stmt.setObject(2, warning.getModeratorUuid());
                stmt.setString(3, warning.getModeratorName());
                stmt.setString(4, warning.getReason());
                stmt.setLong(5, warning.getCreatedAt().toEpochMilli());
                stmt.executeUpdate();
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to add warning: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Warning>> listWarnings(UUID playerUuid) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            List<Warning> list = new ArrayList<>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("SELECT id, player_uuid, moderator_uuid, moderator_name, reason, created_at FROM warnings WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                stmt.setObject(1, playerUuid);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new Warning(rs.getLong("id"), rs.getObject("player_uuid", UUID.class), rs.getObject("moderator_uuid", UUID.class), rs.getString("moderator_name"), rs.getString("reason"), Instant.ofEpochMilli(rs.getLong("created_at"))));
                }
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to list warnings: " + ex.getMessage());
            }
            return list;
        });
    }
}
