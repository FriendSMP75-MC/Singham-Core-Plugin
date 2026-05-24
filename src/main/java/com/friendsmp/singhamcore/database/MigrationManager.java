package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

public final class MigrationManager {

    private MigrationManager() {}

    public static void migrate(HikariDataSource dataSource, SinghamCorePlugin plugin) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
        } catch (Exception ex) {
            plugin.getLogger().severe("Database migration failed: " + ex.getMessage());
        }
    }
}
