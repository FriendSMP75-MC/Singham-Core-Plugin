package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.models.StaffNote;
import com.friendsmp.singhamcore.punishments.PunishmentType;
import com.friendsmp.singhamcore.utils.DurationUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.ArrayList;

public class HistoryCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public HistoryCommand(SinghamCorePlugin plugin) {
        super("history", "singhamcore.command.history", "/history <player>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        int pageSize = plugin.getConfig().getInt("pagination.page-size", 8);

        plugin.getDatabaseManager().loadPunishmentHistoryAsync(target.getUniqueId())
                .thenCombine(plugin.getDatabaseManager().loadStaffNotesAsync(target.getUniqueId()), (history, notes) -> {
                    ArrayList<String> lines = new ArrayList<>();
                    if (!history.isEmpty()) {
                        lines.add("&6Punishments:");
                        Instant now = Instant.now();
                        for (Punishment punishment : history) {
                            if (punishment.isActive() && punishment.getExpiresAt() != null && punishment.getExpiresAt().isBefore(now)) {
                                plugin.getPunishmentManager().expirePunishment(punishment, "SYSTEM");
                            }
                            String status = punishment.isActive() ? "&aActive" : "&7Expired";
                            if (!punishment.isActive() && punishment.getRevokedAt() != null) {
                                status = "&eRevoked";
                            }
                            String duration = punishment.getExpiresAt() == null
                                    ? "permanent"
                                    : DurationUtils.formatRemaining(punishment.getExpiresAt());
                            lines.add("&7- [&e" + punishment.getType() + "&7] &f" + punishment.getReason()
                                    + " &7by &f" + punishment.getModerator()
                                    + " &7(" + status + "&7, &f" + duration + "&7)");
                        }
                    }
                    if (!notes.isEmpty()) {
                        lines.add("&6Staff Notes:");
                        for (StaffNote note : notes) {
                            lines.add("&7- &f#" + note.getId() + " &7» &f" + note.getNote());
                        }
                    }
                    int banCount = (int) history.stream()
                            .filter(punishment -> punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN)
                            .count();
                    return new HistoryPayload(lines, banCount, history.isEmpty() && notes.isEmpty());
                })
                .thenAccept(payload -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (payload.empty()) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-history")));
                        return;
                    }
                    String header = plugin.getConfig().getString("messages.history-header")
                            .replace("{player}", target.getName() == null ? args[0] : target.getName())
                            .replace("{bans}", String.valueOf(payload.banCount()));
                    TextUtils.sendPaginated(sender, plugin.getConfig().getString("messages.prefix") + header, payload.lines(), page, pageSize);
                }));
        return true;
    }

    private record HistoryPayload(ArrayList<String> lines, int banCount, boolean empty) {
    }
}
