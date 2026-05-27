package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final SinghamCorePlugin plugin;
    private final Map<UUID, Boolean> vanishedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> silentVanish = new ConcurrentHashMap<>();

    public VanishManager(SinghamCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(UUID playerUuid) {
        return vanishedPlayers.containsKey(playerUuid);
    }

    public boolean isSilent(UUID playerUuid) {
        return silentVanish.getOrDefault(playerUuid, false);
    }

    public boolean toggleVanish(Player player, boolean silentMode) {
        UUID uuid = player.getUniqueId();
        boolean nowVanished = !isVanished(uuid);
        if (nowVanished) {
            vanishedPlayers.put(uuid, true);
            silentVanish.put(uuid, silentMode);
        } else {
            vanishedPlayers.remove(uuid);
            silentVanish.remove(uuid);
        }
        updatePlayerVisibility(player);
        return nowVanished;
    }

    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.remove(uuid) != null) {
            silentVanish.remove(uuid);
            updatePlayerVisibility(player);
        }
    }

    public void refreshVisibilityFor(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            if (isVanished(target.getUniqueId())) {
                if (viewer.hasPermission(plugin.getConfig().getString("vanish.staff-see-permission", "singhamcore.vanish.see"))) {
                    viewer.showPlayer(plugin, target);
                } else {
                    viewer.hidePlayer(plugin, target);
                }
            } else {
                viewer.showPlayer(plugin, target);
            }
        }
    }

    public void updatePlayerVisibility(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            if (isVanished(target.getUniqueId())) {
                if (viewer.hasPermission(plugin.getConfig().getString("vanish.staff-see-permission", "singhamcore.vanish.see"))) {
                    viewer.showPlayer(plugin, target);
                } else {
                    viewer.hidePlayer(plugin, target);
                }
            } else {
                viewer.showPlayer(plugin, target);
            }
        }
    }
}
