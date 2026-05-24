package com.friendsmp.singhamcore.utils;

import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerLookupUtil {
    private PlayerLookupUtil() {}

    public static CompletableFuture<UUID> lookupUuidByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            var offline = Bukkit.getOfflinePlayer(name);
            return offline.getUniqueId();
        });
    }
}
