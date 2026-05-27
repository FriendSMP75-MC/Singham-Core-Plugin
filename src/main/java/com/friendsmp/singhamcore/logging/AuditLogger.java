package com.friendsmp.singhamcore.logging;

import com.friendsmp.singhamcore.SinghamCorePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;

public class AuditLogger {

    private final SinghamCorePlugin plugin;
    private final Path file;
    private final WebhookDispatcher webhookDispatcher;

    public AuditLogger(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("audit.log");
        this.webhookDispatcher = new WebhookDispatcher(plugin);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.createFile(file);
        } catch (IOException ignored) {}
    }

    public void log(String category, String message, Map<String, String> meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(Instant.now().toString()).append("] ")
          .append(category).append(" ")
          .append(message);
        if (meta != null && !meta.isEmpty()) {
            sb.append(" | ");
            meta.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
        }
        sb.append(System.lineSeparator());
        String line = sb.toString();
        try {
            Files.write(file, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write audit log: " + e.getMessage());
        }
        if (webhookDispatcher.isEnabled()) {
            webhookDispatcher.dispatch(line.trim());
        }
    }
}
