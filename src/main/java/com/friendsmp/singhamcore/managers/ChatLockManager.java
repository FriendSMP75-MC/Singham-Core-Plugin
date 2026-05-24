package com.friendsmp.singhamcore.managers;

public class ChatLockManager {

    private boolean locked;

    public synchronized boolean isLocked() {
        return locked;
    }

    public synchronized boolean toggle() {
        locked = !locked;
        return locked;
    }
}
