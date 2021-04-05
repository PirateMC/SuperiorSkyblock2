package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.boydti.fawe.object.mask.AirMask;
import com.boydti.fawe.object.mask.LiquidMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
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
        if (slots.removeSlotOfOwner(ownerUuid)) {
            SuperiorSkyblockPlugin.raidDebug("Successfully removed raid slot of " + ownerUuid);
        }
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

    public RaidIsland createRaidIsland(Island island, Location destination, boolean flip) {
        Location islandCenter = island.getCenter(World.Environment.NORMAL);
        int islandSize = island.getIslandSize();
        Location pasteLocation = new Location(destination.getWorld(), destination.getX() - islandSize, destination.getY() - islandSize, destination.getZ() - islandSize);

        CuboidRegion islandRegion = CuboidRegion.fromCenter(BlockVector3.at(islandCenter.getX(), islandCenter.getY(), islandCenter.getZ()), islandSize);

        SuperiorSkyblockPlugin.raidDebug("Island region center: " + islandRegion.getCenter());
        SuperiorSkyblockPlugin.raidDebug("Island region minimum point: " + islandRegion.getMinimumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region maximum point: " + islandRegion.getMaximumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region area: " + islandRegion.getArea());

        BlockArrayClipboard clipboard = new BlockArrayClipboard(islandRegion);

        // Copy island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(islandCenter.getWorld()), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, islandRegion, clipboard, islandRegion.getMinimumPoint());
            forwardExtentCopy.setCopyingEntities(true);
            forwardExtentCopy.setFilterFunction(new RegionMaskingFilter(session, new LiquidMask(session).inverse().tryCombine(new AirMask(session).inverse()), blockVector3 -> true));
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying " + island.getName());
        }

        // Paste island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(destination.getWorld()), -1)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            if (flip) {
                AffineTransform affineTransform = new AffineTransform().rotateY(180);
                AffineTransform affineTranslate = new AffineTransform().translate(-islandSize * 2, 0, -islandSize * 2);
                holder.setTransform(affineTransform.combine(affineTranslate));
                SuperiorSkyblockPlugin.raidDebug("Flipping island " + island.getName());
            }
            Operation operation = holder
                    .createPaste(session)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    .ignoreAirBlocks(true)
                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting " + island.getName());

            return new RaidIsland(island, pasteLocation);
        }
    }
}
