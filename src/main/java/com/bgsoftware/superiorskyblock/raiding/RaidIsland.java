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
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

//TODO Remove current flipping implementation in favor of better one

public final class RaidIsland {
    private final World world;
    private final BlockVector3 location;
    private final Location center;
    private final CuboidRegion region;
    private final Vector teleportOffset;
    private final Island island;
    private boolean flip;

    public RaidIsland(Island island, BlockVector3 location) {
        this.location = location;
        this.island = island;
        world = Bukkit.getWorld(SuperiorSkyblockPlugin.RAID_WORLD_NAME);
        center = new Location(world, location.getBlockX() + island.getIslandSize(), location.getBlockY() +
                island.getIslandSize(), location.getBlockZ() + island.getIslandSize());
        region = getIslandRegion(island);
        teleportOffset = getTeleportLocationOffsetFromCenter(island);
    }

    void copyPaste() {
        IslandCopy copy = copyIsland();
        pasteIsland(copy);
    }

    void flip(boolean flip) {
        this.flip = flip;
    }

    UUID getOwner() {
        return island.getOwner().getUniqueId();
    }

    Location getTeleportLocation() {
        Vector rotatedTeleportOffset = VectorUtils.rotateAroundY(teleportOffset, Math.toRadians(getRotation()));
        Location teleportLocation = center.clone().add(rotatedTeleportOffset.getBlockX() + 1.5, rotatedTeleportOffset.getBlockY(),
                rotatedTeleportOffset.getBlockZ() + 1.5);
        teleportLocation.setYaw(teleportLocation.getYaw() + getRotation());
        SuperiorSkyblockPlugin.raidDebug("Teleported player to " + teleportLocation);
        return teleportLocation;
    }

    void restore() {
        for (int x = location.getX(); x < location.getX() + region.getWidth(); x++)
            for (int z = location.getZ(); z < location.getZ() + region.getLength(); z++)
                for (int y = location.getY(); y < location.getY() + region.getHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y <= SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }
        SuperiorSkyblockPlugin.raidDebug("Restored raid island at " + location);
    }

    private IslandCopy copyIsland() {
        return new IslandCopy(copyIslandBlocks());
    }

    private BlockArrayClipboard copyIslandBlocks() {
        return IslandCopier.copyBlocks(region);
    }

    private CuboidRegion getIslandRegion(Island island) {
        return IslandHelper.getRegionOf(island);
    }

    private Location getLocationOfStackedObject(Object stackedBlock) {
        if (stackedBlock instanceof StackedObject) return ((StackedObject<?>) stackedBlock).getLocation();
        else return ((StackedBlocksHandler.StackedBlock) stackedBlock).getBlockPosition().parse();
    }

