package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.models.ReportEntry;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;

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
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-success")))));
        return true;
    }
}
