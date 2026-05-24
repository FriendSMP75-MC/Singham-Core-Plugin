package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.managers.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;

    public VanishListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        vanishManager.hideVanishedFrom(event.getPlayer());
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player && vanishManager.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        vanishManager.handleQuit(event.getPlayer());
    }
}