    private Set<Object> getStackedBlocks() {
        Set<Object> stackedBlocks = new HashSet<>();
        SystemManager wildStackerSystemManager = WildStackerAPI.getWildStacker().getSystemManager();
        island.getAllChunks().forEach(chunk -> {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < snapshot.getHighestBlockYAt(x, z); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.WATER || block.getType() == Material.AIR) continue;
                        if (wildStackerSystemManager.isStackedBarrel(block)) {
                            stackedBlocks.add(wildStackerSystemManager.getStackedBarrel(block));
                        } else if (wildStackerSystemManager.isStackedSpawner(block)) {
                            stackedBlocks.add(wildStackerSystemManager.getStackedSpawner(block.getLocation()));
                        }
                    }
            stackedBlocks.addAll(SuperiorSkyblockPlugin.getPlugin().getGrid().getStackedBlocks(ChunkPosition.of(chunk)));
        });
        return stackedBlocks;
    }

    private Map<Location, Object> getStackedBlockOffsets(Set<Object> stackedBlocks) {
        Map<Location, Object> stackedBlockOffsets = new HashMap<>();
        Location center = island.getCenter(World.Environment.NORMAL);
        stackedBlocks.forEach(stackedBlock -> {
            Location stackedBlockLocation = getLocationOfStackedObject(stackedBlock).subtract(center);
            Vector stackedBlockVector = VectorUtils.rotateAroundY(stackedBlockLocation.toVector(), getRotation() * Math.PI / 180);
            stackedBlockOffsets.put(stackedBlockVector.toLocation(world).add(1, 0, 1), stackedBlock);
        });
        return stackedBlockOffsets;
    }

    private Vector getTeleportLocationOffsetFromCenter(Island island) {
        Location center = island.getCenter(World.Environment.NORMAL);
        Location teleport = island.getTeleportLocation(World.Environment.NORMAL);
        return teleport.subtract(center).clone().toVector();
    }

    private float getRotation() {
        return flip ? 180 : 0;
    }

    private void pasteIsland(IslandCopy copy) {
        pasteIslandBlocks(copy.getClipboard());
        Bukkit.getScheduler().runTask(SuperiorSkyblockPlugin.getPlugin(), () -> {
            pasteIslandStackedBlocks(getStackedBlocks());
        });
    }

    private void pasteIslandBlocks(BlockArrayClipboard clipboard) {
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            //TODO Fix flipping functionality
            if (flip) {
                AffineTransform rotation = new AffineTransform().rotateY(getRotation());
                AffineTransform translation = new AffineTransform().translate(-region.getWidth(), 0, -region.getLength());
                holder.setTransform(rotation.combine(translation));
            }
            SuperiorSkyblockPlugin.raidDebug("Flipping island.");
            Operation operation = holder
                    .createPaste(session)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(true)
//                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting at " + location);
        }
    }

    private void pasteIslandStackedBlocks(Set<Object> stackedBlocks) {
        Map<Location, Object> stackedBlockOffsets = getStackedBlockOffsets(stackedBlocks);
        SystemManager wildStackerSystemManager = WildStackerAPI.getWildStacker().getSystemManager();
        stackedBlockOffsets.forEach((offset, stackedBlock) -> {
            Block block = world.getBlockAt(
                    location.getBlockX() + island.getIslandSize() + offset.getBlockX(),
                    location.getBlockY() + island.getIslandSize() + offset.getBlockY(),
                    location.getBlockZ() + island.getIslandSize() + offset.getBlockZ()
            );
            if (stackedBlock instanceof StackedBarrel) {
                wildStackerSystemManager.getStackedBarrel(block).setStackAmount(((StackedBarrel) stackedBlock).getStackAmount(), true);
            } else if (stackedBlock instanceof StackedSpawner) {
                StackedSpawner spawner = wildStackerSystemManager.getStackedSpawner(block.getLocation());
                spawner.setStackAmount(((StackedSpawner) stackedBlock).getStackAmount(), true);
                spawner.setLinkedEntity(((StackedSpawner) stackedBlock).getLinkedEntity());
            } else if (stackedBlock instanceof StackedBlocksHandler.StackedBlock) {
                SuperiorSkyblockPlugin.getPlugin().getGrid().setBlockAmount(block, ((StackedBlocksHandler.StackedBlock) stackedBlock).getAmount());
            }
        });
        SuperiorSkyblockPlugin.raidDebug("Finished pasting stacked blocks.");
    }
}

class IslandCopy {
    private final BlockArrayClipboard clipboard;

    IslandCopy(BlockArrayClipboard clipboard) {
        this.clipboard = clipboard;
    }

    BlockArrayClipboard getClipboard() {
        return clipboard;
    }
}

class IslandCopier {
    public static BlockArrayClipboard copyBlocks(CuboidRegion region) {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
//            forwardExtentCopy.setCopyingEntities(true);
            forwardExtentCopy.setFilterFunction(new RegionMaskingFilter(session, new LiquidMask(session).inverse().tryCombine(new AirMask(session).inverse()), blockVector3 -> true));
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying.");
        }
        return clipboard;
    }
}

class IslandHelper {
    public static CuboidRegion getRegionOf(Island island) {
        Location center = island.getCenter(World.Environment.NORMAL);
        CuboidRegion region = CuboidRegion.fromCenter(BlockVectorUtils.fromLocation(center), island.getIslandSize());
        region.setWorld(BukkitAdapter.adapt(center.getWorld()));
        return region;
    }
}

class VectorUtils {
    public static Vector rotateAroundY(Vector vector, double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * vector.getX() + angleSin * vector.getZ();
        double z = -angleSin * vector.getX() + angleCos * vector.getZ();
        return vector.setX(x).setZ(z);
    }
}