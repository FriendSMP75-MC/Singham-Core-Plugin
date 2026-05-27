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
            // Auto-expire if needed
            if (punishment.getExpiresAt() != null && punishment.getExpiresAt().isBefore(java.time.Instant.now())) {
                punishmentManager.expirePunishment(punishment);
                return; // allow login
            }
            if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) {
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("type", punishment.getType().name());
                placeholders.put("reason", punishment.getReason());
                placeholders.put("moderator", punishment.getModerator());
                placeholders.put("expires", punishment.getExpiresAt() == null ? "Never" : punishment.getExpiresAt().toString());
                placeholders.put("id", String.valueOf(punishment.getId()));
                String banText = com.friendsmp.singhamcore.utils.MessageManager.buildBanScreen(com.friendsmp.singhamcore.SinghamCorePlugin.getInstance().getConfig(), placeholders);
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banText);
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
