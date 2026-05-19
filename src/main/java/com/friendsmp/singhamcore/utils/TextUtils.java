package com.friendsmp.singhamcore.utils;

import net.md_5.bungee.api.ChatColor;

public final class TextUtils {

    private TextUtils() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
