package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final SinghamCorePlugin plugin;
    private final HikariDataSource dataSource;
    private final boolean available;

    public DatabaseManager(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        HikariDataSource source = null;
        boolean isAvailable = false;

        FileConfiguration config = plugin.getConfig();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                config.getString("database.host"),
                config.getInt("database.port"),
                config.getString("database.database")));
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMinimumIdle(config.getInt("database.minimumPoolSize"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.maximumPoolSize"));
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setInitializationFailTimeout(0);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            Class.forName("org.postgresql.Driver");
            source = new HikariDataSource(hikariConfig);
            try (Connection connection = source.getConnection()) {
                createTables(connection);
            }
            isAvailable = true;
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().severe("PostgreSQL driver not found: " + exception.getMessage());
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to initialize database connection: " + exception.getMessage());
            if (source != null) {
                source.close();
                source = null;
            }
        }

        if (!isAvailable) {
            plugin.getLogger().warning("Singham Core is starting in degraded mode because PostgreSQL is unavailable. Punishments will be cached locally until the database is restored.");
        }

        this.dataSource = source;
        this.available = isAvailable;
    }

    private void createTables(Connection connection) {
        try {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS players (uuid UUID PRIMARY KEY, name TEXT NOT NULL);")
                    .execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS punishments (id SERIAL PRIMARY KEY, player_uuid UUID NOT NULL, player_name TEXT NOT NULL, punishment_type TEXT NOT NULL, moderator TEXT NOT NULL, reason TEXT NOT NULL, duration BIGINT, created_at TIMESTAMP NOT NULL, expires_at TIMESTAMP, ip_address TEXT, active BOOLEAN NOT NULL);")
                    .execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS reputation (player_uuid UUID PRIMARY KEY, score INTEGER NOT NULL DEFAULT 0, updated_at TIMESTAMP NOT NULL);")
                    .execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS reports (id SERIAL PRIMARY KEY, reporter_uuid UUID NOT NULL, reported_uuid UUID NOT NULL, reported_name TEXT NOT NULL, reason TEXT NOT NULL, created_at TIMESTAMP NOT NULL, status TEXT NOT NULL);")
                    .execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS staff_logs (id SERIAL PRIMARY KEY, staff_uuid UUID NOT NULL, action TEXT NOT NULL, target_uuid UUID, target_name TEXT, reason TEXT, created_at TIMESTAMP NOT NULL);")
                    .execute();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to initialize database tables: " + exception.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (!available || dataSource == null) {
            throw new SQLException("Database is unavailable.");
        }
        return dataSource.getConnection();
    }

    public boolean isAvailable() {
        return available;
    }

    public CompletableFuture<Void> savePunishmentAsync(Punishment punishment) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO punishments (player_uuid, player_name, punishment_type, moderator, reason, duration, created_at, expires_at, ip_address, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;")) {
                statement.setObject(1, punishment.getPlayerUuid());
                statement.setString(2, punishment.getPlayerName());
                statement.setString(3, punishment.getType().name());
                statement.setString(4, punishment.getModerator());
                statement.setString(5, punishment.getReason());
                statement.setLong(6, punishment.getDuration());
                statement.setObject(7, punishment.getCreatedAt());
                statement.setObject(8, punishment.getExpiresAt());
                statement.setString(9, punishment.getIpAddress());
                statement.setBoolean(10, punishment.isActive());
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    punishment.setId(resultSet.getLong("id"));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save punishment: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<List<Punishment>> loadActivePunishmentsAsync() {
        if (!available) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE active = true;")) {
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    punishments.add(new Punishment(
                            resultSet.getLong("id"),
                            resultSet.getObject("player_uuid", java.util.UUID.class),
                            resultSet.getString("player_name"),
                            com.friendsmp.singhamcore.punishments.PunishmentType.valueOf(resultSet.getString("punishment_type")),
                            resultSet.getString("moderator"),
                            resultSet.getString("reason"),
                            resultSet.getLong("duration"),
                            resultSet.getObject("created_at", Instant.class),
                            resultSet.getObject("expires_at", Instant.class),
                            resultSet.getString("ip_address"),
                            resultSet.getBoolean("active")
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load active punishments: " + exception.getMessage());
            }
            return punishments;
        });
    }

    public CompletableFuture<Void> updatePunishmentActiveAsync(long punishmentId, boolean active) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE punishments SET active = ? WHERE id = ?;")) {
                statement.setBoolean(1, active);
                statement.setLong(2, punishmentId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to update punishment active status: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> saveReportAsync(com.friendsmp.singhamcore.models.ReportEntry report) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO reports (reporter_uuid, reported_uuid, reported_name, reason, created_at, status) VALUES (?, ?, ?, ?, ?, ?);") ) {
                statement.setObject(1, report.getReporterUuid());
                statement.setObject(2, report.getReportedUuid());
                statement.setString(3, report.getReportedName());
                statement.setString(4, report.getReason());
                statement.setObject(5, report.getCreatedAt());
                statement.setString(6, report.getStatus());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save report: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> saveStaffLogAsync(com.friendsmp.singhamcore.models.StaffLogEntry entry) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO staff_logs (staff_uuid, action, target_uuid, target_name, reason, created_at) VALUES (?, ?, ?, ?, ?, ?);")) {
                statement.setObject(1, entry.getStaffUuid());
                statement.setString(2, entry.getAction());
                statement.setObject(3, entry.getTargetUuid());
                statement.setString(4, entry.getTargetName());
                statement.setString(5, entry.getReason());
                statement.setObject(6, entry.getCreatedAt());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save staff log: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Integer> adjustReputationAsync(UUID playerUuid, int delta) {
        if (!available) {
            return CompletableFuture.completedFuture(0);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?) ON CONFLICT (player_uuid) DO UPDATE SET score = reputation.score + EXCLUDED.score, updated_at = EXCLUDED.updated_at RETURNING score;")) {
                statement.setObject(1, playerUuid);
                statement.setInt(2, delta);
                statement.setObject(3, Instant.now());
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("score");
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to adjust reputation: " + exception.getMessage());
            }
            return 0;
        });
    }

    public CompletableFuture<com.friendsmp.singhamcore.models.ReputationRecord> getReputationRecordAsync(UUID playerUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid, 0, Instant.now()));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT score, updated_at FROM reputation WHERE player_uuid = ?;")) {
                statement.setObject(1, playerUuid);
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid,
                            resultSet.getInt("score"), resultSet.getObject("updated_at", Instant.class));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to fetch reputation record: " + exception.getMessage());
            }
            return new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid, 0, Instant.now());
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.Punishment>> loadPunishmentHistoryAsync(UUID playerUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            var history = new java.util.ArrayList<com.friendsmp.singhamcore.models.Punishment>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                statement.setObject(1, playerUuid);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    history.add(new com.friendsmp.singhamcore.models.Punishment(
                            resultSet.getLong("id"),
                            resultSet.getObject("player_uuid", UUID.class),
                            resultSet.getString("player_name"),
                            com.friendsmp.singhamcore.punishments.PunishmentType.valueOf(resultSet.getString("punishment_type")),
                            resultSet.getString("moderator"),
                            resultSet.getString("reason"),
                            resultSet.getLong("duration"),
                            resultSet.getObject("created_at", Instant.class),
                            resultSet.getObject("expires_at", Instant.class),
                            resultSet.getString("ip_address"),
                            resultSet.getBoolean("active")
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load punishment history: " + exception.getMessage());
            }
            return history;
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.StaffLogEntry>> loadStaffLogsAsync(int limit) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            var list = new java.util.ArrayList<com.friendsmp.singhamcore.models.StaffLogEntry>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT staff_uuid, action, target_uuid, target_name, reason, created_at FROM staff_logs ORDER BY created_at DESC LIMIT ?;")) {
                statement.setInt(1, limit);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    list.add(new com.friendsmp.singhamcore.models.StaffLogEntry(
                            resultSet.getObject("staff_uuid", UUID.class),
                            resultSet.getString("action"),
                            resultSet.getObject("target_uuid", UUID.class),
                            resultSet.getString("target_name"),
                            resultSet.getString("reason"),
                            resultSet.getObject("created_at", Instant.class)
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load staff logs: " + exception.getMessage());
            }
            return list;
        });
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
