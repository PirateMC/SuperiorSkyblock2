package com.bgsoftware.superiorskyblock.raiding;

import java.util.UUID;

public class RaidInvitation {
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
