package com.friendsmp.singhamcore.utils;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PaginationUtil {

    private PaginationUtil() {}

    public static <T> List<T> page(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        int total = items.size();
        int from = Math.max(0, (page - 1) * pageSize);
        if (from >= total) return Collections.emptyList();
        int to = Math.min(total, from + pageSize);
        return new ArrayList<>(items.subList(from, to));
    }

    public static String footer(int page, int pageSize, int total, String title) {
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        return String.format("%s — Page %d/%d — Total: %d", title == null ? "List" : title, page, pages, total);
    }

    public static void sendPaged(CommandSender sender, List<String> lines, int page, int pageSize, String title) {
        int total = lines.size();
        List<String> pageLines = page(lines, page, pageSize);
        sender.sendMessage("\u00A7a" + (title == null ? "List" : title));
        for (String l : pageLines) sender.sendMessage(l);
        sender.sendMessage("\u00A77" + footer(page, pageSize, total, title));
    }
}
