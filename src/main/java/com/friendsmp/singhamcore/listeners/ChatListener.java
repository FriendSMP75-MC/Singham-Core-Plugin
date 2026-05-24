package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.DurationUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Instant;

public class ChatListener implements Listener {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;
    private final ChatLockManager chatLockManager;

    public ChatListener(SinghamCorePlugin plugin, PunishmentManager punishmentManager, ChatLockManager chatLockManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.chatLockManager = chatLockManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Punishment mute = punishmentManager.getActivePunishment(player.getUniqueId(), PunishmentType.MUTE);
        if (mute == null) {
            mute = punishmentManager.getActivePunishment(player.getUniqueId(), PunishmentType.TEMPMUTE);
        }
        if (mute != null) {
            if (mute.getExpiresAt() != null && mute.getExpiresAt().isBefore(Instant.now())) {
                punishmentManager.expirePunishment(mute, "SYSTEM");
            } else {
                event.setCancelled(true);
                String remaining = DurationUtils.formatRemaining(mute.getExpiresAt());
                String message = plugin.getConfig().getString("messages.muted")
                        .replace("{duration}", remaining)
                        .replace("{reason}", mute.getReason());
                notifyPlayer(player, message);
                return;
            }
        }

        if (chatLockManager.isLocked() && !player.hasPermission("singhamcore.chat.lock.bypass")) {
            event.setCancelled(true);
            notifyPlayer(player, plugin.getConfig().getString("messages.chatlock-active"));
        }
    }

    private void notifyPlayer(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + message)));
    }
}
