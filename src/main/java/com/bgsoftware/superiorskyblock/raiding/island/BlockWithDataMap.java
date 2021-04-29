package com.bgsoftware.superiorskyblock.raiding.island;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.handlers.GridHandler;
import com.bgsoftware.superiorskyblock.handlers.StackedBlocksHandler;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunkPosition;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.handlers.SystemManager;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BlockWithDataMap extends HashMap<Block, Map<DataType, Object>> {

    public void attachDirectionData(Direction direction) {
        forEach((block, data) -> data.put(DataType.DIRECTION, direction));
    }

    public void attachOffsetData(Vector from) {
        forEach((block, data) -> data.put(DataType.VECTOR_OFFSET, block.getLocation().toVector().subtract(from)));
    }

    public void attachStackedBlockDataIfStackedBlock() {
        SystemManager wildStackerSystemManager = WildStackerAPI.getWildStacker().getSystemManager();
        Set<Chunk> blockChunks = new HashSet<>();
        forEach((block, data) -> {
            Chunk blockChunk = block.getChunk();
            blockChunks.add(blockChunk);
            if (wildStackerSystemManager.isStackedBarrel(block))
                data.put(DataType.STACKED_BLOCK_OBJECT, wildStackerSystemManager.getStackedBarrel(block));
            else if (wildStackerSystemManager.isStackedSpawner(block))
                //FIXME getStackedSpawner returning null
                data.put(DataType.STACKED_BLOCK_OBJECT, wildStackerSystemManager.getStackedSpawner(block.getLocation()));
        });
        blockChunks.forEach(chunk -> {
            GridHandler grid = SuperiorSkyblockPlugin.getPlugin().getGrid();
            ChunkPosition chunkPosition = ChunkPosition.of(chunk);
            grid.getStackedBlocks(chunkPosition).forEach(stackedBlock -> {
                Block stackedBlockBlock = stackedBlock.getBlockPosition().getBlock();
                if (this.containsKey(stackedBlockBlock))
                    this.get(stackedBlockBlock).put(DataType.STACKED_BLOCK_OBJECT, stackedBlock);
            });
        });
    }

    public void attachTeleportData(Location teleportLocation) {
        forEach((block, data) -> {
            Location blockLocation = block.getLocation().clone();
            // For some reason checking if the locations are equal isn't ever returning true
            // so I check the distance instead.
            if (blockLocation.add(0, 1, 0).distance(teleportLocation) <= 1)
                data.put(DataType.BOOLEAN_TELEPORT_LOCATION, true);
        });
    }

    /**
     * Copies the blocks in this map to the location specified.
     * The location is the center of the region.
     */
    public void copyToLocation(Location destination) {
        SuperiorSkyblockPlugin.raidDebug("Getting ready to paste " + size() + " blocks.");
        int[] blockCount = {0};
        forEach((baseBlock, data) -> {
            Vector vectorOffsetData = (Vector) data.get(DataType.VECTOR_OFFSET);
            Direction directionData = (Direction) data.get(DataType.DIRECTION);
            Vector vectorOffsetRotationData = rotateAroundY(vectorOffsetData, directionData.getRadians());
            Location placementLocation = destination.clone().add(vectorOffsetRotationData);
            data.put(DataType.LOCATION_DESTINATION, placementLocation);
            Block newBlock = placementLocation.getBlock();
            Bukkit.getScheduler().runTask(SuperiorSkyblockPlugin.getPlugin(), () -> {
                newBlock.setType(baseBlock.getType());
                attachStackedBlockDataIfStackedBlock(baseBlock, newBlock);
                loadContentsIfContainerBlock(baseBlock, newBlock);
                //TODO Get sign text working
            });
            blockCount[0]++;

            // Sleep after every 3000 blocks are copied.
            if (blockCount[0] % 3000 == 0)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        });
    }

    private void attachStackedBlockDataIfStackedBlock(Block from, Block to) {
        Object stackedBlockData = get(from).get(DataType.STACKED_BLOCK_OBJECT);
        if (stackedBlockData == null) return;
        SystemManager wildStackerSystemManager = WildStackerAPI.getWildStacker().getSystemManager();
        if (stackedBlockData instanceof StackedBarrel) {
            wildStackerSystemManager.getStackedBarrel(to).setStackAmount(((StackedBarrel) stackedBlockData).getStackAmount(), true);
        } else if (stackedBlockData instanceof StackedSpawner) {
            StackedSpawner spawner = wildStackerSystemManager.getStackedSpawner(to.getLocation());
            spawner.setStackAmount(((StackedSpawner) stackedBlockData).getStackAmount(), true);
            //FIXME Entity linking not working
            spawner.setLinkedEntity(((StackedSpawner) stackedBlockData).getLinkedEntity());
        } else if (stackedBlockData instanceof StackedBlocksHandler.StackedBlock)
            SuperiorSkyblockPlugin.getPlugin().getGrid().setBlockAmount(to, ((StackedBlocksHandler.StackedBlock) stackedBlockData).getAmount());
    }

    private void loadContentsIfContainerBlock(Block fromBlock, Block toBlock) {
        //FIXME Double chests look like two single chests
        BlockState fromBlockState = fromBlock.getState();
        BlockState toBlockState = toBlock.getState();
        if (!(fromBlockState instanceof Container && toBlockState instanceof Container)) return;
        Container fromContainer = (Container) fromBlockState;
        Container toContainer = (Container) toBlockState;
        for (ItemStack item : fromContainer.getInventory()) {
            toContainer.getInventory().addItem(item != null ? item : new ItemStack(Material.AIR));
        }
    }

    private Vector rotateAroundY(Vector vector, double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * vector.getX() + angleSin * vector.getZ();
        double z = -angleSin * vector.getX() + angleCos * vector.getZ();
        return vector.setX(x).setZ(z);
    }
}
