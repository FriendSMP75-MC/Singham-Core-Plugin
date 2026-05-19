package com.friendsmp.singhamcore.commands;

import org.bukkit.command.CommandSender;

public abstract class BaseCommand {

    private final String name;
    private final String permission;
    private final String usage;

    protected BaseCommand(String name, String permission, String usage) {
        this.name = name;
        this.permission = permission;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getUsage() {
        return usage;
    }

    public abstract boolean execute(CommandSender sender, String[] args);
}
