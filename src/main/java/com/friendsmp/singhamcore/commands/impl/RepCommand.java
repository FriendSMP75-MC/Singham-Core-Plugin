package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.ReputationRecord;
import com.friendsmp.singhamcore.models.ReputationTransaction;
import com.friendsmp.singhamcore.utils.PaginationUtil;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RepCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public RepCommand(SinghamCorePlugin plugin) {
        super("rep", "singhamcore.command.rep", "/rep <player> <amount> [reason] | /rep history <player> [page] | /rep top [page]");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        if (!plugin.ensureStaffAuth(sender)) {
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("history")) {
            if (args.length < 2) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
            var target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
                return true;
            }
            int page = 1;
            if (args.length == 3) {
                try {
                    page = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                    return true;
                }
            }
            final int requestedPage = page;
            plugin.getReputationManager().loadTransactionHistory(target.getUniqueId(), Integer.MAX_VALUE, 0)
                    .thenAccept(entries -> com.friendsmp.singhamcore.utils.BukkitThread.run(plugin, () -> {
                        if (entries.isEmpty()) {
                            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.reputation-history-none").replace("{player}", target.getName())));
                            return;
                        }
                        List<String> lines = new ArrayList<>();
                        for (ReputationTransaction tx : entries) {
                            String staffName = Bukkit.getOfflinePlayer(tx.getStaffUuid()).getName();
                            if (staffName == null || staffName.isEmpty()) {
                                staffName = tx.getStaffUuid().toString();
                            }
                            lines.add("&d[" + (tx.getDelta() >= 0 ? "+" : "") + tx.getDelta() + "] &7by &f" + staffName + " &7» &f" + tx.getReason());
                        }
                        PaginationUtil.sendPaged(sender, lines, requestedPage, 8, TextUtils.color(plugin.getConfig().getString("messages.reputation-history-header").replace("{player}", target.getName())));
                    }));
            return true;
        }

        if (subcommand.equals("top")) {
            int page = 1;
            if (args.length == 2) {
                try {
                    page = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                    return true;
                }
            }
            final int requestedPage = page;
            plugin.getReputationManager().loadLeaderboard(100, 0)
                    .thenAccept(records -> com.friendsmp.singhamcore.utils.BukkitThread.run(plugin, () -> {
                        if (records.isEmpty()) {
                            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.reputation-top-none")));
                            return;
                        }
                        List<String> lines = new ArrayList<>();
                        int rank = 1;
                        for (var record : records) {
                            String playerName = Bukkit.getOfflinePlayer(record.getPlayerUuid()).getName();
                            if (playerName == null || playerName.isEmpty()) {
                                playerName = record.getPlayerUuid().toString();
                            }
                            lines.add("&e#" + rank + " &f" + playerName + " &7» &a" + record.getScore());
                            rank++;
                        }
                        PaginationUtil.sendPaged(sender, lines, requestedPage, 10, plugin.getConfig().getString("messages.reputation-top-header", "&6Top reputation:"));
                    }));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.reputation-self-error")));
            return true;
        }

        int delta;
        try {
            delta = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : plugin.getConfig().getString("messages.reputation-default-reason", "Moderation adjustment");
        UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        plugin.getReputationManager().adjustReputation(target.getUniqueId(), delta, staffUuid, reason)
                .thenAccept(score -> com.friendsmp.singhamcore.utils.BukkitThread.run(plugin, () -> {
                    if (score == Integer.MIN_VALUE) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.reputation-cooldown")));
                        return;
                    }
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rep-success").replace("{player}", target.getName()).replace("{amount}", String.valueOf(delta)).replace("{score}", String.valueOf(score))));
                    plugin.getAuditLogger().log("reputation.adjust", "Reputation changed", java.util.Map.of(
                            "staff", sender.getName(),
                            "target", target.getName(),
                            "delta", String.valueOf(delta),
                            "reason", reason));
                }));
        return true;
    }
}
