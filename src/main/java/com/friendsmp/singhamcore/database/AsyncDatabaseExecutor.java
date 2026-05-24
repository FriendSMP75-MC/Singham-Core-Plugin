package com.friendsmp.singhamcore.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AsyncDatabaseExecutor {
    private AsyncDatabaseExecutor() {}

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }
}
