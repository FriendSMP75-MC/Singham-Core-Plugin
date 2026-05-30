package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.models.ReportEntry;
import com.friendsmp.singhamcore.utils.PaginationUtil;
import com.friendsmp.singhamcore.utils.TextUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReportsCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final ReportManager reportManager;

    public ReportsCommand(SinghamCorePlugin plugin) {
        super("reports", "singhamcore.command.reports", "/reports [page]");
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!plugin.ensureStaffAuth(sender)) {
            return true;
        }

        int page = 1;
        if (args.length == 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                return true;
            }
        }
        final int requestedPage = page;

        reportManager.loadOpenReports(Integer.MAX_VALUE, 0)
                .thenAccept(reports -> com.friendsmp.singhamcore.utils.BukkitThread.run(plugin, () -> {
                    if (reports.isEmpty()) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.report-no-open")));
                        return;
                    }
                    String header = plugin.getConfig().getString("messages.reports-header", "&6Open reports:");
                    List<ReportEntry> pageReports = PaginationUtil.page(reports, requestedPage, 8);
                    if (pageReports.isEmpty()) {
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
                        return;
                    }
                    sender.sendMessage(TextUtils.color(header));
                    for (ReportEntry report : pageReports) {
                        String reporterName = Bukkit.getOfflinePlayer(report.getReporterUuid()).getName();
                        if (reporterName == null || reporterName.isEmpty()) {
                            reporterName = report.getReporterUuid().toString();
                        }
                        String rawLine = "&e#" + report.getReportId() + " &7[&f" + report.getStatus() + "&7] &f" + report.getReportedName() + " &7reported by &f" + reporterName + " &7» &f" + report.getReason();
                        if (sender instanceof Player player) {
                            TextComponent component = new TextComponent(TextUtils.color(rawLine));
                            TextComponent action = new TextComponent(TextUtils.color(" &7[&aClaim&7]"));
                            action.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report claim " + report.getReportId()));
                            action.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to claim report #" + report.getReportId()).create()));
                            component.addExtra(action);
                            player.spigot().sendMessage(component);
                        } else {
                            sender.sendMessage(TextUtils.color(rawLine + " &7[Use /report claim " + report.getReportId() + "]"));
                        }
                    }
                    sender.sendMessage(TextUtils.color(PaginationUtil.footer(requestedPage, 8, reports.size(), header)));
                }));
        return true;
    }
}
