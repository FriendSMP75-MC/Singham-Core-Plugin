package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class RepCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public RepCommand(SinghamCorePlugin plugin) {
        super("rep", "singhamcore.command.rep", "/rep <player> <amount> [reason]");
        this.plugin = plugin;
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

        int delta;
        try {
            delta = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        plugin.getReputationManager().adjustReputation(target.getUniqueId(), delta)
                .thenAccept(score -> sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.rep-success").replace("{player}", target.getName()).replace("{amount}", String.valueOf(delta)).replace("{score}", String.valueOf(score)))));
        return true;
    }
}
