package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
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
        try {
            source = DataSourceFactory.createDataSource(config, plugin);
            MigrationManager.migrate(source, plugin);
            isAvailable = true;
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().severe("JDBC driver not found: " + ex.getMessage());
            if (source != null) {
                source.close();
                source = null;
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Unable to initialize/migrate database: " + ex.getMessage());
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

    public SinghamCorePlugin getPlugin() {
        return plugin;
    }

    public boolean isAvailable() {
        return available;
    }

    public CompletableFuture<Void> savePunishmentAsync(Punishment punishment) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO punishments (player_uuid, player_name, punishment_type, moderator, reason, duration, created_at, expires_at, ip_address, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;")) {
                statement.setObject(1, punishment.getPlayerUuid());
                statement.setString(2, punishment.getPlayerName());
                statement.setString(3, punishment.getType().name());
                statement.setString(4, punishment.getModerator());
                statement.setString(5, punishment.getReason());
                statement.setLong(6, punishment.getDuration());
                statement.setLong(7, punishment.getCreatedAt().toEpochMilli());
                if (punishment.getExpiresAt() != null) {
                    statement.setLong(8, punishment.getExpiresAt().toEpochMilli());
                } else {
                    statement.setObject(8, null);
                }
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
                        long created = resultSet.getLong("created_at");
                        Long expiresVal = null;
                        try { expiresVal = resultSet.getObject("expires_at") == null ? null : resultSet.getLong("expires_at"); } catch (Exception ignored) {}
                        punishments.add(new Punishment(
                            resultSet.getLong("id"),
                            resultSet.getObject("player_uuid", java.util.UUID.class),
                            resultSet.getString("player_name"),
                            com.friendsmp.singhamcore.punishments.PunishmentType.valueOf(resultSet.getString("punishment_type")),
                            resultSet.getString("moderator"),
                            resultSet.getString("reason"),
                            resultSet.getLong("duration"),
                            Instant.ofEpochMilli(created),
                            expiresVal == null ? null : Instant.ofEpochMilli(expiresVal),
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
        return AsyncDatabaseExecutor.runAsync(() -> {
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
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO reports (reporter_uuid, reported_uuid, reported_name, reason, created_at, status) VALUES (?, ?, ?, ?, ?, ?);")) {
                statement.setObject(1, report.getReporterUuid());
                statement.setObject(2, report.getReportedUuid());
                statement.setString(3, report.getReportedName());
                statement.setString(4, report.getReason());
                statement.setLong(5, report.getCreatedAt().toEpochMilli());
                statement.setString(6, report.getStatus());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save report: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReportEntry>> loadOpenReportsAsync(int limit, int offset) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var reports = new java.util.ArrayList<com.friendsmp.singhamcore.models.ReportEntry>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT report_id, reporter_uuid, reported_uuid, reported_name, reason, created_at, status, claimed_by, claimed_at, resolved_by, resolved_at FROM reports WHERE status = 'OPEN' ORDER BY created_at ASC LIMIT ? OFFSET ?;")) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    reports.add(new com.friendsmp.singhamcore.models.ReportEntry(
                            resultSet.getLong("report_id"),
                            resultSet.getObject("reporter_uuid", java.util.UUID.class),
                            resultSet.getObject("reported_uuid", java.util.UUID.class),
                            resultSet.getString("reported_name"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at")),
                            resultSet.getString("status"),
                            resultSet.getObject("claimed_by", java.util.UUID.class),
                            resultSet.getObject("claimed_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("claimed_at")),
                            resultSet.getObject("resolved_by", java.util.UUID.class),
                            resultSet.getObject("resolved_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("resolved_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load open reports: " + exception.getMessage());
            }
            return reports;
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReportEntry>> loadOpenReportsAsync() {
        return loadOpenReportsAsync(Integer.MAX_VALUE, 0);
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReportEntry>> loadReportsForPlayerAsync(java.util.UUID reportedUuid, int limit, int offset) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var reports = new java.util.ArrayList<com.friendsmp.singhamcore.models.ReportEntry>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT report_id, reporter_uuid, reported_uuid, reported_name, reason, created_at, status, claimed_by, claimed_at, resolved_by, resolved_at FROM reports WHERE reported_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;")) {
                statement.setObject(1, reportedUuid);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    reports.add(new com.friendsmp.singhamcore.models.ReportEntry(
                            resultSet.getLong("report_id"),
                            resultSet.getObject("reporter_uuid", java.util.UUID.class),
                            resultSet.getObject("reported_uuid", java.util.UUID.class),
                            resultSet.getString("reported_name"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at")),
                            resultSet.getString("status"),
                            resultSet.getObject("claimed_by", java.util.UUID.class),
                            resultSet.getObject("claimed_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("claimed_at")),
                            resultSet.getObject("resolved_by", java.util.UUID.class),
                            resultSet.getObject("resolved_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("resolved_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load reports for player: " + exception.getMessage());
            }
            return reports;
        });
    }

    public CompletableFuture<java.util.Optional<com.friendsmp.singhamcore.models.ReportEntry>> findReportByIdAsync(long reportId) {
        if (!available) {
            return CompletableFuture.completedFuture(java.util.Optional.empty());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT report_id, reporter_uuid, reported_uuid, reported_name, reason, created_at, status, claimed_by, claimed_at, resolved_by, resolved_at FROM reports WHERE report_id = ?;")) {
                statement.setLong(1, reportId);
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return java.util.Optional.of(new com.friendsmp.singhamcore.models.ReportEntry(
                            resultSet.getLong("report_id"),
                            resultSet.getObject("reporter_uuid", java.util.UUID.class),
                            resultSet.getObject("reported_uuid", java.util.UUID.class),
                            resultSet.getString("reported_name"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at")),
                            resultSet.getString("status"),
                            resultSet.getObject("claimed_by", java.util.UUID.class),
                            resultSet.getObject("claimed_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("claimed_at")),
                            resultSet.getObject("resolved_by", java.util.UUID.class),
                            resultSet.getObject("resolved_at") == null ? null : Instant.ofEpochMilli(resultSet.getLong("resolved_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to find report: " + exception.getMessage());
            }
            return java.util.Optional.empty();
        });
    }

    public CompletableFuture<Void> claimReportAsync(long reportId, java.util.UUID staffUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE reports SET status = 'CLAIMED', claimed_by = ?, claimed_at = ? WHERE report_id = ?;")) {
                statement.setObject(1, staffUuid);
                statement.setLong(2, Instant.now().toEpochMilli());
                statement.setLong(3, reportId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to claim report: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> resolveReportAsync(long reportId, java.util.UUID staffUuid) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE reports SET status = 'RESOLVED', resolved_by = ?, resolved_at = ? WHERE report_id = ?;")) {
                statement.setObject(1, staffUuid);
                statement.setLong(2, Instant.now().toEpochMilli());
                statement.setLong(3, reportId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to resolve report: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> saveReportCommentAsync(com.friendsmp.singhamcore.models.ReportComment comment) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO report_comments (report_id, staff_uuid, note, created_at) VALUES (?, ?, ?, ?);")) {
                statement.setLong(1, comment.getReportId());
                statement.setObject(2, comment.getStaffUuid());
                statement.setString(3, comment.getNote());
                statement.setLong(4, comment.getCreatedAt().toEpochMilli());
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save report comment: " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReportComment>> loadReportCommentsAsync(long reportId) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var comments = new java.util.ArrayList<com.friendsmp.singhamcore.models.ReportComment>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT comment_id, report_id, staff_uuid, note, created_at FROM report_comments WHERE report_id = ? ORDER BY created_at ASC;")) {
                statement.setLong(1, reportId);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    comments.add(new com.friendsmp.singhamcore.models.ReportComment(
                            resultSet.getLong("comment_id"),
                            resultSet.getLong("report_id"),
                            resultSet.getObject("staff_uuid", java.util.UUID.class),
                            resultSet.getString("note"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load report comments: " + exception.getMessage());
            }
            return comments;
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.StaffLogEntry>> loadStaffLogsForTargetAsync(UUID targetUuid, int limit, int offset) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var list = new java.util.ArrayList<com.friendsmp.singhamcore.models.StaffLogEntry>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT staff_uuid, action, target_uuid, target_name, reason, created_at FROM staff_logs WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;")) {
                statement.setObject(1, targetUuid);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    long created = resultSet.getLong("created_at");
                    list.add(new com.friendsmp.singhamcore.models.StaffLogEntry(
                            resultSet.getObject("staff_uuid", UUID.class),
                            resultSet.getString("action"),
                            resultSet.getObject("target_uuid", UUID.class),
                            resultSet.getString("target_name"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(created)
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load staff logs for target: " + exception.getMessage());
            }
            return list;
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReputationTransaction>> loadReputationTransactionsAsync(UUID playerUuid, int limit, int offset) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var list = new java.util.ArrayList<com.friendsmp.singhamcore.models.ReputationTransaction>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT tx_id, staff_uuid, player_uuid, delta, reason, created_at FROM reputation_transactions WHERE player_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;")) {
                statement.setObject(1, playerUuid);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    list.add(new com.friendsmp.singhamcore.models.ReputationTransaction(
                            resultSet.getLong("tx_id"),
                            resultSet.getObject("staff_uuid", UUID.class),
                            resultSet.getObject("player_uuid", UUID.class),
                            resultSet.getInt("delta"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load reputation transactions: " + exception.getMessage());
            }
            return list;
        });
    }

    public CompletableFuture<java.util.List<com.friendsmp.singhamcore.models.ReputationRecord>> loadReputationLeaderboardAsync(int limit, int offset) {
        if (!available) {
            return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        }
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var leaderboard = new java.util.ArrayList<com.friendsmp.singhamcore.models.ReputationRecord>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT player_uuid, score, updated_at FROM reputation ORDER BY score DESC, updated_at DESC LIMIT ? OFFSET ?;")) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    leaderboard.add(new com.friendsmp.singhamcore.models.ReputationRecord(
                            resultSet.getObject("player_uuid", UUID.class),
                            resultSet.getInt("score"),
                            Instant.ofEpochMilli(resultSet.getLong("updated_at"))
                    ));
                }
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to load reputation leaderboard: " + exception.getMessage());
            }
            return leaderboard;
        });
    }

    public CompletableFuture<Void> saveReputationTransactionAsync(com.friendsmp.singhamcore.models.ReputationTransaction transaction) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO reputation_transactions (staff_uuid, player_uuid, delta, reason, created_at, cooldown_key) VALUES (?, ?, ?, ?, ?, ?);")) {
                statement.setObject(1, transaction.getStaffUuid());
                statement.setObject(2, transaction.getPlayerUuid());
                statement.setInt(3, transaction.getDelta());
                statement.setString(4, transaction.getReason());
                statement.setLong(5, transaction.getCreatedAt().toEpochMilli());
                statement.setString(6, null);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Failed to save reputation transaction: " + exception.getMessage());
            }
        });
    }

    private String getDatabaseProductName(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase();
    }

    public CompletableFuture<Void> saveStaffLogAsync(com.friendsmp.singhamcore.models.StaffLogEntry entry) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return AsyncDatabaseExecutor.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO staff_logs (staff_uuid, action, target_uuid, target_name, reason, created_at) VALUES (?, ?, ?, ?, ?, ?);")) {
                statement.setObject(1, entry.getStaffUuid());
                statement.setString(2, entry.getAction());
                statement.setObject(3, entry.getTargetUuid());
                statement.setString(4, entry.getTargetName());
                statement.setString(5, entry.getReason());
                statement.setLong(6, entry.getCreatedAt().toEpochMilli());
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
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                String product = getDatabaseProductName(connection);
                PreparedStatement statement;
                if (product.contains("postgres")) {
                    statement = connection.prepareStatement(
                            "INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?) ON CONFLICT (player_uuid) DO UPDATE SET score = reputation.score + EXCLUDED.score, updated_at = EXCLUDED.updated_at RETURNING score;");
                } else if (product.contains("mysql") || product.contains("mariadb")) {
                    statement = connection.prepareStatement(
                            "INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE score = reputation.score + VALUES(score), updated_at = VALUES(updated_at);");
                } else if (product.contains("sqlite")) {
                    statement = connection.prepareStatement(
                            "INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?) ON CONFLICT(player_uuid) DO UPDATE SET score = score + excluded.score, updated_at = excluded.updated_at;");
                } else {
                    statement = connection.prepareStatement("SELECT score FROM reputation WHERE player_uuid = ?;");
                    statement.setObject(1, playerUuid);
                    var resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        int current = resultSet.getInt("score");
                        try (PreparedStatement update = connection.prepareStatement("UPDATE reputation SET score = ?, updated_at = ? WHERE player_uuid = ?;")) {
                            update.setInt(1, current + delta);
                            update.setLong(2, Instant.now().toEpochMilli());
                            update.setObject(3, playerUuid);
                            update.executeUpdate();
                        }
                        return current + delta;
                    }
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO reputation (player_uuid, score, updated_at) VALUES (?, ?, ?);")) {
                        insert.setObject(1, playerUuid);
                        insert.setInt(2, delta);
                        insert.setLong(3, Instant.now().toEpochMilli());
                        insert.executeUpdate();
                    }
                    return delta;
                }
                try (statement) {
                    statement.setObject(1, playerUuid);
                    statement.setInt(2, delta);
                    statement.setLong(3, Instant.now().toEpochMilli());
                    if (product.contains("postgres")) {
                        var resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            return resultSet.getInt("score");
                        }
                    } else {
                        statement.executeUpdate();
                        try (PreparedStatement lookup = connection.prepareStatement("SELECT score FROM reputation WHERE player_uuid = ?;")) {
                            lookup.setObject(1, playerUuid);
                            var resultSet = lookup.executeQuery();
                            if (resultSet.next()) {
                                return resultSet.getInt("score");
                            }
                        }
                    }
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
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT score, updated_at FROM reputation WHERE player_uuid = ?;")) {
                statement.setObject(1, playerUuid);
                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                        long updated = resultSet.getLong("updated_at");
                        return new com.friendsmp.singhamcore.models.ReputationRecord(playerUuid,
                            resultSet.getInt("score"), Instant.ofEpochMilli(updated));
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
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var history = new java.util.ArrayList<com.friendsmp.singhamcore.models.Punishment>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC;")) {
                statement.setObject(1, playerUuid);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                        long created = resultSet.getLong("created_at");
                        Long expiresVal = null;
                        try { expiresVal = resultSet.getObject("expires_at") == null ? null : resultSet.getLong("expires_at"); } catch (Exception ignored) {}
                        history.add(new com.friendsmp.singhamcore.models.Punishment(
                            resultSet.getLong("id"),
                            resultSet.getObject("player_uuid", UUID.class),
                            resultSet.getString("player_name"),
                            com.friendsmp.singhamcore.punishments.PunishmentType.valueOf(resultSet.getString("punishment_type")),
                            resultSet.getString("moderator"),
                            resultSet.getString("reason"),
                            resultSet.getLong("duration"),
                            Instant.ofEpochMilli(created),
                            expiresVal == null ? null : Instant.ofEpochMilli(expiresVal),
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
        return AsyncDatabaseExecutor.supplyAsync(() -> {
            var list = new java.util.ArrayList<com.friendsmp.singhamcore.models.StaffLogEntry>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT staff_uuid, action, target_uuid, target_name, reason, created_at FROM staff_logs ORDER BY created_at DESC LIMIT ?;")) {
                statement.setInt(1, limit);
                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                        long created = resultSet.getLong("created_at");
                        list.add(new com.friendsmp.singhamcore.models.StaffLogEntry(
                            resultSet.getObject("staff_uuid", UUID.class),
                            resultSet.getString("action"),
                            resultSet.getObject("target_uuid", UUID.class),
                            resultSet.getString("target_name"),
                            resultSet.getString("reason"),
                            Instant.ofEpochMilli(created)
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
