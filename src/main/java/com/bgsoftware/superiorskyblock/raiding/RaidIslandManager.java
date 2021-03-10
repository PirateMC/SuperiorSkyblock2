package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidIslandManager {

    private Map<UUID, Location> raidIslandLocations = new HashMap<>();
    private int nextRaidLocationX = 0;

    public RaidIslandManager() {

    }

    public void deleteRaidIsland(UUID player) {
        Location raidIslandCenter = raidIslandLocations.get(player);
        World world = raidIslandCenter.getWorld();
        CuboidRegion region = CuboidRegion.fromCenter(BlockVector3.at(raidIslandCenter.getX(), raidIslandCenter.getY(), raidIslandCenter.getZ()), 32);
        for (int x = region.getMinimumPoint().getX(); x < region.getMaximumPoint().getX(); x++)
            for (int z = region.getMinimumPoint().getZ(); z < region.getMaximumPoint().getZ(); z++)
                for (int y = region.getMinimumPoint().getY(); y < region.getMaximumPoint().getY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y <= 197) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }
    }

    public void createRaidIsland(Island island, Location destination) {
        Location islandCenter = island.getCenter(World.Environment.NORMAL);

        CuboidRegion islandRegion = CuboidRegion.fromCenter(BlockVector3.at(islandCenter.getX(), islandCenter.getY(), islandCenter.getZ()), 32);

        SuperiorSkyblockPlugin.raidDebug("Island region center: " + islandRegion.getCenter());
        SuperiorSkyblockPlugin.raidDebug("Island region minimum point: " + islandRegion.getMinimumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region maximum point: " + islandRegion.getMaximumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region area: " + islandRegion.getArea());

        BlockArrayClipboard clipboard = new BlockArrayClipboard(islandRegion);

        // Copy island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(islandCenter.getWorld()), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, islandRegion, clipboard, islandRegion.getMinimumPoint());
            forwardExtentCopy.setCopyingBiomes(true);
            forwardExtentCopy.setCopyingEntities(true);
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying " + island.getName());
        }

        // Paste island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(destination.getWorld()), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(destination.getX() - 32, destination.getY() - 32, destination.getZ() - 32))
                    .ignoreAirBlocks(true)
                    .copyBiomes(true)
                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting " + island.getName());
        }

        raidIslandLocations.put(island.getOwner().getUniqueId(), destination);
    }
}
