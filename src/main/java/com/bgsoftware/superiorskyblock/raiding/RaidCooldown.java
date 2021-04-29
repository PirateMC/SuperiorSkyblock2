package com.bgsoftware.superiorskyblock.raiding;

import java.util.UUID;

public final class RaidCooldown {

    private long duration;
    private UUID islandUuid;

    public RaidCooldown(UUID islandUuid, long duration) {
        this.islandUuid = islandUuid;
        this.duration = duration;
    }

    public void decrement() {
        duration--;
    }

    public long getDuration() {
        return duration;
    }

    public UUID getIslandUuid() {
        return islandUuid;
    }

    public boolean hasExpired() {
        return duration <= 0;
    }
}
