package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatControlListener implements Listener {

    private final SinghamCorePlugin plugin;
    private final ChatLockManager chatLockManager;

    public ChatControlListener(SinghamCorePlugin plugin, ChatLockManager chatLockManager) {
        this.plugin = plugin;
        this.chatLockManager = chatLockManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // Global chat lock enforcement
        if (chatLockManager.isLocked() && !player.hasPermission("singham.chat.lock.bypass")) {
            player.sendMessage(plugin.getConfig().getString("messages.chat-locked", "Chat is currently locked."));
            event.setCancelled(true);
            return;
        }

        // Mute enforcement via PunishmentService
        var pManager = plugin.getPunishmentManager();
        if (pManager.isPlayerPunished(player.getUniqueId())) {
            Punishment punish = pManager.getActivePunishment(player.getUniqueId());
            if (punish != null && (punish.getType() == PunishmentType.MUTE || punish.getType() == PunishmentType.TEMPMUTE)) {
                String msg = plugin.getConfig().getString("messages.muted", "You are muted.");
                player.sendMessage(msg.replace("{reason}", punish.getReason() == null ? "" : punish.getReason()));
                event.setCancelled(true);
            }
        }
    }
}
