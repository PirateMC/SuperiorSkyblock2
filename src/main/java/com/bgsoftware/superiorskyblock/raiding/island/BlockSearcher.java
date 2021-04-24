package com.bgsoftware.superiorskyblock.raiding.island;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;
import java.util.stream.Collectors;

class BlockSearcher {

    public static void searchForSolidBlocks(Collection<Chunk> chunks, BlockWithDataMap destination) {
        Material stationaryWater = Material.getMaterial("LEGACY_STATIONARY_WATER");
        ArrayList<Material> ignoredTypes = new ArrayList<>(Arrays.asList(Material.AIR, Material.WATER, Material.BEDROCK));
        if (stationaryWater != null) ignoredTypes.add(stationaryWater);
        int[] chunkCount = {0};
        chunks.forEach(chunk -> {
            SuperiorSkyblockPlugin.raidDebug("Searching chunk " + chunkCount[0]);
            destination.putAll(toBlockWithDataMap(searchChunk(chunk, ignoredTypes)));
            SuperiorSkyblockPlugin.raidDebug("Done searching chunk " + chunkCount[0]++);
        });
    }

    private static Set<Block> searchChunk(Chunk chunk, List<Material> ignoredTypes) {
        ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
        Set<Block> blocks = new HashSet<>();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 30; y < chunkSnapshot.getHighestBlockYAt(x, z); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (ignoredTypes.contains(block.getType())) continue;
                    blocks.add(block);
                }
        return blocks;
    }

    private static Map<Block, Map<DataType, Object>> toBlockWithDataMap(Set<Block> set) {
        if (set.isEmpty()) return Collections.emptyMap();
        return set.stream().collect(Collectors.toMap(key -> key, value -> new HashMap<>()));
    }
}
