package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class HistoryCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public HistoryCommand(SinghamCorePlugin plugin) {
        super("history", "singhamcore.command.history", "/history <player>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        plugin.getDatabaseManager().loadPunishmentHistoryAsync(target.getUniqueId())
                .thenAccept(history -> {
                    if (history.isEmpty()) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-history")));
                        return;
                    }
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.history-header").replace("{player}", target.getName())));
                    for (Punishment punishment : history) {
                        sender.sendMessage(TextUtils.color("&7- [&e" + punishment.getType() + "&7] &f" + punishment.getModerator() + " &7» &f" + punishment.getReason()));
                    }
                });
        return true;
    }
}
