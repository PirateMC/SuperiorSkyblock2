package com.bgsoftware.superiorskyblock.commands.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CmdRaidRestore implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("raidrestore");
    }

    @Override
    public String getPermission() {
        return "superior.island.raidrestore";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "raidrestore";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_RAID_RESTORE.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        String ownerName = args[1];
        Player player = Bukkit.getPlayer(ownerName);
        if (player == null) {
            sender.sendMessage("Could not find player " + ownerName + ".");
            return;
        }
        Location raidIslandCenter = CmdStartRaid.occupiedIslandLocations.get(player.getUniqueId());
        World world = raidIslandCenter.getWorld();
        CuboidRegion region = CuboidRegion.fromCenter(BlockVector3.at(raidIslandCenter.getX(), raidIslandCenter.getY(), raidIslandCenter.getZ()), 32);
        for (int x = region.getMinimumPoint().getX(); x < region.getMaximumPoint().getX(); x++)
            for (int z = region.getMinimumPoint().getZ(); z < region.getMaximumPoint().getZ(); z++)
                for (int y = region.getMinimumPoint().getY(); y < region.getMaximumPoint().getY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (y <= 197) block.setType(Material.WATER);
                    else block.setType(Material.AIR);
                }
        SuperiorSkyblockPlugin.raidDebug("Island of " + player.getName() + " has been removed from raid world.");
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
