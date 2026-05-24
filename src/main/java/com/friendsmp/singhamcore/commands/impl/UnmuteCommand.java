package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.EnumSet;

public class UnmuteCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public UnmuteCommand(SinghamCorePlugin plugin) {
        super("unmute", "singhamcore.command.unmute", "/unmute <player>");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        punishmentManager.revokePunishments(target.getUniqueId(), EnumSet.of(PunishmentType.MUTE, PunishmentType.TEMPMUTE), sender.getName())
                .thenAccept(revoked -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String key = revoked ? "messages.unmute-success" : "messages.not-muted";
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString(key)
                            .replace("{player}", target.getName() == null ? args[0] : target.getName())));
                }));
        return true;
    }
}
