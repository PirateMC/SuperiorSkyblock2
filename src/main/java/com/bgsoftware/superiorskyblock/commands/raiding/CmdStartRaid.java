package com.bgsoftware.superiorskyblock.commands.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class CmdStartRaid implements ISuperiorCommand {

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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Island teamOneIsland = plugin.getGrid().getIsland(teamOneLeader.getUniqueId());
            Island teamTwoIsland = plugin.getGrid().getIsland(teamTwoLeader.getUniqueId());
            int destX = (int) (Math.random() * 100);
            int destZ = (int) (Math.random() * 100);
            copyIsland(teamOneIsland, raidWorld, destX, destZ, plugin);
            copyIsland(teamTwoIsland, raidWorld, destX, destZ + 3, plugin);
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<SuperiorPlayer> teamOneMembers = teamOneIsland.getIslandMembers(true);
                List<SuperiorPlayer> teamTwoMembers = teamTwoIsland.getIslandMembers(true);
                teamOneMembers.forEach(member -> {
                    if (member.isOnline()) {
                        Location center = teamOneIsland.getCenter(World.Environment.NORMAL).add(0, 3, 0);
                        member.teleport(new Location(raidWorld, destX * 16, center.getY(), destZ * 16));
                    }
                });
                teamTwoMembers.forEach(member -> {
                    if (member.isOnline()) {
                        Location center = teamTwoIsland.getCenter(World.Environment.NORMAL).add(0, 3, 0);
                        member.teleport(new Location(raidWorld, destX * 16, center.getY(), (destZ + 3) * 16));
                    }
                });
            });
        });
    }

    private void copyIsland(Island island, World destWorld, int toChunkX, int toChunkZ, SuperiorSkyblockPlugin plugin) {
        final AtomicInteger chunkXOffset = new AtomicInteger(0);
        final AtomicInteger chunkZOffset = new AtomicInteger(0);
        final int[] initialChunkX = new int[2];
        final int[] initialChunkZ = new int[2];
        island.getAllChunks().forEach(chunk -> {
            if (initialChunkX[0] == 0 || initialChunkZ[0] == 0) {
                initialChunkX[1] = chunk.getX();
                initialChunkZ[1] = chunk.getZ();
                initialChunkX[0] = 1;
                initialChunkZ[0] = 1;
            }
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < chunk.getChunkSnapshot().getHighestBlockYAt(x, z); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        int destX = (initialChunkX[1] > chunk.getX() ? initialChunkX[1] - chunk.getX() : -(chunk.getX() - initialChunkX[1]) + toChunkX) * 16 + x;
                        int destZ = (initialChunkZ[1] > chunk.getZ() ? initialChunkZ[1] - chunk.getZ() : -(chunk.getZ() - initialChunkZ[1]) + toChunkZ) * 16 + z;
                        int finalY = y;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            destWorld.getBlockAt(destX, finalY + 3, destZ).setType(block.getType());
                        });
                    }
            if (chunkXOffset.getAndIncrement() >= 3) {
                chunkXOffset.set(0);
                chunkZOffset.getAndIncrement();
            }
        });
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
