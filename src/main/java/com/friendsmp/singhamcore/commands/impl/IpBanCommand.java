package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.net.InetAddress;

public class IpBanCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public IpBanCommand(SinghamCorePlugin plugin) {
        super("ipban", "singhamcore.command.ipban", "/ipban <ip> <reason>");
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String ipAddress;
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            ipAddress = address.getHostAddress();
        } catch (Exception ex) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-ip")));
            return true;
        }

        if (punishmentManager.isIpBanned(ipAddress)) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.already-punished")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        punishmentManager.createPunishment(null, ipAddress, PunishmentType.IP_BAN,
                sender.getName(), reason, 0L, null, ipAddress, true)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(TextUtils.color(
                        plugin.getConfig().getString("messages.prefix")
                                + plugin.getConfig().getString("messages.ipban-success")
                                .replace("{ip}", ipAddress)
                                .replace("{reason}", reason)))));
        return true;
    }
}
