package com.bgsoftware.superiorskyblock.raiding.util;

import com.bgsoftware.superiorskyblock.api.objects.Pair;

public class ChunkLocationPair {
    private Pair<Integer, Integer> chunkLocation;
    private Pair<Integer, Integer> blockLocation;

    public ChunkLocationPair(int chunkX, int chunkZ, int locationX, int locationZ) {
        chunkLocation = new Pair<>(chunkX, chunkZ);
        blockLocation = new Pair<>(locationX, locationZ);
    }

    public int getChunkX() {
        return chunkLocation.getKey();
    }

    public int getChunkZ() {
        return chunkLocation.getValue();
    }

    public int getLocationX() {
        return blockLocation.getKey();
    }

    public int getLocationZ() {
        return blockLocation.getValue();
    }
}
