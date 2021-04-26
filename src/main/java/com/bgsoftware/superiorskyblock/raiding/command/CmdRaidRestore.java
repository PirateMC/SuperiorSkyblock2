package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class CmdRaidRestore implements ISuperiorCommand {

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
        return true;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        String ownerName = args[1];
        Player player = Bukkit.getPlayer(ownerName);
        if (player == null) {
            sender.sendMessage("Could not find player " + ownerName + ".");
            return;
        }
        plugin.getRaidIslandManager().restoreRaidSlot(player.getUniqueId());
        SuperiorSkyblockPlugin.raidDebug("Island of " + player.getName() + " has been removed from raid world.");
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
