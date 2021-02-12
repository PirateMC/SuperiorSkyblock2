package com.bgsoftware.superiorskyblock.commands;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.utils.commands.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CmdRaid implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("withdraw");
    }

    @Override
    public String getPermission() {
        return "superior.island.raid";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "raid < " + Locale.COMMAND_ARGUMENT_AMOUNT.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_RAID.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {

    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

class RaidInvitation {
    private UUID senderUuid;
    private UUID inviteeUuid;
    private long timeLeft = 60;

    public RaidInvitation(UUID senderUuid, UUID inviteeUuid) {
        this.senderUuid = senderUuid;
        this.inviteeUuid = inviteeUuid;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public UUID getInviteeUuid() {
        return inviteeUuid;
    }

    public long getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RaidInvitation)) return false;
        return ((RaidInvitation) obj).senderUuid.equals(senderUuid)
                && ((RaidInvitation) obj).inviteeUuid.equals(inviteeUuid);
    }
}

class RaidInvitationHandler {
    private static Set<RaidInvitation> raidInvitations = new HashSet<>();

    public static boolean addInvitation(RaidInvitation invitation) {
        return raidInvitations.add(invitation);
    }
}
