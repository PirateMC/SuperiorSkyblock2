package com.bgsoftware.superiorskyblock.raiding;

import java.util.HashSet;
import java.util.Set;

public class RaidInvitationHandler {
    private static Set<RaidInvitation> raidInvitations = new HashSet<>();

    public static boolean addInvitation(RaidInvitation invitation) {
        return raidInvitations.add(invitation);
    }
}
