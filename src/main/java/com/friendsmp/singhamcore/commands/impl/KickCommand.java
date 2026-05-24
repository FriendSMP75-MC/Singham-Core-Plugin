package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public KickCommand(SinghamCorePlugin plugin) {
        super("kick", "singhamcore.command.kick", "/kick <player> <reason>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String kickMessage = plugin.getConfig().getString("messages.kick-message")
                .replace("{reason}", reason);
        target.kick(Component.text(TextUtils.color(kickMessage)));
        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.kick-success").replace("{player}", target.getName()).replace("{reason}", reason)));
        return true;
    }
}
