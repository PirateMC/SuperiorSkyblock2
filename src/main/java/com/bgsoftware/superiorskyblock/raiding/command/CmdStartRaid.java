package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class CmdStartRaid implements ISuperiorCommand {

    private int nextRaidLocationX = 0;

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
        plugin.getRaidIslandManager().createRaidIsland(teamOneIsland, islandOneDest, false);
        plugin.getRaidIslandManager().createRaidIsland(teamTwoIsland, islandTwoDest, true);
        teamOneIsland.getIslandMembers(true).forEach(member -> member.teleport(islandOneDest));
        teamTwoIsland.getIslandMembers(true).forEach(member -> member.teleport(islandTwoDest));
        nextRaidLocationX += 100;
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
