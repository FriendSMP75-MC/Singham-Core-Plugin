package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (!plugin.ensureStaffAuth(sender)) {
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        var activePunishment = punishmentManager.getActivePunishment(target.getUniqueId());
        if (activePunishment == null || (activePunishment.getType() != PunishmentType.MUTE && activePunishment.getType() != PunishmentType.TEMPMUTE)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-muted")));
            return true;
        }

        punishmentManager.revokePunishment(sender instanceof Player ? ((Player) sender).getUniqueId() : null, target.getUniqueId(), PunishmentType.MUTE, PunishmentType.TEMPMUTE)
                .thenRun(() -> com.friendsmp.singhamcore.utils.BukkitThread.run(plugin, () ->
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.unmute-success").replace("{player}", target.getName())))));
        return true;
    }
}
