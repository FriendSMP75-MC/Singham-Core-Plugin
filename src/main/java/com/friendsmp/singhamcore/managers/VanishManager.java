package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.integrations.DiscordSRVHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final SinghamCorePlugin plugin;
    private final DiscordSRVHook discordSRVHook;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishManager(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        this.discordSRVHook = new DiscordSRVHook(plugin);
    }

    public boolean toggleVanish(Player player) {
        boolean nowVanished = !isVanished(player.getUniqueId());
        setVanished(player, nowVanished);
        return nowVanished;
    }

    public boolean isVanished(UUID playerUuid) {
        return vanished.contains(playerUuid);
    }

    public void setVanished(Player player, boolean vanish) {
        if (vanish) {
            vanished.add(player.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            vanished.remove(player.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    online.showPlayer(plugin, player);
                }
            }
        }
        String messageKey = vanish ? "messages.discord-vanish-quit" : "messages.discord-vanish-join";
        discordSRVHook.sendVanishToggle(player, plugin.getConfig().getString(messageKey));
    }

    public void hideVanishedFrom(Player player) {
        for (UUID vanishedId : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(vanishedId);
            if (vanishedPlayer != null && !vanishedPlayer.getUniqueId().equals(player.getUniqueId())) {
                player.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    public void handleQuit(Player player) {
        vanished.remove(player.getUniqueId());
    }
}
