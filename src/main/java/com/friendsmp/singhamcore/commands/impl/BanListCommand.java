package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.DurationUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BanListCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public BanListCommand(SinghamCorePlugin plugin) {
        super("banlist", "singhamcore.command.banlist", "/banlist [page]");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        } else if (args.length > 1) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        int pageSize = plugin.getConfig().getInt("pagination.page-size", 8);
        List<PunishmentType> types = List.of(PunishmentType.BAN, PunishmentType.TEMPBAN);
        plugin.getDatabaseManager().loadActivePunishmentsByTypesAsync(types)
                .thenAccept(punishments -> Bukkit.getScheduler().runTask(plugin, () -> {
                    ArrayList<String> lines = new ArrayList<>();
                    Instant now = Instant.now();
                    for (Punishment punishment : punishments) {
                        if (punishment.getExpiresAt() != null && punishment.getExpiresAt().isBefore(now)) {
                            plugin.getPunishmentManager().expirePunishment(punishment, "SYSTEM");
                            continue;
                        }
                        String name = punishment.getPlayerName() == null ? "Unknown" : punishment.getPlayerName();
                        String duration = punishment.getType() == PunishmentType.TEMPBAN
                                ? DurationUtils.formatRemaining(punishment.getExpiresAt())
                                : "permanent";
                        lines.add("&7- &f" + name + " &7(&e" + punishment.getType() + "&7, &f" + duration + "&7)");
                    }
                    if (lines.isEmpty()) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-bans")));
                        return;
                    }
                    String header = plugin.getConfig().getString("messages.banlist-header")
                            .replace("{total}", String.valueOf(lines.size()));
                    TextUtils.sendPaginated(sender, plugin.getConfig().getString("messages.prefix") + header, lines, page, pageSize);
                }));
        return true;
    }
}
