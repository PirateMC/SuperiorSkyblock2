package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;

import java.util.UUID;

public final class RaidSlot {
    private RaidIsland firstIsland;
    private RaidIsland secondIsland;

    RaidSlot(RaidIsland firstIsland, RaidIsland secondIsland) {
        this.firstIsland = firstIsland;
        this.secondIsland = secondIsland;
    }

    void restore() {
        firstIsland.restore();
        secondIsland.restore();
        SuperiorSkyblockPlugin.raidDebug("Restored slot");
    }

    UUID getFirstIslandOwner() {
        return firstIsland.getOwner();
    }

    UUID getSecondIslandOwner() {
        return secondIsland.getOwner();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RaidSlot))
            return false;
        return ((RaidSlot) obj).firstIsland.getOwner().equals(firstIsland.getOwner())
                && ((RaidSlot) obj).secondIsland.getOwner().equals(secondIsland.getOwner());
    }
}
