package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.models.StaffNote;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flywaydb.core.Flyway;

public class DatabaseManager {

    private final SinghamCorePlugin plugin;
    private final HikariDataSource dataSource;
    private final boolean available;
    private final ExecutorService executor;
    private final String databaseType;

    public DatabaseManager(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        this.plugin.getDataFolder().mkdirs();
        HikariDataSource source = null;
        boolean isAvailable = false;

        FileConfiguration config = plugin.getConfig();
        this.databaseType = normalizeDatabaseType(config.getString("database.type", "postgresql"));
        this.executor = Executors.newFixedThreadPool(Math.max(2, config.getInt("database.executor-threads", 4)));

        try {
            HikariConfig hikariConfig = new HikariConfig();
            String jdbcUrl = resolveJdbcUrl(config, databaseType);
            hikariConfig.setJdbcUrl(jdbcUrl);
            if (!databaseType.equals("sqlite")) {
                hikariConfig.setUsername(config.getString("database.username"));
                hikariConfig.setPassword(config.getString("database.password"));
            }
            String driver = driverFor(databaseType);
            if (driver != null) {
                hikariConfig.setDriverClassName(driver);
                Class.forName(driver);
            }
            configurePool(hikariConfig, config);
            hikariConfig.setPoolName("singhamcore-" + databaseType);
            source = new HikariDataSource(hikariConfig);

            migrateDatabase(source, databaseType);

            isAvailable = true;
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().severe("Database driver not found: " + exception.getMessage());
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to initialize/migrate database: " + exception.getMessage());
            if (source != null) {
                source.close();
                source = null;
            }
        }

        if (!isAvailable) {
            plugin.getLogger().warning("Singham Core is starting in degraded mode because the database is unavailable. Punishments will be cached locally until the database is restored.");
        }

        this.dataSource = source;
        this.available = isAvailable;
    }

