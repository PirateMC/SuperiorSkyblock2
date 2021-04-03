package com.bgsoftware.superiorskyblock.raiding.util;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;

public class BlockVectorUtils {
    public static BlockVector3 fromLocation(Location location) {
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }
}
