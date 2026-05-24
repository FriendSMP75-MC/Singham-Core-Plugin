package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public VanishCommand(SinghamCorePlugin plugin) {
        super("vanish", "singhamcore.command.vanish", "/vanish");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-only")));
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        boolean vanished = plugin.getVanishManager().toggleVanish(player);
        String key = vanished ? "messages.vanish-enabled" : "messages.vanish-disabled";
        player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString(key)));
        return true;
    }
}
