package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CheckCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public CheckCommand(SinghamCorePlugin plugin) {
        super("check", "singhamcore.command.check", "/check <player>");
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

        plugin.getReputationManager().fetchReputation(target.getUniqueId())
                .thenCombine(plugin.getDatabaseManager().loadPunishmentHistoryAsync(target.getUniqueId()), (record, history) -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.check-profile").replace("{player}", target.getName() == null ? args[0] : target.getName()))).append("\n");
                    builder.append(TextUtils.color("&7Reputation: &f" + record.getScore())).append("\n");
                    builder.append(TextUtils.color("&7Total punishments: &f" + history.size()));
                    return builder.toString().split("\n");
                })
                .thenAccept(lines -> Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(lines)));
        return true;
    }
}
