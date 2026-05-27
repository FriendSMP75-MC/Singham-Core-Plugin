package com.friendsmp.singhamcore.managers;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatLockManager {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    public boolean isLocked() {
        return locked.get();
    }

    public void setLocked(boolean value) {
        locked.set(value);
    }
}
