package com.bgsoftware.superiorskyblock.raiding;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;

import java.util.Set;

public class IslandCopy {
    private BlockArrayClipboard clipboard;
    private Set<Object> stackedBlocks;

    IslandCopy(BlockArrayClipboard clipboard, Set<Object> stackedBlocks) {
        this.clipboard = clipboard;
        this.stackedBlocks = stackedBlocks;
    }

    BlockArrayClipboard getClipboard() {
        return clipboard;
    }

    Set<Object> getStackedBlocks() {
        return stackedBlocks;
    }
}
