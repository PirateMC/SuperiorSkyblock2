package com.bgsoftware.superiorskyblock.raiding;

public class RaidQueueAddResult {
    private boolean success;
    private int size;

    public RaidQueueAddResult(boolean wasSuccessful, int newSize) {
        success = wasSuccessful;
        size = newSize;
    }

    public int getSize() {
        return size;
    }

    public boolean wasSuccessful() {
        return success;
    }
}
