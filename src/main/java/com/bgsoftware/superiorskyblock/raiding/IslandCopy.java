package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;

import java.util.Set;

public class IslandCopy {
    private BlockArrayClipboard clipboard;
    private Set<StackedObject> stackedBlocks;

    IslandCopy(BlockArrayClipboard clipboard, Set<StackedObject> stackedBlocks) {
        this.clipboard = clipboard;
        this.stackedBlocks = stackedBlocks;
    }

    BlockArrayClipboard getClipboard() {
        return clipboard;
    }

    Set<StackedObject> getStackedBlocks() {
        return stackedBlocks;
    }
}
