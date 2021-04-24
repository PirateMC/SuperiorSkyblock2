package com.bgsoftware.superiorskyblock.raiding.island;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.raiding.exception.NoTeleportLocationException;
import com.bgsoftware.superiorskyblock.raiding.util.BlockVectorUtils;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//TODO Remove current flipping implementation in favor of better one

public final class RaidIsland {
    private final BlockWithDataMap blockWithDataMap;
    private final UUID owner;
    private final int size;
    private final Vector locationVector;
    private final Direction direction;
    private final CopyMethod copyMethod;

    private final World world;
    private final CuboidRegion region;

    private RaidIsland(BlockWithDataMap blockWithDataMap, UUID owner, int size, Vector locationVector, Direction direction, CopyMethod copyMethod) {
        this.blockWithDataMap = blockWithDataMap;
        this.owner = owner;
        this.size = size;
        this.locationVector = locationVector;
        this.direction = direction;
        this.copyMethod = copyMethod;

        world = Bukkit.getWorld(SuperiorSkyblockPlugin.RAID_WORLD_NAME);
        region = CuboidRegion.fromCenter(BlockVectorUtils.fromVector(locationVector), size);
    }

    UUID getOwner() {
        return owner;
    }

    public Location getTeleportLocation() {
        Optional<Map<DataType, Object>> dataOptional = blockWithDataMap
                .values()
                .stream()
                .filter(data -> data.containsKey(DataType.BOOLEAN_TELEPORT_LOCATION))
                .findAny();
        return (Location) dataOptional.orElseThrow(NoTeleportLocationException::new).get(DataType.LOCATION_DESTINATION);
    }

    public void restore() {
        for (int x = region.getMinimumPoint().getBlockX(); x < region.getMaximumPoint().getBlockX(); x++)
            for (int z = region.getMinimumPoint().getBlockZ(); z < region.getMaximumPoint().getBlockZ(); z++)
                for (int y = region.getMinimumPoint().getBlockY(); y < region.getMaximumPoint().getBlockY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y <= SuperiorSkyblockPlugin.RAID_WORLD_WATER_LEVEL) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }
        SuperiorSkyblockPlugin.raidDebug("Restored raid island at " + locationVector);
    }

    public static class RaidIslandBuilder {
        private Island sourceIsland;

        public LocationBuilder setSourceIsland(Island island) {
            sourceIsland = island;
            return new LocationBuilder();
        }

        class LocationBuilder {
            private Vector locationVector;

            public CopyMethodBuilder setLocation(int x, int y, int z) {
                locationVector = new Vector(x, y, z);
                return new CopyMethodBuilder();
            }

            class CopyMethodBuilder {
                private CopyMethod copyMethod;

                public ConfiguredRaidIsland setCopyMethod(CopyMethod copyMethod) {
                    this.copyMethod = copyMethod;
                    return new ConfiguredRaidIsland();
                }

                class ConfiguredRaidIsland {
                    private Direction direction = Direction.NORTH;

                    public ConfiguredRaidIsland setDirection(Direction direction) {
                        this.direction = direction;
                        return this;
                    }

                    public RaidIsland build() {
                        final BlockWithDataMap sourceIslandBlockWithDataMap = new BlockWithDataMap();
                        Bukkit.getScheduler().runTaskAsynchronously(SuperiorSkyblockPlugin.getPlugin(), () -> {
                            SuperiorSkyblockPlugin.raidDebug("Getting island chunks...");
                            List<Chunk> sourceIslandChunks = sourceIsland.getAllChunks(World.Environment.NORMAL, true, true);
                            SuperiorSkyblockPlugin.raidDebug("Done getting island chunks...");

                            SuperiorSkyblockPlugin.raidDebug("Searching for solid blocks...");
                            BlockSearcher.searchForSolidBlocks(sourceIslandChunks, sourceIslandBlockWithDataMap);
                            SuperiorSkyblockPlugin.raidDebug("Done searching for solid blocks.");

                            Location sourceIslandCenter = sourceIsland.getCenter(World.Environment.NORMAL);

                            SuperiorSkyblockPlugin.raidDebug("Attaching teleport data...");
                            sourceIslandBlockWithDataMap.attachTeleportData(sourceIsland.getTeleportLocation(World.Environment.NORMAL));
                            SuperiorSkyblockPlugin.raidDebug("Done attaching teleport data.");

                            SuperiorSkyblockPlugin.raidDebug("Attaching offset data...");
                            sourceIslandBlockWithDataMap.attachOffsetData(sourceIslandCenter.toVector());
                            SuperiorSkyblockPlugin.raidDebug("Done attaching offset data.");

                            SuperiorSkyblockPlugin.raidDebug("Attaching direction data...");
                            sourceIslandBlockWithDataMap.attachDirectionData(direction);
                            SuperiorSkyblockPlugin.raidDebug("Done attaching offset data.");

                            SuperiorSkyblockPlugin.raidDebug("Attaching stacked block data...");
                            sourceIslandBlockWithDataMap.attachStackedBlockDataIfStackedBlock();
                            SuperiorSkyblockPlugin.raidDebug("Done attaching stacked block data.");

                            SuperiorSkyblockPlugin.raidDebug("Copying island to raid location...");
                            sourceIslandBlockWithDataMap.copyToLocation(locationVector.toLocation(Bukkit.getWorld(SuperiorSkyblockPlugin.RAID_WORLD_NAME)));
                            SuperiorSkyblockPlugin.raidDebug("Done copying island to raid location.");
                        });
                        return new RaidIsland(
                                sourceIslandBlockWithDataMap,
                                sourceIsland.getOwner().getUniqueId(),
                                sourceIsland.getIslandSize(),
                                locationVector,
                                direction,
                                copyMethod
                        );
                    }
                }
            }
        }
    }
}


