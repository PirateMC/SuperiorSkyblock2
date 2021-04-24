package com.bgsoftware.superiorskyblock.raiding.invites;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class RaidInvitationHandler {

    private static Set<RaidInvitation> raidInvitations = new HashSet<>();

    static {
        Bukkit.getScheduler().runTaskTimer(SuperiorSkyblockPlugin.getPlugin(), () -> {
            Iterator<RaidInvitation> iterator = raidInvitations.iterator();
            while (iterator.hasNext()) {
                RaidInvitation invitation = iterator.next();
                invitation.setTimeLeft(invitation.getTimeLeft() - 1);
                SuperiorSkyblockPlugin.raidDebug("Invitation from " + invitation.getSenderUuid() + " has " + invitation.getTimeLeft() + " time left.");
                if (invitation.getTimeLeft() <= 0) {
                    Player invitee = Bukkit.getPlayer(invitation.getInviteeUuid());
                    Player sender = Bukkit.getPlayer(invitation.getSenderUuid());
                    if (invitee != null && sender != null) {
                        invitee.sendMessage("Invitation from " + sender.getName() + " expired.");
                        sender.sendMessage("Invitation to " + invitee.getName() + " expired.");
                    }
                    iterator.remove();
                    SuperiorSkyblockPlugin.raidDebug("Invitation by " + invitation.getSenderUuid() + " to " + invitation.getInviteeUuid() + " expired.");
                }
            }
        }, 20, 20);
    }

    public static boolean addInvitation(RaidInvitation invitation) {
        return raidInvitations.add(invitation);
    }

    public static boolean containsInvitationTo(UUID uuid) {
        return raidInvitations.stream().anyMatch(invitation -> invitation.getInviteeUuid().equals(uuid));
    }

    public static RaidInvitation getInvitation(UUID sender, UUID invitee) {
        return raidInvitations.stream()
                .filter(invitation -> invitation.getSenderUuid().equals(sender) && invitation.getInviteeUuid().equals(invitee))
                .findAny()
                .get();
    }

    public static boolean removeInvitation(UUID sender, UUID invitee) {
        return raidInvitations.removeIf(invitation -> invitation.getSenderUuid().equals(sender) && invitation.getInviteeUuid().equals(invitee));
    }

    public static boolean removeInvitationsOfSender(UUID sender) {
        return raidInvitations.removeIf(invitation -> invitation.getSenderUuid().equals(sender));
    }

    public static boolean removeInvitationsOfInvitee(UUID invitee) {
        return raidInvitations.removeIf(invitation -> invitation.getInviteeUuid().equals(invitee));
    }
}
