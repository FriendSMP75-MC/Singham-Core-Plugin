package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

public class BanCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public BanCommand(SinghamCorePlugin plugin) {
        super("ban", "singhamcore.command.ban", "/ban <player> <reason>");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        if (punishmentManager.isPlayerPunished(target.getUniqueId())) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.already-punished")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String moderator = sender.getName();
        punishmentManager.createPunishment(target.getUniqueId(), target.getName(), PunishmentType.BAN,
                moderator, reason, 0L, null, null, true)
                .thenRun(() -> {
                    String message = TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.ban-success")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason));
                    sender.sendMessage(message);
                    if (target.isOnline()) {
                        Player online = target.getPlayer();
                        if (online != null) {
                            online.kick(TextUtils.color("&cYou have been banned: " + reason));
                        }
                    }
                });
        return true;
    }
}
