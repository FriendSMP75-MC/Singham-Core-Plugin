package com.friendsmp.singhamcore;

import com.friendsmp.singhamcore.commands.CommandManager;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.managers.ReputationManager;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.managers.StaffLogManager;
import com.friendsmp.singhamcore.utils.ConfigUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class SinghamCorePlugin extends JavaPlugin {

    private static SinghamCorePlugin instance;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private ReputationManager reputationManager;
    private ReportManager reportManager;
    private StaffLogManager staffLogManager;
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

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.PlayerConnectionListener(punishmentManager), this);
    }
}
