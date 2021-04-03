package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

public final class RaidIsland {
    private UUID owner;
    private World world;
    private BlockVector3 minimumPoint;
    private BlockVector3 maximumPoint;

    RaidIsland(UUID owner, BlockVector3 minimumPoint, BlockVector3 maximumPoint) {
        world = Bukkit.getWorld(SuperiorSkyblockPlugin.RAID_WORLD_NAME);
        this.owner = owner;
        this.minimumPoint = minimumPoint;
        this.maximumPoint = maximumPoint;
    }

    void restore() {
        //TODO Prefer WorldEdit over this method
        for (int x = minimumPoint.getX(); x < maximumPoint.getX(); x++)
            for (int z = minimumPoint.getZ(); z < maximumPoint.getZ(); z++)
                for (int y = minimumPoint.getY(); y < maximumPoint.getY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y <= SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }

        SuperiorSkyblockPlugin.raidDebug("Restored raid island " + minimumPoint + ", " + maximumPoint);
    }

    UUID getOwner() {
        return owner;
    }
}
