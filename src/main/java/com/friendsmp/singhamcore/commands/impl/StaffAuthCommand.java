package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.service.AuthService;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffAuthCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final AuthService authService;

    public StaffAuthCommand(SinghamCorePlugin plugin, AuthService authService) {
        super("staffauth", "singhamcore.command.staffauth", "/staffauth <pin>");
        this.plugin = plugin;
        this.authService = authService;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&cOnly players can authenticate."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }
        Player p = (Player) sender;
        boolean ok = authService.authenticate(p, args[0]);
        if (ok) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&aAuthenticated."));
        } else {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&cInvalid PIN."));
        }
        return true;
    }
}
