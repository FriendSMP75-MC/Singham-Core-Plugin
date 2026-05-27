package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.models.ReportEntry;
import com.friendsmp.singhamcore.models.StaffLogEntry;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ReportCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final ReportManager reportManager;

    public ReportCommand(SinghamCorePlugin plugin) {
        super("report", "singhamcore.command.report", "/report <player> <reason>");
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String mode = args[0].toLowerCase();
        if (mode.equals("claim") || mode.equals("resolve") || mode.equals("comment")) {
            if (!plugin.ensureStaffAuth(sender)) {
                return true;
            }
            if (!(sender instanceof Player) && mode.equals("comment")) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
            long reportId;
            try {
                reportId = Long.parseLong(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
            UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
            if (mode.equals("claim")) {
                reportManager.claimReport(reportId, staffUuid)
                        .thenRun(() -> {
                            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-claim-success").replace("{id}", String.valueOf(reportId))));
                            plugin.getAuditLogger().log("report.claim", "Report claimed", Map.of("staff", sender.getName(), "reportId", String.valueOf(reportId)));
                            plugin.getStaffLogManager().recordAction(new StaffLogEntry(staffUuid, "REPORT_CLAIM", null, null, "Claimed report " + reportId, Instant.now()));
                        });
                return true;
            }
            if (mode.equals("resolve")) {
                reportManager.resolveReport(reportId, staffUuid)
                        .thenRun(() -> {
                            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-resolve-success").replace("{id}", String.valueOf(reportId))));
                            plugin.getAuditLogger().log("report.resolve", "Report resolved", Map.of("staff", sender.getName(), "reportId", String.valueOf(reportId)));
                            plugin.getStaffLogManager().recordAction(new StaffLogEntry(staffUuid, "REPORT_RESOLVE", null, null, "Resolved report " + reportId, Instant.now()));
                        });
                return true;
            }
            if (mode.equals("comment")) {
                if (args.length < 3) {
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                    return true;
                }
                String note = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                reportManager.addComment(reportId, staffUuid, note)
                        .thenRun(() -> {
                            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-comment-success").replace("{id}", String.valueOf(reportId))));
                            plugin.getAuditLogger().log("report.comment", "Comment added to report", Map.of("staff", sender.getName(), "reportId", String.valueOf(reportId)));
                            plugin.getStaffLogManager().recordAction(new StaffLogEntry(staffUuid, "REPORT_COMMENT", null, null, "Commented on report " + reportId + ": " + note, Instant.now()));
                        });
                return true;
            }
        }

        if (!(sender instanceof Player reporter) || args.length < 2) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        ReportEntry report = new ReportEntry(reporter.getUniqueId(), target.getUniqueId(), target.getName(), reason, Instant.now(), "OPEN");
        reportManager.submitReport(report)
                .thenRun(() -> {
                    sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-success")));
                    String notification = plugin.getConfig().getString("messages.report-notification", "&6New report against {target} by {reporter}: {reason}")
                            .replace("{target}", target.getName())
                            .replace("{reporter}", reporter.getName())
                            .replace("{reason}", reason);
                    Bukkit.getOnlinePlayers().stream()
                            .filter(player -> player.hasPermission("singhamcore.command.reports"))
                            .forEach(player -> player.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + notification)));
                    plugin.getAuditLogger().log("report.submit", "Report submitted", Map.of("reporter", reporter.getName(), "reported", target.getName(), "reason", reason));
                });
        return true;
    }
}
