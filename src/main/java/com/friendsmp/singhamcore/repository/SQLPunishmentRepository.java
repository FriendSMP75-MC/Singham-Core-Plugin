package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.database.AsyncDatabaseExecutor;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.Punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLPunishmentRepository implements PunishmentRepository {

    private final DatabaseManager databaseManager;

    public SQLPunishmentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> save(Punishment punishment) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(null);
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO punishments (player_uuid, player_name, punishment_type, moderator_uuid, moderator_name, reason, duration, created_at, expires_at, ip_address, active, revoked, server_origin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;")) {
                statement.setObject(1, punishment.getPlayerUuid());
                statement.setString(2, punishment.getPlayerName());
                statement.setString(3, punishment.getType().name());
                statement.setObject(4, null);
                statement.setString(5, punishment.getModerator());
                statement.setString(6, punishment.getReason());
                statement.setLong(7, punishment.getDuration());
                statement.setObject(8, punishment.getCreatedAt());
                statement.setObject(9, punishment.getExpiresAt());
                statement.setString(10, punishment.getIpAddress());
                statement.setBoolean(11, punishment.isActive());
                statement.setBoolean(12, false);
                statement.setString(13, null);
                var rs = statement.executeQuery();
                if (rs.next()) punishment.setId(rs.getLong("id"));
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to save punishment: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> loadActivePunishments() {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE active = true;")) {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to load active punishments: " + ex.getMessage());
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<Optional<Punishment>> findActiveByPlayerUuid(UUID playerUuid) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(Optional.empty());
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? AND active = true ORDER BY created_at DESC LIMIT 1;")) {
                statement.setObject(1, playerUuid);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to query active punishment: " + ex.getMessage());
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> updateActive(long id, boolean active) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(null);
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE punishments SET active = ? WHERE id = ?;")) {
                statement.setBoolean(1, active);
                statement.setLong(2, id);
                statement.executeUpdate();
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to update punishment active: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> loadHistory(UUID playerUuid) {
        if (!databaseManager.isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            List<Punishment> history = new ArrayList<>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                statement.setObject(1, playerUuid);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) history.add(mapRow(rs));
            } catch (SQLException ex) {
                databaseManager.getPlugin().getLogger().severe("Failed to load punishment history: " + ex.getMessage());
            }
            return history;
        });
    }

    private Punishment mapRow(ResultSet rs) throws SQLException {
        return new Punishment(
                rs.getLong("id"),
                rs.getObject("player_uuid", java.util.UUID.class),
                rs.getString("player_name"),
                com.friendsmp.singhamcore.punishments.PunishmentType.valueOf(rs.getString("punishment_type")),
                rs.getString("moderator_name"),
                rs.getString("reason"),
                rs.getLong("duration"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                rs.getObject("expires_at") == null ? null : Instant.ofEpochMilli(rs.getLong("expires_at")),
                rs.getString("ip_address"),
                rs.getBoolean("active")
        );
    }
}
