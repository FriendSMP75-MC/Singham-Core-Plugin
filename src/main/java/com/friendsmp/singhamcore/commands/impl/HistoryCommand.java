package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.models.ReportEntry;
import com.friendsmp.singhamcore.models.ReputationTransaction;
import com.friendsmp.singhamcore.models.StaffLogEntry;
import com.friendsmp.singhamcore.utils.PaginationUtil;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HistoryCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public HistoryCommand(SinghamCorePlugin plugin) {
        super("history", "singhamcore.command.history", "/history <player> [page]");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
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

        int page = 1;
        if (args.length == 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
        }

        UUID targetUuid = target.getUniqueId();
        var punishmentsFuture = plugin.getDatabaseManager().loadPunishmentHistoryAsync(targetUuid);
        var reportsFuture = plugin.getReportManager().loadReportsForPlayer(targetUuid, Integer.MAX_VALUE, 0);
        var transactionsFuture = plugin.getReputationManager().loadTransactionHistory(targetUuid, Integer.MAX_VALUE, 0);
        var logsFuture = plugin.getDatabaseManager().loadStaffLogsForTargetAsync(targetUuid, Integer.MAX_VALUE, 0);

        CompletableFuture.allOf(punishmentsFuture, reportsFuture, transactionsFuture, logsFuture)
                .thenRun(() -> {
                    List<Punishment> punishments = punishmentsFuture.join();
                    List<ReportEntry> reports = reportsFuture.join();
                    List<ReputationTransaction> transactions = transactionsFuture.join();
                    List<StaffLogEntry> logs = logsFuture.join();

                    List<Map.Entry<Instant, String>> timeline = new ArrayList<>();
                    String prefix = plugin.getConfig().getString("messages.prefix");

                    for (Punishment punishment : punishments) {
                        String state = punishment.isActive() ? "&aACTIVE" : "&7EXPIRED";
                        String line = "&e[" + punishment.getType() + "] &f" + punishment.getReason() + " &7by &f" + punishment.getModerator() + " &7(" + state + ")";
                        timeline.add(Map.entry(punishment.getCreatedAt(), line));
                    }

                    for (ReportEntry report : reports) {
                        String reporterName = Bukkit.getOfflinePlayer(report.getReporterUuid()).getName();
                        if (reporterName == null || reporterName.isEmpty()) {
                            reporterName = report.getReporterUuid().toString();
                        }
                        String status = report.getStatus();
                        String line = "&b[REPORT] &f" + report.getReportedName() + " &7reported by &f" + reporterName + " &7(" + status + ") &7» &f" + report.getReason();
                        timeline.add(Map.entry(report.getCreatedAt(), line));
                        if (report.getClaimedAt() != null) {
                            String claimName = report.getClaimedBy() == null ? "Unknown" : Bukkit.getOfflinePlayer(report.getClaimedBy()).getName();
                            timeline.add(Map.entry(report.getClaimedAt(), "&7[REPORT] Claimed by &f" + claimName));
                        }
                        if (report.getResolvedAt() != null) {
                            String resolvedName = report.getResolvedBy() == null ? "Unknown" : Bukkit.getOfflinePlayer(report.getResolvedBy()).getName();
                            timeline.add(Map.entry(report.getResolvedAt(), "&7[REPORT] Resolved by &f" + resolvedName));
                        }
                    }

                    for (ReputationTransaction tx : transactions) {
                        String line = "&d[REP] &f" + (tx.getDelta() >= 0 ? "+" : "") + tx.getDelta() + " &7by &f" + Bukkit.getOfflinePlayer(tx.getStaffUuid()).getName() + " &7for &f" + tx.getReason();
                        timeline.add(Map.entry(tx.getCreatedAt(), line));
                    }

                    for (StaffLogEntry log : logs) {
                        String line = "&7[NOTE] &f" + log.getAction() + " &7by &f" + Bukkit.getOfflinePlayer(log.getStaffUuid()).getName() + " &7» &f" + log.getReason();
                        timeline.add(Map.entry(log.getCreatedAt(), line));
                    }

                    if (timeline.isEmpty()) {
                        sender.sendMessage(TextUtils.color(prefix + plugin.getConfig().getString("messages.no-history")));
                        return;
                    }

                    timeline.sort(Comparator.comparing(Map.Entry<Instant, String>::getKey).reversed());
                    List<String> lines = timeline.stream().map(Map.Entry::getValue).collect(Collectors.toList());
                    paginationSend(sender, lines, page, 8, plugin.getConfig().getString("messages.history-header", "&6Moderation timeline for {player}:").replace("{player}", target.getName()));
                });
        return true;
    }

    private void paginationSend(CommandSender sender, List<String> lines, int page, int pageSize, String title) {
        if (sender instanceof org.bukkit.entity.Player) {
            PaginationUtil.sendPaged(sender, lines, page, pageSize, title);
        } else {
            PaginationUtil.sendPaged(sender, lines, page, pageSize, title);
        }
    }
}
