package com.friendsmp.singhamcore;

import com.friendsmp.singhamcore.commands.CommandManager;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.managers.ReputationManager;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.managers.StaffLogManager;
import com.friendsmp.singhamcore.managers.VanishManager;
import com.friendsmp.singhamcore.utils.ConfigUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class SinghamCorePlugin extends JavaPlugin {

    private static SinghamCorePlugin instance;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private ReputationManager reputationManager;
    private ReportManager reportManager;
    private StaffLogManager staffLogManager;
    private ChatLockManager chatLockManager;
    private VanishManager vanishManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ConfigUtils.loadConfig(getConfig());

        databaseManager = new DatabaseManager(this);
        punishmentManager = new PunishmentManager(this, databaseManager);
        reputationManager = new ReputationManager(this, databaseManager);
        reportManager = new ReportManager(this, databaseManager);
        staffLogManager = new StaffLogManager(this, databaseManager);
        chatLockManager = new ChatLockManager();
        vanishManager = new VanishManager(this);
        commandManager = new CommandManager(this);

        punishmentManager.loadActivePunishments();
        registerListeners();
        commandManager.registerCommands();
        getLogger().info("Singham Core enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("Singham Core disabled.");
    }

    public static SinghamCorePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public StaffLogManager getStaffLogManager() {
        return staffLogManager;
    }

    public ChatLockManager getChatLockManager() {
        return chatLockManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.PlayerConnectionListener(this, punishmentManager), this);
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.ChatListener(this, punishmentManager, chatLockManager), this);
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.VanishListener(vanishManager), this);
    }
}
