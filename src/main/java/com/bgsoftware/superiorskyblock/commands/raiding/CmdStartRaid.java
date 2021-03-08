package com.bgsoftware.superiorskyblock.commands.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public final class CmdStartRaid implements ISuperiorCommand {

    private int nextRaidLocationX = 0;
    private int nextRaidLocationZ = 0;
    public static Map<UUID, HashSet<Location>> islandOwnerBlocks = new HashMap<>();

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("startraid");
    }

    @Override
    public String getPermission() {
        return "superior.island.startraid";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "startraid <" + Locale.COMMAND_ARGUMENT_ISLAND_NAME.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_START_RAID.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return true;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        Player teamOneLeader = Bukkit.getPlayer(args[1]);
        Player teamTwoLeader = Bukkit.getPlayer(args[2]);

        if (teamOneLeader == null) {
            sender.sendMessage("Could not find player " + args[1] + ".");
            return;
        }

        if (teamTwoLeader == null) {
            sender.sendMessage("Could not find player " + args[2] + ".");
            return;
        }

        World raidWorld = Bukkit.getWorld("RaidWorld");

        if (raidWorld == null) {
            SuperiorSkyblockPlugin.raidDebug("Couldn't get world 'RaidWorld'.");
            return;
        }

//        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        Island teamOneIsland = plugin.getGrid().getIsland(teamOneLeader.getUniqueId());
        Island teamTwoIsland = plugin.getGrid().getIsland(teamTwoLeader.getUniqueId());
        List<Chunk> teamOneIslandChunks = teamOneIsland.getAllChunks();
        List<Chunk> teamTwoIslandChunks = teamTwoIsland.getAllChunks();
        islandOwnerBlocks.put(teamOneIsland.getOwner().getUniqueId(), new HashSet<>());
        islandOwnerBlocks.put(teamTwoIsland.getOwner().getUniqueId(), new HashSet<>());
        int destX = nextRaidLocationX;
        int destZ = nextRaidLocationZ;
        nextRaidLocationX += teamOneIslandChunks.size() * 2;

        Location islandCenter = teamOneIsland.getCenter(World.Environment.NORMAL);
        double teleportOffsetX = islandCenter.getX() - teamOneIslandChunks.get(0).getX() * 16;
        double teleportOffsetZ = islandCenter.getZ() - teamOneIslandChunks.get(0).getZ() * 16;

        Class<?> blockVector3Class = null;
        Constructor<?> forwardExtentCopyConstructor = null;
        Method blockVectorAt = null;
        Method cuboidRegionFromCenter = null;
        Method regionMinimumPoint = null;
        Object blockVector3 = null;
        try {
            blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            forwardExtentCopyConstructor = ForwardExtentCopy.class.getConstructor(Extent.class, Region.class, Extent.class, blockVector3Class);
            blockVectorAt = blockVector3Class.getDeclaredMethod("at", double.class, double.class, double.class);
            blockVector3 = blockVectorAt.invoke(null, islandCenter.getX(), islandCenter.getY(), islandCenter.getZ());
            cuboidRegionFromCenter = CuboidRegion.class.getDeclaredMethod("fromCenter", blockVector3Class, int.class);
            regionMinimumPoint = CuboidRegion.class.getMethod("getMinimumPoint");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Region islandRegion = null;
        Object minimumPoint = null;
        try {
            islandRegion = (Region) cuboidRegionFromCenter.invoke(null, blockVector3, 100);
            minimumPoint = regionMinimumPoint.invoke(islandRegion);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
//        Region islandRegion = CuboidRegion.fromCenter(new Vector(islandCenter.getX(), islandCenter.getY(), islandCenter.getZ()), 100);

        if (islandRegion == null) {
            SuperiorSkyblockPlugin.raidDebug("The island region is null.");
        }
        if (minimumPoint == null) {
            SuperiorSkyblockPlugin.raidDebug("The minimum point is null.");
        }

        // Copy island
        BlockArrayClipboard clipboard = new BlockArrayClipboard(islandRegion);
        {
            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(islandCenter.getWorld()), 1000);
            Object forwardExtentCopy = null;
            try {
                forwardExtentCopy = forwardExtentCopyConstructor.newInstance(editSession, islandRegion, clipboard, minimumPoint);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (forwardExtentCopy == null) {
                SuperiorSkyblockPlugin.raidDebug("The forward extent copy is null.");
            }

            Operations.complete((ForwardExtentCopy) forwardExtentCopy);
        }

        // Paste island
        {
            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(raidWorld), 1000);
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(new Vector((destX * 16) + teleportOffsetX, islandCenter.getY() + 3, (destZ)))
                    .build();
            Operations.complete(operation);
        }

//            copyIsland(teamOneIsland, raidWorld, destX, destZ, plugin);
//            copyIsland(teamTwoIsland, raidWorld, destX, destZ + 3, plugin);
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<SuperiorPlayer> teamOneMembers = teamOneIsland.getIslandMembers(true);
            List<SuperiorPlayer> teamTwoMembers = teamTwoIsland.getIslandMembers(true);
            teamOneMembers.forEach(member -> {
                if (member.isOnline()) {
//                        Location islandCenter = teamOneIsland.getCenter(World.Environment.NORMAL);
//                        double teleportOffsetX = islandCenter.getX() - teamOneIslandChunks.get(0).getX() * 16;
//                        double teleportOffsetZ = islandCenter.getZ() - teamOneIslandChunks.get(0).getZ() * 16;
                    Location teleportLocation = new Location(raidWorld, (destX * 16) + teleportOffsetX, islandCenter.getY() + 3, (destZ * 16) + teleportOffsetZ);
                    member.teleport(teleportLocation);

                }
            });
            teamTwoMembers.forEach(member -> {
                if (member.isOnline()) {
//                        Location islandCenter = teamTwoIsland.getCenter(World.Environment.NORMAL);
//                        double teleportOffsetX = islandCenter.getX() - teamTwoIslandChunks.get(0).getX() * 16;
//                        double teleportOffsetZ = islandCenter.getZ() - teamTwoIslandChunks.get(0).getZ() * 16;
                    Location teleportLocation = new Location(raidWorld, (destX * 16) + teleportOffsetX, islandCenter.getY() + 3, ((destZ + 3) * 16) + teleportOffsetZ);
                    member.teleport(teleportLocation);
                }
            });
        });
