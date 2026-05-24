package com.friendsmp.singhamcore.utils;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class MessageFormatter {
    private MessageFormatter() {}

    public static String format(String raw, FileConfiguration config, Map<String, String> placeholders) {
        if (raw == null) return "";
        String out = raw;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        String prefix = config.getString("messages.prefix", "");
        return translateColorCodes(prefix + out);
    }

    private static String translateColorCodes(String input) {
        return input.replace('&', '§');
    }
}
