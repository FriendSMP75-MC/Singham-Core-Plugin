package com.friendsmp.singhamcore.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class TextUtils {

    private TextUtils() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String stripColor(String input) {
        return ChatColor.stripColor(color(input));
    }

    public static void sendPaginated(CommandSender sender, String header, List<String> lines, int page, int pageSize) {
        int totalPages = Math.max(1, (int) Math.ceil(lines.size() / (double) pageSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);
        sender.sendMessage(color(header + " &7(Page " + safePage + "/" + totalPages + ")"));
        if (lines.isEmpty()) {
            return;
        }
        int startIndex = (safePage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, lines.size());
        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(color(lines.get(i)));
        }
    }
}
