package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.managers.StaffLogManager;
import com.friendsmp.singhamcore.models.StaffLogEntry;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;

public class NoteCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;
    private final StaffLogManager staffLogManager;

    public NoteCommand(SinghamCorePlugin plugin) {
        super("note", "singhamcore.command.note", "/note <player> <note>");
        this.plugin = plugin;
        this.staffLogManager = plugin.getStaffLogManager();
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

        var target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        String note = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        var staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        StaffLogEntry entry = new StaffLogEntry(staffUuid, "NOTE", target.getUniqueId(), target.getName(), note, Instant.now());
        staffLogManager.recordAction(entry)
                .thenRun(() -> sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.note-success").replace("{player}", target.getName()))));
        return true;
    }
}
