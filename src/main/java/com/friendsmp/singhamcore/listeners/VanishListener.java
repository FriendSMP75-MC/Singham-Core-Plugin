package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.managers.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;

    public VanishListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        vanishManager.refreshVisibilityFor(joiner);
        if (vanishManager.isVanished(joiner.getUniqueId())) {
            event.joinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player.getUniqueId())) {
            event.quitMessage(null);
        }
        vanishManager.remove(player);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        if (!vanishManager.isVanished(target.getUniqueId())) {
            return;
        }
        if (target.hasPermission("singhamcore.vanish.see")) {
            return;
        }
        event.setCancelled(true);
    }
}
