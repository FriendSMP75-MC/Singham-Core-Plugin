package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.VanishManager;
import com.friendsmp.singhamcore.service.DiscordSRVHook;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final VanishManager vanishManager;
    private final DiscordSRVHook discordHook;

    public VanishCommand(SinghamCorePlugin plugin) {
        super("vanish", "singhamcore.command.vanish", "/vanish [silent]");
        this.plugin = plugin;
        this.vanishManager = plugin.getVanishManager();
        this.discordHook = new DiscordSRVHook(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&cOnly players can use vanish."));
            return true;
        }
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        boolean silent = args.length > 0 && args[0].equalsIgnoreCase("silent");
        boolean nowVanished = vanishManager.toggleVanish(player, silent);
        String message;
        if (nowVanished) {
            message = plugin.getConfig().getString("messages.vanish-enabled", "You are now vanished.");
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + message));
            if (!silent) {
                String quit = plugin.getConfig().getString("messages.vanish-fake-quit", "{player} has left the game.").replace("{player}", player.getName());
                Bukkit.broadcastMessage(TextUtils.color(quit));
                if (plugin.getConfig().getBoolean("vanish.discordsrv-fake-messages", true)) {
                    discordHook.sendMessage(quit);
                }
            }
        } else {
            message = plugin.getConfig().getString("messages.vanish-disabled", "You are no longer vanished.");
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + message));
            if (!silent) {
                String join = plugin.getConfig().getString("messages.vanish-fake-join", "{player} joined the game.").replace("{player}", player.getName());
                Bukkit.broadcastMessage(TextUtils.color(join));
                if (plugin.getConfig().getBoolean("vanish.discordsrv-fake-messages", true)) {
                    discordHook.sendMessage(join);
                }
            }
        }
        return true;
    }
}
