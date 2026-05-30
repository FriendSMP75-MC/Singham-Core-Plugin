package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class SensitiveCommandListener implements Listener {

    private final SinghamCorePlugin plugin;

    public SensitiveCommandListener(SinghamCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("/staffauth ")) {
            return;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length < 3 || (!parts[1].equalsIgnoreCase("setup") && !parts[1].equalsIgnoreCase("login"))) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + " issued server command: /staffauth [PROTECTED]");
        plugin.getCommand("staffauth").execute(player, "staffauth", new String[]{parts[1], parts[2]});
    }
}
