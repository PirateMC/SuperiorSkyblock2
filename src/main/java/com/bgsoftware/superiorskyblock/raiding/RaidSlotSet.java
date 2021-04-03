package com.bgsoftware.superiorskyblock.raiding;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public final class RaidSlotSet extends HashSet<RaidSlot> {
    public Optional<RaidSlot> getSlotOfIslandOwner(UUID ownerUuid) {
        return this.stream().filter(slot -> slot.getFirstIslandOwner().equals(ownerUuid) || slot.getSecondIslandOwner().equals(ownerUuid)).findFirst();
    }

    public boolean removeSlotOfOwner(UUID ownerUuid) {
        return removeIf(slot -> slot.getFirstIslandOwner().equals(ownerUuid) || slot.getSecondIslandOwner().equals(ownerUuid));
    }
}
