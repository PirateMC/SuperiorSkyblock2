package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
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

    public RaidSlot newRaidSlot(Island islandOne, Island islandTwo) {
        World raidWorld = Bukkit.getWorld(SuperiorSkyblockPlugin.RAID_WORLD_NAME);

        //TODO Implementation
        BlockVector3 locationOne = BlockVector3.at(nextRaidLocationX, raidIslandY, nextRaidLocationZ);
        BlockVector3 locationTwo = BlockVector3.at(nextRaidLocationX, raidIslandY, nextRaidLocationZ + minimumSpacingBetweenIslands + islandOne.getIslandSize() + islandTwo.getIslandSize());

        SuperiorSkyblockPlugin.raidDebug("Island one size is " + islandOne.getIslandSize());
        SuperiorSkyblockPlugin.raidDebug("Island two size is " + islandTwo.getIslandSize());

        RaidIsland firstRaidIsland = new RaidIsland(islandOne, locationOne);
        firstRaidIsland.flip(false);
        firstRaidIsland.copyPaste();

        RaidIsland secondRaidIsland = new RaidIsland(islandTwo, locationTwo);
        secondRaidIsland.flip(true);
        secondRaidIsland.copyPaste();

        RaidSlot slot = new RaidSlot(firstRaidIsland, secondRaidIsland);
        slots.add(slot);
        nextRaidLocationX += raidIslandSpacingX + lastIslandMaxSize;
        nextRaidLocationZ += raidIslandSpacingZ;
        lastIslandMaxSize = Integer.max(islandOne.getIslandSize(), islandTwo.getIslandSize());
        return slot;
    }
}
