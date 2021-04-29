package com.bgsoftware.superiorskyblock.raiding.queue;

//TODO Type is similar to RaidInvitation
// Create more abstract type

import com.bgsoftware.superiorskyblock.raiding.util.Pair;

import java.util.UUID;

public final class RaidQueueEntry extends Pair<UUID, UUID> {

    public RaidQueueEntry(UUID sender, UUID receiver) {
        super(sender, receiver);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RaidQueueEntry)) return false;
        return ((RaidQueueEntry) obj).getKey().equals(this.getKey()) &&
                ((RaidQueueEntry) obj).getValue().equals(this.getValue());
    }
}
