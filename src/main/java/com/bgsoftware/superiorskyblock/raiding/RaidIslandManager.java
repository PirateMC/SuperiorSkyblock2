package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;

import java.util.UUID;

public final class RaidIslandManager {

    private RaidSlotSet slots = new RaidSlotSet();

    private int lastIslandMaxSize = 0;
    private int nextRaidLocationX = 0;
    private int nextRaidLocationZ = 0;
    private final int raidIslandSpacingX = 1000;
    private final int raidIslandSpacingZ = 0;
    private final int minimumSpacingBetweenIslands = 30;
    private final int raidIslandY = SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL + 3;

    public RaidIslandManager() {
    }

    public void restoreRaidSlot(UUID ownerUuid) {
        slots.getSlotOfIslandOwner(ownerUuid).ifPresent(RaidSlot::restore);
        slots.removeSlotOfOwner(ownerUuid);
    }

    public void restoreAllSlots() {
        slots.forEach(RaidSlot::restore);
        slots.clear();
    }

    public RaidSlot generateNewRaidSlotAsynchronously(Island islandOne, Island islandTwo) {
        SuperiorSkyblockPlugin.raidDebug("Island one size is " + islandOne.getIslandSize());
        SuperiorSkyblockPlugin.raidDebug("Island two size is " + islandTwo.getIslandSize());

        RaidIsland firstRaidIsland = new RaidIsland.RaidIslandBuilder()
                .setSourceIsland(islandOne)
                .setLocation(nextRaidLocationX, raidIslandY, nextRaidLocationZ)
                .setCopyMethod(CopyMethod.CHUNKS)
                .setDirection(Direction.NORTH)
                .build();

        RaidIsland secondRaidIsland = new RaidIsland.RaidIslandBuilder()
                .setSourceIsland(islandTwo)
                .setLocation(nextRaidLocationX, raidIslandY, nextRaidLocationZ + minimumSpacingBetweenIslands + islandOne.getIslandSize() + islandTwo.getIslandSize())
                .setCopyMethod(CopyMethod.CHUNKS)
                .setDirection(Direction.SOUTH)
                .build();

        RaidSlot slot = new RaidSlot(firstRaidIsland, secondRaidIsland);
        slots.add(slot);
        nextRaidLocationX += raidIslandSpacingX + lastIslandMaxSize;
        nextRaidLocationZ += raidIslandSpacingZ;
        lastIslandMaxSize = Integer.max(islandOne.getIslandSize(), islandTwo.getIslandSize());
        return slot;
    }
}