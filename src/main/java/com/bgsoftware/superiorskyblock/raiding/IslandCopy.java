package com.bgsoftware.superiorskyblock.raiding;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;

public class IslandCopy {
    private BlockArrayClipboard clipboard;

    IslandCopy(BlockArrayClipboard clipboard) {
        this.clipboard = clipboard;
    }

    BlockArrayClipboard getClipboard() {
        return clipboard;
    }
}
