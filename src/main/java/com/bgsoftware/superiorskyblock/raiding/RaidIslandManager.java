package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class RaidIslandManager {

    private RaidSlotSet slots = new RaidSlotSet();

    private int lastIslandMaxSize = 0;
    private int nextRaidLocationX = 0;
    private int nextRaidLocationZ = 0;
    private final int raidIslandSpacingX = 10000;
    private final int raidIslandSpacingZ = 0;
    private final int minimumSpacingBetweenIslands = 30;
    private final int raidIslandY = 200;

    public RaidIslandManager() {
    }

    public void restoreRaidSlot(UUID ownerUuid) {
        slots.getSlotOfIslandOwner(ownerUuid).ifPresent(RaidSlot::restore);
        slots.removeSlotOfOwner(ownerUuid);
    }

    public void restoreAllSlots() {
        slots.forEach(RaidSlot::restore);
    }

    public Pair<Location, Location> setupIslands(Island islandOne, Island islandTwo) {
        World raidWorld = Bukkit.getWorld("RaidWorld");
        Location locationOne = new Location(raidWorld, nextRaidLocationX, raidIslandY, nextRaidLocationZ);
        Location locationTwo = new Location(raidWorld, nextRaidLocationX, raidIslandY, nextRaidLocationZ + minimumSpacingBetweenIslands + islandOne.getIslandSize() + islandTwo.getIslandSize());

        RaidIsland firstRaidIsland = new RaidIsland(islandOne, locationOne);
        firstRaidIsland.flip(false);
        firstRaidIsland.copyPaste();

        RaidIsland secondRaidIsland = new RaidIsland(islandTwo, locationTwo);
        secondRaidIsland.flip(true);
        secondRaidIsland.copyPaste();

        slots.add(new RaidSlot(firstRaidIsland, secondRaidIsland));
        nextRaidLocationX += raidIslandSpacingX + lastIslandMaxSize;
        nextRaidLocationZ += raidIslandSpacingZ;
        lastIslandMaxSize = Integer.max(islandOne.getIslandSize(), islandTwo.getIslandSize());
        return new Pair<>(locationOne, locationTwo);
    }
}
