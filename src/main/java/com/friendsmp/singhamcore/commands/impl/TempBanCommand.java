package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.DurationUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;

public class TempBanCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public TempBanCommand(SinghamCorePlugin plugin) {
        super("tempban", "singhamcore.command.tempban", "/tempban <player> <duration> <reason>");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        if (punishmentManager.hasActivePunishment(target.getUniqueId(), PunishmentType.BAN, PunishmentType.TEMPBAN)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.already-punished")));
            return true;
        }

        long durationMillis = DurationUtils.parseDuration(args[1]);
        if (durationMillis <= 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        String moderator = sender.getName();
        Instant expiresAt = Instant.now().plusMillis(durationMillis);

        punishmentManager.createPunishment(target.getUniqueId(), target.getName(), PunishmentType.TEMPBAN,
                moderator, reason, durationMillis, expiresAt, null, true)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String formattedDuration = DurationUtils.formatDuration(durationMillis);
                    String message = TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.tempban-success")
                            .replace("{player}", target.getName() == null ? args[0] : target.getName())
                            .replace("{duration}", formattedDuration));
                    sender.sendMessage(message);
                    if (target.isOnline()) {
                        Player online = target.getPlayer();
                        if (online != null) {
                            online.kick(Component.text(TextUtils.color(plugin.getConfig().getString("messages.tempban-kick-message")
                                    .replace("{duration}", formattedDuration)
                                    .replace("{reason}", reason))));
                        }
                    }
                }));
        return true;
    }
}
