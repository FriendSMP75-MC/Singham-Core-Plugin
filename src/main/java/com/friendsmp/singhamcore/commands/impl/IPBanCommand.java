package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

public class IPBanCommand extends BaseCommand {

    private static final UUID IP_BAN_PLACEHOLDER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final SinghamCorePlugin plugin;
    private final PunishmentManager punishmentManager;

    public IPBanCommand(SinghamCorePlugin plugin) {
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
        if (!plugin.ensureStaffAuth(sender)) {
            return true;
        }

        String ipAddress = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        punishmentManager.createPunishment(IP_BAN_PLACEHOLDER, ipAddress, PunishmentType.IP_BAN,
                sender.getName(), reason, 0L, null, ipAddress, true)
                .thenRun(() -> sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.ipban-success").replace("{ip}", ipAddress).replace("{reason}", reason))));
        return true;
    }
}
