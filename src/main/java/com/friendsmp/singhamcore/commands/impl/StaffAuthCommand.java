package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.service.AuthService;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffAuthCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final AuthService authService;

    public StaffAuthCommand(SinghamCorePlugin plugin, AuthService authService) {
        super("staffauth", "singhamcore.command.staffauth", "/staffauth setup <pin> | /staffauth login <pin>");
        this.plugin = plugin;
        this.authService = authService;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&cOnly players can authenticate."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String action = args[0].toLowerCase();
        if (action.equals("setup")) {
            return handleSetup(player, args);
        }

        if (action.equals("login")) {
            return handleLogin(player, args);
        }

        if (action.equals("logout")) {
            return handleLogout(player);
        }

        // fallback to direct login with pin
        return handleLogin(player, new String[]{"login", args[0]});
    }

    private boolean handleSetup(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        if (authService.isPinRegistered(player)) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-already-registered")));
            return true;
        }

        String pin = args[1];
        String validation = authService.validatePinFormat(pin);
        if (validation != null) {
            if (validation.equals("length")) {
                player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-pin-length").replace("{min}", String.valueOf(plugin.getConfig().getInt("staff-auth.min-length", 4))).replace("{max}", String.valueOf(plugin.getConfig().getInt("staff-auth.max-length", 12)))));
            } else if (validation.equals("numeric")) {
                player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-pin-numeric")));
            } else {
                player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-invalid-pin-format")));
            }
            return true;
        }

        if (authService.setupPin(player, pin)) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-setup-success")));
        } else {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-setup-failed")));
        }
        return true;
    }

    private boolean handleLogin(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        if (!authService.isPinRegistered(player)) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-no-pin")));
            return true;
        }

        if (authService.isLocked(player.getUniqueId())) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-locked")));
            return true;
        }

        String pin = args[1];
        boolean ok = authService.verifyPin(player, pin);
        if (ok) {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-login-success")));
        } else {
            player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-invalid-pin")));
        }
        return true;
    }

    private boolean handleLogout(Player player) {
        authService.revoke(player.getUniqueId());
        player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.staffauth-logout-success")));
        return true;
    }
}
