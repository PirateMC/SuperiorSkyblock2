package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.handlers.StackedBlocksHandler;
import com.bgsoftware.superiorskyblock.raiding.util.BlockVectorUtils;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunkPosition;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.handlers.SystemManager;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
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
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

//TODO Move copy code to IslandCopy class

public final class RaidIsland {
    private final Location location;
    private final CuboidRegion region;
    private boolean flip;
    private final Location teleportOffset;
    private final Island island;

    RaidIsland(Island island, Location location) {
        this.location = location;
        this.island = island;
        region = getIslandRegion(island);
        teleportOffset = getTeleportLocationOffsetFromCenter(island);
    }

    private Location getTeleportLocationOffsetFromCenter(Island island) {
        Location center = island.getCenter(World.Environment.NORMAL);
        Location teleport = island.getTeleportLocation(World.Environment.NORMAL);
        int xOffset = teleport.getBlockX() - center.getBlockX();
        int yOffset = teleport.getBlockY() - center.getBlockY();
        int zOffset = teleport.getBlockZ() - center.getBlockZ();
        return new Location(location.getWorld(), xOffset, yOffset, zOffset);
    }

    private CuboidRegion getIslandRegion(Island island) {
        Location center = island.getCenter(World.Environment.NORMAL);
        CuboidRegion region = CuboidRegion.fromCenter(BlockVectorUtils.fromLocation(center), island.getIslandSize());
        region.setWorld(BukkitAdapter.adapt(center.getWorld()));
        return region;
    }

    void flip(boolean flip) {
        this.flip = flip;
    }

    private BlockArrayClipboard copyIslandBlocks() {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
            forwardExtentCopy.setCopyingEntities(true);
            forwardExtentCopy.setFilterFunction(new RegionMaskingFilter(session, new LiquidMask(session).inverse().tryCombine(new AirMask(session).inverse()), blockVector3 -> true));
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying.");
        }
        return clipboard;
    }

    private Set<Object> getStackedBlocks() {
        Set<Object> stackedBlocks = new HashSet<>();
        SystemManager systemManager = WildStackerAPI.getWildStacker().getSystemManager();
        island.getAllChunks().forEach(chunk -> {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < snapshot.getHighestBlockYAt(x, z); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (systemManager.isStackedBarrel(block)) {
                            stackedBlocks.add(systemManager.getStackedBarrel(block));
                        } else if (systemManager.isStackedSpawner(block)) {
                            stackedBlocks.add(systemManager.getStackedSpawner(block.getLocation()));
                        }
                    }
            stackedBlocks.addAll(SuperiorSkyblockPlugin.getPlugin().getGrid().getStackedBlocks(ChunkPosition.of(chunk)));
        });
        return stackedBlocks;
    }

    private IslandCopy copyIsland() {
        return new IslandCopy(copyIslandBlocks(), getStackedBlocks());
    }

    private void pasteIslandBlocks(BlockArrayClipboard clipboard) {
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(location.getWorld()), -1)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            //TODO Fix flipping functionality
//            if (flip) {
//                AffineTransform affineTransform = new AffineTransform().rotateY(180);
//                AffineTransform affineTranslate = new AffineTransform().translate(-region.getWidth(), 0, -region.getLength());
//                holder.setTransform(affineTransform.combine(affineTranslate));
//                holder.setTransform(affineTransform);
//                SuperiorSkyblockPlugin.raidDebug("Flipping island.");
//            }
            Operation operation = holder
                    .createPaste(session)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(true)
                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting at " + location);
        }
    }

    private Map<Location, Object> getStackedBlockOffsets(Set<Object> stackedBlocks) {
        Map<Location, Object> stackedBlockOffsets = new HashMap<>();
        Location center = island.getCenter(World.Environment.NORMAL);
        stackedBlocks.forEach(stackedBlock -> {
            int xOffset = getLocationOfStackedObject(stackedBlock).getBlockX() - center.getBlockX();
            int yOffset = getLocationOfStackedObject(stackedBlock).getBlockY() - center.getBlockY();
            int zOffset = getLocationOfStackedObject(stackedBlock).getBlockZ() - center.getBlockZ();
            stackedBlockOffsets.put(new Location(location.getWorld(), xOffset, yOffset, zOffset), stackedBlock);
        });
        return stackedBlockOffsets;
    }

    private Location getLocationOfStackedObject(Object stackedBlock) {
        if (stackedBlock instanceof StackedObject) return ((StackedObject<?>) stackedBlock).getLocation();
        else return ((StackedBlocksHandler.StackedBlock) stackedBlock).getBlockPosition().parse();
    }

    private void pasteIslandStackedBlocks(Set<Object> stackedBlocks) {
        Map<Location, Object> stackedBlockOffsets = getStackedBlockOffsets(stackedBlocks);
        SystemManager manager = WildStackerAPI.getWildStacker().getSystemManager();
        stackedBlockOffsets.forEach((offset, stackedBlock) -> {
            Block block = location.clone().add(
                    island.getIslandSize() + offset.getBlockX(),
                    island.getIslandSize() + offset.getBlockY(),
                    island.getIslandSize() + offset.getBlockZ()
            ).getBlock();
            if (stackedBlock instanceof StackedBarrel) {
                manager.getStackedBarrel(block).setStackAmount(((StackedBarrel) stackedBlock).getStackAmount(), true);
            } else if (stackedBlock instanceof StackedSpawner) {
                StackedSpawner spawner = manager.getStackedSpawner(block.getLocation());
                spawner.setStackAmount(((StackedSpawner) stackedBlock).getStackAmount(), true);
                spawner.setLinkedEntity(((StackedSpawner) stackedBlock).getLinkedEntity());
            } else if (stackedBlock instanceof StackedBlocksHandler.StackedBlock) {
                SuperiorSkyblockPlugin.getPlugin().getGrid().setBlockAmount(block, ((StackedBlocksHandler.StackedBlock) stackedBlock).getAmount());
            }
        });
        SuperiorSkyblockPlugin.raidDebug("Finished pasting stacked blocks.");
    }

    private void pasteIsland(IslandCopy copy) {
        pasteIslandBlocks(copy.getClipboard());
        pasteIslandStackedBlocks(copy.getStackedBlocks());
    }

    void copyPaste() {
        IslandCopy copy = copyIsland();
        pasteIsland(copy);
    }

    void restore() {
        for (int x = (int) location.getX(); x < location.getX() + region.getWidth(); x++)
            for (int z = (int) location.getZ(); z < location.getZ() + region.getLength(); z++)
                for (int y = (int) location.getY(); y < location.getY() + region.getHeight(); y++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    if (y <= SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }
        SuperiorSkyblockPlugin.raidDebug("Restored raid island at " + location);
    }

    UUID getOwner() {
        return island.getOwner().getUniqueId();
    }

    Location getTeleportLocation() {
        Location teleportLocation = location.clone();
        teleportLocation.add(
                island.getIslandSize() + teleportOffset.getBlockX(),
                island.getIslandSize() + teleportOffset.getBlockY(),
                island.getIslandSize() + teleportOffset.getBlockZ()
        );
        SuperiorSkyblockPlugin.raidDebug("Teleported player to " + teleportLocation);
        return teleportLocation;
    }
}
