package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.raiding.util.BlockVectorUtils;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

public final class RaidIsland {
    private UUID owner;
    private Location location;
    private CuboidRegion region;
    private BlockVector3 minimumPoint;
    private BlockVector3 maximumPoint;
    private boolean flip;

    RaidIsland(Island island, Location location) {
        this.owner = island.getOwner().getUniqueId();
        this.location = location;

        region = getIslandRegion(island);
        this.minimumPoint = region.getMinimumPoint();
        this.maximumPoint = region.getMaximumPoint();
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

    private BlockArrayClipboard copyIsland() {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(region.getWorld(), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, region, clipboard, minimumPoint);
            forwardExtentCopy.setCopyingEntities(true);
            forwardExtentCopy.setFilterFunction(new RegionMaskingFilter(session, new LiquidMask(session).inverse().tryCombine(new AirMask(session).inverse()), blockVector3 -> true));
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying.");
        }
        return clipboard;
    }

    private void pasteIsland(BlockArrayClipboard clipboard) {
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(location.getWorld()), -1)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            if (flip) {
                AffineTransform affineTransform = new AffineTransform().rotateY(180);
                AffineTransform affineTranslate = new AffineTransform().translate(-region.getWidth() * 2, 0, -region.getLength() * 2);
                holder.setTransform(affineTransform.combine(affineTranslate));
                SuperiorSkyblockPlugin.raidDebug("Flipping island.");
            }
            Operation operation = holder
                    .createPaste(session)
                    .to(BlockVector3.at(location.getX() - region.getWidth(), location.getY() - region.getHeight(), location.getZ() - region.getLength()))
                    .ignoreAirBlocks(true)
                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting.");
        }
    }

    void copyPaste() {
        BlockArrayClipboard clipboard = copyIsland();
        pasteIsland(clipboard);
    }

    void restore() {
        //TODO Prefer WorldEdit over this method
        for (int x = minimumPoint.getX(); x < maximumPoint.getX(); x++)
            for (int z = minimumPoint.getZ(); z < maximumPoint.getZ(); z++)
                for (int y = minimumPoint.getY(); y < maximumPoint.getY(); y++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);
                    if (y <= SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }

        SuperiorSkyblockPlugin.raidDebug("Restored raid island " + minimumPoint + ", " + maximumPoint);
    }

    UUID getOwner() {
        return owner;
    }
}
