package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.DurationUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;

public class PlayerConnectionListener implements Listener {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public PlayerConnectionListener(SinghamCorePlugin plugin, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        var playerUuid = event.getPlayer().getUniqueId();
        Punishment ban = Optional.ofNullable(punishmentManager.getActivePunishment(playerUuid, PunishmentType.BAN))
                .orElse(punishmentManager.getActivePunishment(playerUuid, PunishmentType.TEMPBAN));
        if (ban != null) {
            if (ban.getExpiresAt() != null && ban.getExpiresAt().isBefore(java.time.Instant.now())) {
                punishmentManager.expirePunishment(ban, "SYSTEM");
            } else {
                String message = plugin.getConfig().getString("messages.ban-login")
                        .replace("{reason}", ban.getReason())
                        .replace("{duration}", DurationUtils.formatRemaining(ban.getExpiresAt()));
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, TextUtils.color(message));
                return;
            }
        }

        InetAddress address = event.getAddress();
        if (address != null) {
            Punishment ipBan = punishmentManager.getActiveIpBan(address.getHostAddress());
            if (ipBan != null) {
                if (ipBan.getExpiresAt() != null && ipBan.getExpiresAt().isBefore(java.time.Instant.now())) {
                    punishmentManager.expirePunishment(ipBan, "SYSTEM");
                } else {
                    String message = plugin.getConfig().getString("messages.ipban-login")
                            .replace("{reason}", ipBan.getReason());
                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, TextUtils.color(message));
                }
            }
        }
    }
}
