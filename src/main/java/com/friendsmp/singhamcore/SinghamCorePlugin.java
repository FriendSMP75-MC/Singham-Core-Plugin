package com.friendsmp.singhamcore;

import com.friendsmp.singhamcore.commands.CommandManager;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.managers.ReputationManager;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.managers.StaffLogManager;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.managers.VanishManager;
import com.friendsmp.singhamcore.utils.ConfigUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import com.friendsmp.singhamcore.listeners.VanishListener;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SinghamCorePlugin extends JavaPlugin {

    private static SinghamCorePlugin instance;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private ReputationManager reputationManager;
    private ReportManager reportManager;
    private StaffLogManager staffLogManager;
    private VanishManager vanishManager;
    private com.friendsmp.singhamcore.service.AuthService authService;
    private com.friendsmp.singhamcore.logging.AuditLogger auditLogger;
    private CommandManager commandManager;
    private ChatLockManager chatLockManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ConfigUtils.loadConfig(getConfig());

        databaseManager = new DatabaseManager(this);
        authService = new com.friendsmp.singhamcore.service.AuthService(this);
        auditLogger = new com.friendsmp.singhamcore.logging.AuditLogger(this);
        punishmentManager = new PunishmentManager(this, databaseManager);
        reputationManager = new ReputationManager(this, databaseManager);
        reportManager = new ReportManager(this, databaseManager);
        staffLogManager = new StaffLogManager(this, databaseManager);
        vanishManager = new VanishManager(this);
        commandManager = new CommandManager(this);

        punishmentManager.loadActivePunishments();
        // register listeners
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

    public com.friendsmp.singhamcore.service.AuthService getAuthService() {
        return authService;
    }

    public com.friendsmp.singhamcore.logging.AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public ChatLockManager getChatLockManager() {
        return chatLockManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public boolean ensureStaffAuth(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!authService.isAuthenticated(player.getUniqueId())) {
                player.sendMessage(TextUtils.color(getConfig().getString("messages.prefix") + "&cYou must authenticate with /staffauth <pin> before moderating."));
                auditLogger.log("auth.failure", "Staff authentication required", java.util.Map.of(
                        "sender", player.getName(),
                        "uuid", player.getUniqueId().toString()));
                return false;
            }
        }
        return true;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.PlayerConnectionListener(punishmentManager), this);
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.AuthListener(this), this);
        // Chat control (mutes & global lock)
        chatLockManager = new com.friendsmp.singhamcore.managers.ChatLockManager();
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.ChatControlListener(this, chatLockManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager), this);
    }
}
