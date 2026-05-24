package com.friendsmp.singhamcore.commands;

import com.friendsmp.singhamcore.utils.MessageFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    protected final FileConfiguration config;

    protected AbstractCommand(FileConfiguration config) {
        this.config = config;
    }

    protected void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(MessageFormatter.format(config.getString(path), config, placeholders));
    }

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
