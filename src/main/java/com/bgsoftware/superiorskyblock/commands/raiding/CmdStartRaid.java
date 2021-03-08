package com.bgsoftware.superiorskyblock.commands.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public final class CmdStartRaid implements ISuperiorCommand {

    private int nextRaidLocationX = 0;
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
        World superiorWorld = Bukkit.getWorld("SuperiorWorld");

        if (raidWorld == null) {
            SuperiorSkyblockPlugin.raidDebug("Couldn't get world 'RaidWorld'.");
            return;
        }

        Island teamOneIsland = plugin.getGrid().getIsland(teamOneLeader.getUniqueId());
        Island teamTwoIsland = plugin.getGrid().getIsland(teamTwoLeader.getUniqueId());
        Location islandOneDest = new Location(raidWorld, nextRaidLocationX, 200, 0);
        Location islandTwoDest = new Location(raidWorld, nextRaidLocationX, 200, 100);
        copyIsland(teamOneIsland, islandOneDest);
        copyIsland(teamTwoIsland, islandTwoDest);
        teamOneIsland.getIslandMembers(true).forEach(member -> member.teleport(islandOneDest));
        teamTwoIsland.getIslandMembers(true).forEach(member -> member.teleport(islandTwoDest));
        nextRaidLocationX += 100;
    }

    private void copyIsland(Island island, Location destination) {
        Location islandCenter = island.getCenter(World.Environment.NORMAL);

        CuboidRegion islandRegion = CuboidRegion.fromCenter(BlockVector3.at(islandCenter.getX(), islandCenter.getY(), islandCenter.getZ()), 32);

        SuperiorSkyblockPlugin.raidDebug("Island region center: " + islandRegion.getCenter());
        SuperiorSkyblockPlugin.raidDebug("Island region minimum point: " + islandRegion.getMinimumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region maximum point: " + islandRegion.getMaximumPoint());
        SuperiorSkyblockPlugin.raidDebug("Island region area: " + islandRegion.getArea());

        BlockArrayClipboard clipboard = new BlockArrayClipboard(islandRegion);

        // Copy island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(islandCenter.getWorld()), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, islandRegion, clipboard, islandRegion.getMinimumPoint());
            forwardExtentCopy.setCopyingBiomes(true);
            forwardExtentCopy.setCopyingEntities(true);
            Operations.complete(forwardExtentCopy);
            SuperiorSkyblockPlugin.raidDebug("Finished copying " + island.getName());
        }

        // Paste island
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(destination.getWorld()), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(destination.getX() - 32, destination.getY() - 32, destination.getZ() - 32))
                    .ignoreAirBlocks(true)
                    .copyBiomes(true)
                    .copyEntities(true)
                    .build();
            Operations.complete(operation);
            SuperiorSkyblockPlugin.raidDebug("Finished pasting " + island.getName());
        }
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
