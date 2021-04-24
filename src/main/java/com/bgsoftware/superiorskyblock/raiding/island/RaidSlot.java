package com.bgsoftware.superiorskyblock.raiding.island;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import org.bukkit.Location;

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

    public Location getFirstIslandTeleportLocation() {
        return firstIsland.getTeleportLocation();
    }

    public Location getSecondIslandTeleportLocation() {
        return secondIsland.getTeleportLocation();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RaidSlot))
            return false;
        return ((RaidSlot) obj).firstIsland.getOwner().equals(firstIsland.getOwner())
                && ((RaidSlot) obj).secondIsland.getOwner().equals(secondIsland.getOwner());
    }
}
