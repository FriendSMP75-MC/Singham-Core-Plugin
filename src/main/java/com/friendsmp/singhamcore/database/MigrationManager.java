package com.friendsmp.singhamcore.database;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public final class MigrationManager {

    private static final Pattern FLYWAY_SQL_NAME = Pattern.compile("V\\d+__.+\\.sql");

    private MigrationManager() {}

    public static void migrate(HikariDataSource dataSource, SinghamCorePlugin plugin) {
        String configuredType = plugin.getConfig().getString("database.type", "sqlite");
        String location = migrationLocation(configuredType);
        String resourcePath = location.substring("classpath:".length());
        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();
        List<String> migrationResources = findMigrationResources(plugin, pluginClassLoader, resourcePath);
        boolean sqlite = isSqlite(configuredType);

        Flyway flyway = Flyway.configure(pluginClassLoader)
                .dataSource(dataSource)
                .locations(location)
                .baselineOnMigrate(true)
                .mixed(sqlite)
                .load();

        if (sqlite) {
            flyway.repair();
        }

        MigrationInfo[] discovered = flyway.info().all();
        plugin.getLogger().info("Flyway migration location: " + location);
        plugin.getLogger().info("Plugin classloader: " + pluginClassLoader.getClass().getName());
        plugin.getLogger().info("Runtime SQL resources visible before Flyway: " + migrationResources.size());
        migrationResources.forEach(resource -> plugin.getLogger().info(" - " + resource));
        if (sqlite) {
            plugin.getLogger().info("SQLite migration mode: Flyway mixed transactional statements enabled.");
        }
        plugin.getLogger().info("Flyway discovered " + discovered.length + " migration(s).");
        if (discovered.length == 0) {
            if (migrationResources.isEmpty()) {
                throw new IllegalStateException("No migration resources found at " + location);
            }
            plugin.getLogger().warning("Flyway discovered zero migrations; using manual SQL migration fallback.");
            runManualMigrations(dataSource, pluginClassLoader, migrationResources);
        } else {
            flyway.migrate();
        }
        validateRequiredTables(dataSource);
    }

    private static String migrationLocation(String configuredType) {
        String type = configuredType == null ? "sqlite" : configuredType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "postgres", "postgresql" -> "classpath:db/migration/postgresql";
            case "mysql", "mariadb" -> "classpath:db/migration/mysql";
            default -> "classpath:db/migration/sqlite";
        };
    }

    private static boolean isSqlite(String configuredType) {
        return configuredType == null || configuredType.equalsIgnoreCase("sqlite");
    }

    private static List<String> findMigrationResources(SinghamCorePlugin plugin, ClassLoader classLoader, String resourcePath) {
        Set<String> resources = new TreeSet<>();
        try {
            Enumeration<URL> roots = classLoader.getResources(resourcePath);
            while (roots.hasMoreElements()) {
                plugin.getLogger().info("Migration resource root visible: " + roots.nextElement());
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Unable to enumerate migration resource roots: " + ex.getMessage());
        }

        collectKnownResource(classLoader, resourcePath, resources);
        collectFromCodeSource(plugin, resourcePath, resources);

        List<String> valid = new ArrayList<>();
        for (String resource : resources) {
            String fileName = resource.substring(resource.lastIndexOf('/') + 1);
            if (FLYWAY_SQL_NAME.matcher(fileName).matches()) {
                valid.add(resource);
            } else {
                plugin.getLogger().warning("Ignoring migration with invalid Flyway filename: " + resource);
            }
        }
        return valid;
    }

    private static void collectKnownResource(ClassLoader classLoader, String resourcePath, Set<String> resources) {
        String initialMigration = resourcePath + "/V1__init.sql";
        if (classLoader.getResource(initialMigration) != null) {
            resources.add(initialMigration);
        }
    }

    private static void collectFromCodeSource(SinghamCorePlugin plugin, String resourcePath, Set<String> resources) {
        URL location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return;
        }
        try {
            File source = new File(location.toURI());
            if (source.isFile()) {
                collectFromJar(source, resourcePath, resources);
            } else if (source.isDirectory()) {
                collectFromDirectory(source.toPath(), resourcePath, resources);
            }
        } catch (IOException | URISyntaxException ex) {
            plugin.getLogger().warning("Unable to scan plugin code source for migrations: " + ex.getMessage());
        }
    }

    private static void collectFromJar(File jarFile, String resourcePath, Set<String> resources) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(java.util.jar.JarEntry::getName)
                    .filter(name -> name.startsWith(resourcePath + "/"))
                    .filter(name -> name.endsWith(".sql"))
                    .forEach(resources::add);
        }
    }

    private static void collectFromDirectory(Path root, String resourcePath, Set<String> resources) throws IOException {
        Path migrationRoot = root.resolve(resourcePath);
        if (!Files.isDirectory(migrationRoot)) {
            return;
        }
        try (var paths = Files.walk(migrationRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> root.relativize(path).toString().replace(File.separatorChar, '/'))
                    .forEach(resources::add);
        }
    }

    private static void runManualMigrations(HikariDataSource dataSource, ClassLoader classLoader, List<String> resources) {
        try (Connection connection = dataSource.getConnection()) {
            ensureManualHistoryTable(connection);
            for (String resource : resources) {
                String script = resource.substring(resource.lastIndexOf('/') + 1);
                String version = script.substring(1, script.indexOf("__"));
                if (manualMigrationApplied(connection, version)) {
                    continue;
                }
                String sql = readResource(classLoader, resource);
                for (String statement : splitStatements(sql)) {
                    if (!statement.isBlank()) {
                        try (Statement sqlStatement = connection.createStatement()) {
                            sqlStatement.execute(statement);
                        }
                    }
                }
                recordManualMigration(connection, version, script, sql.hashCode());
            }
        } catch (IOException | SQLException ex) {
            throw new IllegalStateException("Manual database migration failed", ex);
        }
    }

    private static void ensureManualHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS singham_schema_history (
                        version VARCHAR(50) PRIMARY KEY,
                        script VARCHAR(255) NOT NULL,
                        checksum INTEGER NOT NULL,
                        installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        success BOOLEAN NOT NULL
                    )
                    """);
        }
    }

    private static boolean manualMigrationApplied(Connection connection, String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM singham_schema_history WHERE version = ? AND success = ?")) {
            statement.setString(1, version);
            statement.setBoolean(2, true);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    private static void recordManualMigration(Connection connection, String version, String script, int checksum) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO singham_schema_history (version, script, checksum, success) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, version);
            statement.setString(2, script);
            statement.setInt(3, checksum);
            statement.setBoolean(4, true);
            statement.executeUpdate();
        }
    }

    private static String readResource(ClassLoader classLoader, String resource) throws IOException {
        try (InputStream stream = classLoader.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static void validateRequiredTables(HikariDataSource dataSource) {
        String[] requiredTables = {
                "punishments",
                "reports",
                "report_comments",
                "reputation",
                "reputation_transactions",
                "staff_logs",
                "staff_pins",
                "staff_sessions",
                "warnings",
                "notes"
        };
        try (Connection connection = dataSource.getConnection()) {
            for (String table : requiredTables) {
                if (!tableExists(connection, table)) {
                    throw new IllegalStateException("Required database table was not created by migrations: " + table);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to validate migrated database schema", ex);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String[] candidates = {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)};
        for (String candidate : candidates) {
            try (ResultSet result = metaData.getTables(null, null, candidate, new String[]{"TABLE"})) {
                if (result.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}
