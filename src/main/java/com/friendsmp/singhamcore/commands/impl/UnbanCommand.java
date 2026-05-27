package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnbanCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public UnbanCommand(SinghamCorePlugin plugin) {
        super("unban", "singhamcore.command.unban", "/unban <player>");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!plugin.getAuthService().isAuthenticated(p.getUniqueId())) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&cYou must authenticate with /staffauth <pin> before moderating."));
                return true;
            }
        }
        String name = args[0];
        com.friendsmp.singhamcore.utils.PlayerLookupUtil.lookupUuidByNameAsync(name).thenAccept(uuid -> {
            if (uuid == null) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
                return;
            }
            UUID staffUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
            punishmentManager.revokePunishment(staffUuid, uuid).thenRun(() -> {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + "&aPlayer unbanned."));
            });
        });
        return true;
    }
}
