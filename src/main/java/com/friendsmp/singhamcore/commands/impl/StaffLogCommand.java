package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.models.StaffLogEntry;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.CommandSender;

public class StaffLogCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public StaffLogCommand(SinghamCorePlugin plugin) {
        super("stafflog", "singhamcore.command.stafflog", "/stafflog [limit]");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length == 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        plugin.getDatabaseManager().loadStaffLogsAsync(limit).thenAccept(logs -> {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.stafflog-header")));
            for (StaffLogEntry entry : logs) {
                sender.sendMessage(TextUtils.color("&7[&e" + entry.getAction() + "&7] &f" + entry.getTargetName() + " &7by &f" + entry.getStaffUuid() + " &7- &f" + entry.getReason()));
            }
        });
        return true;
    }
}
