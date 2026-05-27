package com.friendsmp.singhamcore.commands;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.commands.impl.BanCommand;
import com.friendsmp.singhamcore.commands.impl.ChatLockCommand;
import com.friendsmp.singhamcore.commands.impl.CheckCommand;
import com.friendsmp.singhamcore.commands.impl.HistoryCommand;
import com.friendsmp.singhamcore.commands.impl.IPBanCommand;
import com.friendsmp.singhamcore.commands.impl.KickCommand;
import com.friendsmp.singhamcore.commands.impl.MuteCommand;
import com.friendsmp.singhamcore.commands.impl.NoteCommand;
import com.friendsmp.singhamcore.commands.impl.RepCommand;
import com.friendsmp.singhamcore.commands.impl.ReportCommand;
import com.friendsmp.singhamcore.commands.impl.ReportsCommand;
import com.friendsmp.singhamcore.commands.impl.StaffLogCommand;
import com.friendsmp.singhamcore.commands.impl.TempBanCommand;
import com.friendsmp.singhamcore.commands.impl.TempMuteCommand;
import com.friendsmp.singhamcore.commands.impl.UnbanCommand;
import com.friendsmp.singhamcore.commands.impl.UnmuteCommand;
import com.friendsmp.singhamcore.commands.impl.VanishCommand;
import com.friendsmp.singhamcore.commands.impl.WarnCommand;
import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final SinghamCorePlugin plugin;
    private final Map<String, BaseCommand> commands = new HashMap<>();

    public CommandManager(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        registerCommand(new BanCommand(plugin));
        registerCommand(new TempBanCommand(plugin));
        registerCommand(new UnbanCommand(plugin));
        registerCommand(new IPBanCommand(plugin));
        registerCommand(new MuteCommand(plugin));
        registerCommand(new TempMuteCommand(plugin));
        registerCommand(new UnmuteCommand(plugin));
        registerCommand(new KickCommand(plugin));
        registerCommand(new WarnCommand(plugin));
        registerCommand(new HistoryCommand(plugin));
        registerCommand(new CheckCommand(plugin));
        registerCommand(new ReportCommand(plugin));
        registerCommand(new ReportsCommand(plugin));
        registerCommand(new RepCommand(plugin));
        registerCommand(new NoteCommand(plugin));
        registerCommand(new VanishCommand(plugin));
        registerCommand(new ChatLockCommand(plugin));
        registerCommand(new StaffLogCommand(plugin));
        registerCommand(new com.friendsmp.singhamcore.commands.impl.StaffAuthCommand(plugin, plugin.getAuthService()));
    }

    private void registerCommand(BaseCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public void registerCommands() {
        for (String commandName : commands.keySet()) {
            if (plugin.getCommand(commandName) != null) {
                plugin.getCommand(commandName).setExecutor(this);
                plugin.getCommand(commandName).setTabCompleter(this);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        BaseCommand target = commands.get(command.getName().toLowerCase());
        if (target == null) {
            return false;
        }
        if (!sender.hasPermission(target.getPermission())) {
            sender.sendMessage(TextUtils.color(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
            plugin.getAuditLogger().log("command.unauthorized", "Unauthorized command attempt", java.util.Map.of(
                    "sender", sender.getName(),
                    "command", command.getName(),
                    "label", label));
            return true;
        }
        return target.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ban") || command.getName().equalsIgnoreCase("tempban")) {
            return sender instanceof Player ? plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
        }
        if (command.getName().equalsIgnoreCase("chatlock")) {
            return List.of("on", "off", "status");
        }
        return List.of();
    }
}
