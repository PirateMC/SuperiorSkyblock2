package com.bgsoftware.superiorskyblock.schematics;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.schematic.Schematic;
import com.bgsoftware.superiorskyblock.utils.LocationUtils;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunkPosition;
import com.bgsoftware.superiorskyblock.utils.key.Key;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

public final class WorldEditSchematic extends BaseSchematic implements Schematic {

    private static final ReflectMethod<Object> GET_BLOCK_TYPE = new ReflectMethod<>(BaseBlock.class, "getBlockType");
    private static final ReflectMethod<Integer> GET_INTERNAL_ID = new ReflectMethod<>(BaseBlock.class, "getInternalId");
    private static final ReflectMethod<Material> ADAPT = new ReflectMethod<>("com.sk89q.worldedit.bukkit.BukkitAdapter", "adapt",
            "com.sk89q.worldedit.world.block.BlockTypes");

    private static final ReflectMethod<Integer> GET_ID = new ReflectMethod<>(BaseBlock.class, "getId");
    private static final ReflectMethod<Integer> GET_DATA = new ReflectMethod<>(BaseBlock.class, "getData");

    private final Clipboard schematic;

    public WorldEditSchematic(String name, Clipboard schematic) {
        super(name);
        this.schematic = schematic;
        readBlocks();
    }

    @Override
    public void pasteSchematic(Island island, Location location, Runnable callback) {
        pasteSchematic(island, location, callback, null);
    }

    @Override
    public void pasteSchematic(Island island, Location location, Runnable callback, Consumer<Throwable> onFailure) {
        try {
            SuperiorSkyblockPlugin.debug("Action: Paste Schematic, Island: " + island.getOwner().getName() + ", Location: " + LocationUtils.getLocation(location) + ", Schematic: " + name);

            BlockVector3 _point = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            EditSession editSession = schematic.paste(new BukkitWorld(location.getWorld()), _point, false, true, null);

            if (editSession == null) {
                BlockVector3 point = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                editSession = schematic.paste(new BukkitWorld(location.getWorld()), point, true, true, null);
            }


            //TODO Re-implement this. Currently waiting for a reply on GitHub regarding the issue.
            // https://github.com/IntellectualSites/FastAsyncWorldEdit/issues/317
//            editSession.addNotifyTask(() -> {
//                try {
//                    island.handleBlocksPlace(cachedCounts);
//
//                    EventsCaller.callIslandSchematicPasteEvent(island, name, location);
//
//                    callback.run();
//                } catch (Throwable ex) {
//                    if (onFailure != null)
//                        onFailure.accept(ex);
//                }
//            });
        } catch (Throwable ex) {
            if (onFailure != null)
                onFailure.accept(ex);
        }
    }

    @Override
    public Set<ChunkPosition> getLoadedChunks() {
        return Collections.emptySet();
    }

    private void readBlocks() {
        BlockArrayClipboard clipboard = (BlockArrayClipboard) schematic;

        assert clipboard != null;

        clipboard.forEach(this::readBlock);
    }

    private void readBlock(Object baseBlock) {
        Key key;

        if (ADAPT.isValid() && GET_BLOCK_TYPE.isValid() && GET_INTERNAL_ID.isValid()) {
            Material material = ADAPT.invoke(null, GET_BLOCK_TYPE.invoke(baseBlock));
            int data = GET_INTERNAL_ID.invokeWithDef(baseBlock, 0);
            key = Key.of(material, (byte) data);
        } else {
            int id = GET_ID.invoke(baseBlock);
            int data = GET_DATA.invoke(baseBlock);
            //noinspection deprecation
            key = Key.of(Material.getMaterial(id), (byte) data);
        }

        cachedCounts.put(key, cachedCounts.getRaw(key, 0) + 1);
    }
}
