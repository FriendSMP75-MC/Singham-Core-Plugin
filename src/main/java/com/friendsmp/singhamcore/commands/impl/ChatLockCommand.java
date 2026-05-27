package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.CommandSender;

public class ChatLockCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public ChatLockCommand(SinghamCorePlugin plugin) {
        super("chatlock", "singhamcore.command.chatlock", "/chatlock <on|off|status>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!plugin.ensureStaffAuth(sender)) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String action = args[0].toLowerCase();
        var chatLockManager = plugin.getChatLockManager();
        if (action.equals("on") || action.equals("enable")) {
            chatLockManager.setLocked(true);
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.chat-lock-enabled")));
            return true;
        }
        if (action.equals("off") || action.equals("disable")) {
            chatLockManager.setLocked(false);
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.chat-lock-disabled")));
            return true;
        }
        if (action.equals("status")) {
            String status = chatLockManager.isLocked() ? plugin.getConfig().getString("messages.chat-locked") : plugin.getConfig().getString("messages.chat-unlocked");
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + status));
            return true;
        }

        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
        return true;
    }
}