//        });
    }

    private void copyIsland(Island island, World destWorld, int toChunkX, int toChunkZ, SuperiorSkyblockPlugin plugin) {
        final int[] initialChunkX = new int[2];
        final int[] initialChunkZ = new int[2];

        SuperiorSkyblockPlugin.raidDebug("There are " + island.getAllChunks().size() + " chunks to copy.");

        island.getAllChunks().forEach(chunk -> {
            // This is so the island chunks are placed in the correct
            // order instead of in a jumbled manner
            if (initialChunkX[0] == 0 || initialChunkZ[0] == 0) {
                initialChunkX[1] = chunk.getX();
                initialChunkZ[1] = chunk.getZ();
                initialChunkX[0] = 1;
                initialChunkZ[0] = 1;
            }

            // The copying of each block of the original island to the raid world
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < chunk.getChunkSnapshot().getHighestBlockYAt(x, z); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == Material.AIR) {
                            continue;
                        }
                        int destX = (initialChunkX[1] > chunk.getX() ? -(initialChunkX[1] - chunk.getX()) : (chunk.getX() - initialChunkX[1]) + toChunkX) * 16 + x;
                        int destZ = (initialChunkZ[1] > chunk.getZ() ? -(initialChunkZ[1] - chunk.getZ()) : (chunk.getZ() - initialChunkZ[1]) + toChunkZ) * 16 + z;
                        int finalY = y;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Block targetBlock = destWorld.getBlockAt(destX, finalY + 3, destZ);
                            islandOwnerBlocks.get(island.getOwner().getUniqueId()).add(targetBlock.getLocation());
                            targetBlock.setType(block.getType());
                            if (targetBlock.getType() == Material.CHEST) {
                                Chest fromChest = (Chest) block.getState();
                                BlockState blockState = targetBlock.getState();
                                Chest toChest = (Chest) blockState;
                                for (ItemStack item : fromChest.getBlockInventory().getContents()) {
                                    toChest.getBlockInventory().addItem(item != null ? item : new ItemStack(Material.AIR));
                                }
                            }
                        });
                    }
        });
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
