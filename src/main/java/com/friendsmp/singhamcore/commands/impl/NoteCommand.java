package com.friendsmp.singhamcore.commands.impl;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.BaseCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NoteCommand extends BaseCommand {

    private final SinghamCorePlugin plugin;

    public NoteCommand(SinghamCorePlugin plugin) {
        super("note", "singhamcore.command.note", "/note add|remove <player> <note|note_id>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
            return true;
        }

        String action = args[0].toLowerCase();
        var target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
            return true;
        }

        if (action.equals("add")) {
            String note = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            var staffUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            plugin.getDatabaseManager().addStaffNoteAsync(target.getUniqueId(), staffUuid, note)
                    .thenAccept(noteId -> Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(TextUtils.color(
                            plugin.getConfig().getString("messages.prefix")
                                    + plugin.getConfig().getString("messages.note-add-success")
                                    .replace("{player}", target.getName() == null ? args[1] : target.getName())
                                    .replace("{note_id}", String.valueOf(noteId))))));
            return true;
        }

        if (action.equals("remove")) {
            long noteId;
            try {
                noteId = Long.parseLong(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.note-invalid-id")));
                return true;
            }
            plugin.getDatabaseManager().removeStaffNoteAsync(target.getUniqueId(), noteId)
                    .thenAccept(removed -> Bukkit.getScheduler().runTask(plugin, () -> {
                        String key = removed ? "messages.note-remove-success" : "messages.note-remove-failed";
                        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString(key)
                                .replace("{player}", target.getName() == null ? args[1] : target.getName())
                                .replace("{note_id}", String.valueOf(noteId))));
                    }));
            return true;
        }

        sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.invalid-usage")));
        return true;
    }
}
