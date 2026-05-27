package com.friendsmp.singhamcore.utils;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageManager {
    private MessageManager() {}

    public static String format(FileConfiguration cfg, String path, Map<String, String> placeholders) {
        String raw = cfg.getString(path);
        return MessageFormatter.format(raw, cfg, placeholders);
    }

    public static String buildBanScreen(FileConfiguration cfg, Map<String, String> placeholders) {
        List<String> lines = cfg.getStringList("ban-screen.lines");
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
        for (String line : lines) {
            String out = line;
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue());
            }
            sb.append(out).append('\n');
        }
        return TextUtils.color(sb.toString());
    }
}
