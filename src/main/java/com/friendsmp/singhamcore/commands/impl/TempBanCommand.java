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
import java.util.concurrent.TimeUnit;

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

        if (punishmentManager.isPlayerPunished(target.getUniqueId())) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.already-punished")));
            return true;
        }

        long durationMillis = parseDuration(args[1]);
        if (durationMillis <= 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        String moderator = sender.getName();
        Instant expiresAt = Instant.now().plusMillis(durationMillis);

        punishmentManager.createPunishment(target.getUniqueId(), target.getName(), PunishmentType.TEMPBAN,
                moderator, reason, durationMillis, expiresAt, null, true)
                .thenRun(() -> {
                    String message = TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.tempban-success")
                            .replace("{player}", target.getName())
                            .replace("{duration}", args[1]));
                    sender.sendMessage(message);
                    if (target.isOnline()) {
                        Player online = target.getPlayer();
                        if (online != null) {
                            online.kick(TextUtils.color("&cYou have been temporarily banned for " + args[1] + ": " + reason));
                        }
                    }
                });
        return true;
    }

    private long parseDuration(String input) {
        if (input.endsWith("d")) {
            return Long.parseLong(input.replace("d", "")) * TimeUnit.DAYS.toMillis(1);
        }
        if (input.endsWith("h")) {
            return Long.parseLong(input.replace("h", "")) * TimeUnit.HOURS.toMillis(1);
        }
        if (input.endsWith("m")) {
            return Long.parseLong(input.replace("m", "")) * TimeUnit.MINUTES.toMillis(1);
        }
        return 0L;
    }
}
