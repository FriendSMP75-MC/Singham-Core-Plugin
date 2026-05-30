package com.friendsmp.singhamcore.utils;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import org.bukkit.Bukkit;

public final class BukkitThread {

    private BukkitThread() {}

    public static void run(SinghamCorePlugin plugin, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
