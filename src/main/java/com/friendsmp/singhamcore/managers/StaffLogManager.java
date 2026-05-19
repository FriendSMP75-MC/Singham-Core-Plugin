package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.StaffLogEntry;

import java.util.concurrent.CompletableFuture;

public class StaffLogManager {

    private final DatabaseManager databaseManager;

    public StaffLogManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> recordAction(StaffLogEntry entry) {
        return databaseManager.saveStaffLogAsync(entry);
    }
}
