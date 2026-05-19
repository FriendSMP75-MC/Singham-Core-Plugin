package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.Optional;

public class PlayerConnectionListener implements Listener {

    private final PunishmentManager punishmentManager;

    public PlayerConnectionListener(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        var activePunishment = Optional.ofNullable(punishmentManager.getActivePunishment(event.getPlayer().getUniqueId()));
        if (activePunishment.isPresent()) {
            Punishment punishment = activePunishment.get();
            if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        TextUtils.color("&cYou are banned: " + punishment.getReason()));
                return;
            }
        }

        InetAddress address = event.getAddress();
        if (address != null) {
            punishmentManager.getActivePunishments().stream()
                    .filter(p -> p.getIpAddress() != null && !p.getIpAddress().isBlank())
                    .filter(p -> p.getType() == PunishmentType.IP_BAN)
                    .filter(p -> address.getHostAddress().equals(p.getIpAddress()))
                    .findFirst()
                    .ifPresent(ipPunishment -> event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                            TextUtils.color("&cYour IP is banned.")));
        }
    }
}
