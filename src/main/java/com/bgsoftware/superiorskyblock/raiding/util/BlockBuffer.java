package com.bgsoftware.superiorskyblock.raiding.util;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public class BlockBuffer {

    public Set<Location> filterOutEmptyBlocks(Chunk chunk) {
        Set<Location> blockLocations = new HashSet<>();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int maxY = snapshot.getHighestBlockYAt(x, z);
                for (int y = 0; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() == Material.AIR) continue;
                    blockLocations.add(block.getLocation());
                }
            }
        return blockLocations;
    }
}
