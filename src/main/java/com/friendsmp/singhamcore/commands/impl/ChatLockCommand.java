package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ChatLockCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public ChatLockCommand(SinghamCorePlugin plugin) {
        super("chatlock", "singhamcore.command.chatlock", "/chatlock");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        boolean locked = plugin.getChatLockManager().toggle();
        String key = locked ? "messages.chatlock-enabled" : "messages.chatlock-disabled";
        String message = TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString(key));
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(message);
            }
        });
        return true;
    }
}
