package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static HikariDataSource createDataSource(FileConfiguration config, SinghamCorePlugin plugin) throws ClassNotFoundException {
        String type = config.getString("database.type", "sqlite").toLowerCase();
        HikariConfig hikariConfig = new HikariConfig();

        int minPool = config.getInt("database.minimumPoolSize", 2);
        int maxPool = config.getInt("database.maximumPoolSize", 10);
        hikariConfig.setMinimumIdle(minPool);
        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setInitializationFailTimeout(0);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        switch (type) {
            case "mysql" -> {
                String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC",
                        config.getString("database.host"), config.getInt("database.port", 3306), config.getString("database.database"));
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setUsername(config.getString("database.username"));
                hikariConfig.setPassword(config.getString("database.password"));
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
            case "mariadb" -> {
                String url = String.format("jdbc:mariadb://%s:%d/%s",
                        config.getString("database.host"), config.getInt("database.port", 3306), config.getString("database.database"));
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setUsername(config.getString("database.username"));
                hikariConfig.setPassword(config.getString("database.password"));
                hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
                Class.forName("org.mariadb.jdbc.Driver");
            }
            case "postgresql" -> {
                String url = String.format("jdbc:postgresql://%s:%d/%s",
                        config.getString("database.host"), config.getInt("database.port", 5432), config.getString("database.database"));
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setUsername(config.getString("database.username"));
                hikariConfig.setPassword(config.getString("database.password"));
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                Class.forName("org.postgresql.Driver");
            }
            default -> { // sqlite
                File dbFile = new File(plugin.getDataFolder(), config.getString("database.sqlite-file", "database.db"));
                dbFile.getParentFile().mkdirs();
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                Class.forName("org.sqlite.JDBC");
            }
        }

        return new HikariDataSource(hikariConfig);
    }
}