    // Table creation is handled by Flyway migrations.



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
                        "INSERT INTO punishments (player_uuid, player_name, punishment_type, moderator, reason, duration_ms, created_at, expires_at, ip_address, active, revoked_at, revoked_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)) {
                setUuid(statement, 1, punishment.getPlayerUuid());
                statement.setString(2, punishment.getPlayerName());
                statement.setString(3, punishment.getType().name());
                statement.setString(4, punishment.getModerator());
                statement.setString(5, punishment.getReason());
                statement.setLong(6, punishment.getDuration());
                setInstant(statement, 7, punishment.getCreatedAt());
                setInstant(statement, 8, punishment.getExpiresAt());
                statement.setString(9, punishment.getIpAddress());
                statement.setBoolean(10, punishment.isActive());
                setInstant(statement, 11, punishment.getRevokedAt());
                statement.setString(12, punishment.getRevokedBy());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                   if (keys.next()) {
                       punishment.setId(keys.getLong(1));
                   }
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save punishment: " + exception.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<List<Punishment>> loadActivePunishmentsAsync() {
        if (!available) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE active = true;")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                   punishments.add(new Punishment(
                           resultSet.getLong("punishment_id"),
                           readUuid(resultSet, "player_uuid"),
                           resultSet.getString("player_name"),
                           PunishmentType.valueOf(resultSet.getString("punishment_type")),
                           resultSet.getString("moderator"),
                           resultSet.getString("reason"),
                           resultSet.getLong("duration_ms"),
                           readInstant(resultSet, "created_at"),
                           readInstant(resultSet, "expires_at"),
                           resultSet.getString("ip_address"),
                           resultSet.getBoolean("active"),
                           readInstant(resultSet, "revoked_at"),
                           resultSet.getString("revoked_by")
                   ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load active punishments: " + exception.getMessage());
            }
            return punishments;
        }, executor);
    }

    public CompletableFuture<Void> updatePunishmentActiveAsync(long punishmentId, boolean active, Instant revokedAt, String revokedBy) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE punishments SET active = ?, revoked_at = ?, revoked_by = ? WHERE punishment_id = ?;")) {
                statement.setBoolean(1, active);
                setInstant(statement, 2, revokedAt);
                statement.setString(3, revokedBy);
                statement.setLong(4, punishmentId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to update punishment active status: " + exception.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<Void> saveReportAsync(com.friendsmp.singhamcore.models.ReportEntry report) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO reports (reporter_uuid, reported_uuid, reported_name, reason, created_at, status) VALUES (?, ?, ?, ?, ?, ?);") ) {
                setUuid(statement, 1, report.getReporterUuid());
                setUuid(statement, 2, report.getReportedUuid());
                statement.setString(3, report.getReportedName());
                statement.setString(4, report.getReason());
                setInstant(statement, 5, report.getCreatedAt());
                statement.setString(6, report.getStatus());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save report: " + exception.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<Void> saveStaffLogAsync(com.friendsmp.singhamcore.models.StaffLogEntry entry) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO staff_logs (staff_uuid, action, target_uuid, target_name, reason, created_at) VALUES (?, ?, ?, ?, ?, ?);")) {
                setUuid(statement, 1, entry.getStaffUuid());
                statement.setString(2, entry.getAction());
                setUuid(statement, 3, entry.getTargetUuid());
                statement.setString(4, entry.getTargetName());
                statement.setString(5, entry.getReason());
                setInstant(statement, 6, entry.getCreatedAt());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save staff log: " + exception.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<Integer> adjustReputationAsync(UUID playerUuid, int delta) {
        if (!available) {
            return CompletableFuture.completedFuture(0);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                Instant now = Instant.now();
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE reputation SET score = score + ?, updated_at = ? WHERE player_uuid = ?;")) {
                    update.setInt(1, delta);
                    setInstant(update, 2, now);
                    setUuid(update, 3, playerUuid);
                    int updated = update.executeUpdate();
                    if (updated == 0) {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?);")) {
                            setUuid(insert, 1, playerUuid);
                            insert.setInt(2, delta);
                            setInstant(insert, 3, now);
                            insert.executeUpdate();
                        }
                    }
                }
                int score = 0;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT score FROM reputation WHERE player_uuid = ?;")) {
                    setUuid(select, 1, playerUuid);
                    ResultSet resultSet = select.executeQuery();
                    if (resultSet.next()) {
                        score = resultSet.getInt("score");
                    }
                }
                connection.commit();
                return score;
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to adjust reputation: " + exception.getMessage());
            }
            return 0;
        }, executor);
    }

    public CompletableFuture<com.friendsmp.singhamcore.models.ReputationRecord> getReputationRecordAsync(UUID playerUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid, 0, Instant.now()));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT score, updated_at FROM reputation WHERE player_uuid = ?;")) {
                setUuid(statement, 1, playerUuid);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid,
                           resultSet.getInt("score"), readInstant(resultSet, "updated_at"));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to fetch reputation record: " + exception.getMessage());
            }
            return new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid, 0, Instant.now());
        }, executor);
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.Punishment>> loadPunishmentHistoryAsync(UUID playerUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            var history = new java.util.ArrayList<com.friendsmp.singhamcore.models.Punishment>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                setUuid(statement, 1, playerUuid);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    history.add(new com.friendsmp.singhamcore.models.Punishment(
                           resultSet.getLong("punishment_id"),
                           readUuid(resultSet, "player_uuid"),
                            resultSet.getString("player_name"),
                           PunishmentType.valueOf(resultSet.getString("punishment_type")),
                            resultSet.getString("moderator"),
                            resultSet.getString("reason"),
                           resultSet.getLong("duration_ms"),
                           readInstant(resultSet, "created_at"),
                           readInstant(resultSet, "expires_at"),
                            resultSet.getString("ip_address"),
                           resultSet.getBoolean("active"),
                           readInstant(resultSet, "revoked_at"),
                           resultSet.getString("revoked_by")
                   ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load punishment history: " + exception.getMessage());
            }
            return history;
        }, executor);
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.Punishment>> loadActivePunishmentsByTypesAsync(List<PunishmentType> types) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            var list = new java.util.ArrayList<com.friendsmp.singhamcore.models.Punishment>();
            if (types.isEmpty()) {
                return list;
            }
            String placeholders = String.join(", ", java.util.Collections.nCopies(types.size(), "?"));
            String sql = "SELECT * FROM punishments WHERE active = true AND punishment_type IN (" + placeholders + ") ORDER BY created_at DESC;";
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (PunishmentType type : types) {
                   statement.setString(index++, type.name());
                }
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                   list.add(new com.friendsmp.singhamcore.models.Punishment(
                           resultSet.getLong("punishment_id"),
                           readUuid(resultSet, "player_uuid"),
                           resultSet.getString("player_name"),
                           PunishmentType.valueOf(resultSet.getString("punishment_type")),
                           resultSet.getString("moderator"),
                           resultSet.getString("reason"),
                           resultSet.getLong("duration_ms"),
                           readInstant(resultSet, "created_at"),
                           readInstant(resultSet, "expires_at"),
                           resultSet.getString("ip_address"),
                           resultSet.getBoolean("active"),
                           readInstant(resultSet, "revoked_at"),
                           resultSet.getString("revoked_by")
                   ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load active punishments: " + exception.getMessage());
            }
            return list;
        }, executor);
    }

    public CompletableFuture<java.util.List<StaffNote>> loadStaffNotesAsync(UUID playerUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            var list = new java.util.ArrayList<StaffNote>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                        "SELECT note_id, player_uuid, staff_uuid, note, created_at FROM staff_notes WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                setUuid(statement, 1, playerUuid);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                   list.add(new StaffNote(
                           resultSet.getLong("note_id"),
                           readUuid(resultSet, "player_uuid"),
                           readUuid(resultSet, "staff_uuid"),
                           resultSet.getString("note"),
                           readInstant(resultSet, "created_at")
                   ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load staff notes: " + exception.getMessage());
            }
            return list;
        }, executor);
    }

    public CompletableFuture<Long> addStaffNoteAsync(UUID playerUuid, UUID staffUuid, String note) {
        if (!available) {
            return CompletableFuture.completedFuture(0L);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO staff_notes (player_uuid, staff_uuid, note, created_at) VALUES (?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)) {
                setUuid(statement, 1, playerUuid);
                setUuid(statement, 2, staffUuid);
                statement.setString(3, note);
                setInstant(statement, 4, Instant.now());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                   if (keys.next()) {
                       return keys.getLong(1);
                   }
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to add staff note: " + exception.getMessage());
            }
            return 0L;
        }, executor);
    }

    public CompletableFuture<Boolean> removeStaffNoteAsync(UUID playerUuid, long noteId) {
        if (!available) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM staff_notes WHERE note_id = ? AND player_uuid = ?;")) {
                statement.setLong(1, noteId);
                setUuid(statement, 2, playerUuid);
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to remove staff note: " + exception.getMessage());
            }
            return false;
        }, executor);
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
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    list.add(new com.friendsmp.singhamcore.models.StaffLogEntry(
                           readUuid(resultSet, "staff_uuid"),
                            resultSet.getString("action"),
                           readUuid(resultSet, "target_uuid"),
                            resultSet.getString("target_name"),
                            resultSet.getString("reason"),
                           readInstant(resultSet, "created_at")
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load staff logs: " + exception.getMessage());
            }
            return list;
        }, executor);
    }

    public void shutdown() {
        executor.shutdownNow();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private String normalizeDatabaseType(String type) {
        if (type == null) {
            return "postgresql";
        }
        String normalized = type.toLowerCase();
        return switch (normalized) {
            case "postgres", "postgresql" -> "postgresql";
            case "mysql" -> "mysql";
            case "mariadb" -> "mariadb";
            case "sqlite", "sqlite3" -> "sqlite";
            default -> "postgresql";
        };
    }

    private String resolveJdbcUrl(FileConfiguration config, String databaseType) {
        String override = config.getString("database.jdbcUrl", "");
        if (override != null && !override.isBlank()) {
            return override;
        }
        return switch (databaseType) {
            case "sqlite" -> "jdbc:sqlite:" + plugin.getDataFolder().toPath()
                   .resolve(config.getString("database.sqliteFile", "singham-core.db"))
                   .toAbsolutePath();
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s",
                   config.getString("database.host"),
                   config.getInt("database.port"),
                   config.getString("database.database"));
            case "mariadb" -> String.format("jdbc:mariadb://%s:%d/%s",
                   config.getString("database.host"),
                   config.getInt("database.port"),
                   config.getString("database.database"));
            default -> String.format("jdbc:postgresql://%s:%d/%s",
                   config.getString("database.host"),
                   config.getInt("database.port"),
                   config.getString("database.database"));
        };
    }

    private String driverFor(String databaseType) {
        return switch (databaseType) {
            case "sqlite" -> "org.sqlite.JDBC";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "mariadb" -> "org.mariadb.jdbc.Driver";
            default -> "org.postgresql.Driver";
        };
    }

    private void configurePool(HikariConfig hikariConfig, FileConfiguration config) {
        int minIdle = config.getInt("database.minimumPoolSize", 2);
        int maxPool = config.getInt("database.maximumPoolSize", 10);
        if (databaseType.equals("sqlite")) {
            minIdle = 1;
            maxPool = 1;
        }
        hikariConfig.setMinimumIdle(Math.max(1, minIdle));
        hikariConfig.setMaximumPoolSize(Math.max(1, maxPool));
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setInitializationFailTimeout(0);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }

    private void migrateDatabase(HikariDataSource source, String databaseType) throws Exception {
        String migrationType = switch (databaseType) {
            case "mariadb" -> "mysql";
            default -> databaseType;
        };
        try {
            Flyway flyway = Flyway.configure()
                   .dataSource(source)
                   .locations("classpath:db/migration/" + migrationType)
                   .baselineOnMigrate(true)
                   .load();
            flyway.migrate();
        } catch (Exception exception) {
            if (!databaseType.equals("sqlite")) {
                throw exception;
            }
            applySqlMigration(source, "db/migration/sqlite/V1__init.sql");
        }
    }

    private void applySqlMigration(HikariDataSource source, String resourcePath) throws Exception {
        var resourceStream = plugin.getResource(resourcePath);
        if (resourceStream == null) {
            throw new IllegalStateException("Missing migration resource: " + resourcePath);
        }
        try (Connection connection = source.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            connection.setAutoCommit(false);
            StringBuilder statementBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                   continue;
                }
                statementBuilder.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                   executeSqlStatement(connection, statementBuilder.toString());
                   statementBuilder.setLength(0);
                }
            }
            if (statementBuilder.length() > 0) {
                executeSqlStatement(connection, statementBuilder.toString());
            }
            connection.commit();
        }
    }

    private void executeSqlStatement(Connection connection, String statementText) throws SQLException {
        String sql = statementText.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        if (sql.isBlank()) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void setInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setTimestamp(index, null);
        } else {
            statement.setTimestamp(index, Timestamp.from(instant));
        }
    }

    private Instant readInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void setUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setString(index, null);
        } else {
            statement.setString(index, uuid.toString());
        }
    }

    private UUID readUuid(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
