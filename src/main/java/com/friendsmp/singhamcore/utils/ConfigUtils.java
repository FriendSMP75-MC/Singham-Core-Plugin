package com.friendsmp.singhamcore.utils;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigUtils {

    private static FileConfiguration config;

    private ConfigUtils() {
    }

    public static void loadConfig(FileConfiguration fileConfiguration) {
        config = fileConfiguration;
    }

    public static String getString(String path) {
        return config.getString(path, "");
    }

    public static int getInt(String path) {
        return config.getInt(path, 0);
    }
}
