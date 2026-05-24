package com.friendsmp.singhamcore.utils;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {
    private PermissionUtil() {}

    public static boolean has(CommandSender sender, String node) {
        if (sender == null) return false;
        if (sender.hasPermission(node)) return true;
        return sender.isOp();
    }
}
