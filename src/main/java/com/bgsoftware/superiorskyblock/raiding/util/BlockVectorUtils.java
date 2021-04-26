package com.bgsoftware.superiorskyblock.raiding.util;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class BlockVectorUtils {
    public static BlockVector3 fromLocation(Location location) {
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    public static BlockVector3 fromVector(Vector vector) {
        return BlockVector3.at(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }
}
