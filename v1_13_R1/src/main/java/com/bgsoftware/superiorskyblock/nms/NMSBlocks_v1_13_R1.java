package com.bgsoftware.superiorskyblock.nms;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.listeners.BlocksListener;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunkPosition;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunksTracker;
import com.bgsoftware.superiorskyblock.utils.key.Key;
import com.bgsoftware.superiorskyblock.utils.key.KeyMap;
import com.bgsoftware.superiorskyblock.utils.pair.BiPair;
import com.bgsoftware.superiorskyblock.utils.reflections.ReflectField;
import com.bgsoftware.superiorskyblock.utils.tags.ByteTag;
import com.bgsoftware.superiorskyblock.utils.tags.CompoundTag;
import com.bgsoftware.superiorskyblock.utils.tags.IntArrayTag;
import com.bgsoftware.superiorskyblock.utils.tags.StringTag;
import com.bgsoftware.superiorskyblock.utils.tags.Tag;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import com.google.common.collect.Maps;
import net.minecraft.server.v1_13_R1.AxisAlignedBB;
import net.minecraft.server.v1_13_R1.BiomeBase;
import net.minecraft.server.v1_13_R1.Block;
import net.minecraft.server.v1_13_R1.BlockBed;
import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.BlockProperties;
import net.minecraft.server.v1_13_R1.BlockStateBoolean;
import net.minecraft.server.v1_13_R1.BlockStateEnum;
import net.minecraft.server.v1_13_R1.BlockStateInteger;
import net.minecraft.server.v1_13_R1.Blocks;
import net.minecraft.server.v1_13_R1.Chunk;
import net.minecraft.server.v1_13_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_13_R1.ChunkProviderServer;
import net.minecraft.server.v1_13_R1.ChunkSection;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityPlayer;
import net.minecraft.server.v1_13_R1.EnumSkyBlock;
import net.minecraft.server.v1_13_R1.IBlockData;
import net.minecraft.server.v1_13_R1.IBlockState;
import net.minecraft.server.v1_13_R1.IChatBaseComponent;
import net.minecraft.server.v1_13_R1.IChunkAccess;
import net.minecraft.server.v1_13_R1.IChunkLoader;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R1.PacketPlayOutBlockChange;
import net.minecraft.server.v1_13_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_13_R1.PacketPlayOutUnloadChunk;
import net.minecraft.server.v1_13_R1.PlayerConnection;
import net.minecraft.server.v1_13_R1.ProtoChunk;
import net.minecraft.server.v1_13_R1.TileEntity;
import net.minecraft.server.v1_13_R1.TileEntitySign;
import net.minecraft.server.v1_13_R1.World;
import net.minecraft.server.v1_13_R1.WorldServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R1.block.CraftSign;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_13_R1.util.UnsafeList;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "ConstantConditions", "rawtypes"})
public final class NMSBlocks_v1_13_R1 implements NMSBlocks {

    private static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    private static final Map<UUID, IChunkLoader> chunkLoadersMap = Maps.newHashMap();
    private static final Map<String, IBlockState> nameToBlockState = new HashMap<>();
    private static final Map<IBlockState, String> blockStateToName = new HashMap<>();
    private static final ReflectField<Boolean> RANDOM_TICK = new ReflectField<>(Block.class, Boolean.class, "randomTick");
    private static final ReflectField<IChunkLoader> CHUNK_LOADER = new ReflectField<>(ChunkProviderServer.class, IChunkLoader.class, "chunkLoader");

    static {
        Map<String, String> fieldNameToName = new HashMap<>();
        fieldNameToName.put("z", "axis-empty");
        fieldNameToName.put("H", "facing-notup");
        fieldNameToName.put("I", "facing-horizontal");
        fieldNameToName.put("K", "redstone-east");
        fieldNameToName.put("L", "redstone-north");
        fieldNameToName.put("M", "redstone-south");
        fieldNameToName.put("N", "redstone-west");
        fieldNameToName.put("O", "double-half");
        fieldNameToName.put("Q", "track-shape-empty");
        fieldNameToName.put("R", "track-shape");
        fieldNameToName.put("S", "age2");
        fieldNameToName.put("T", "age3");
        fieldNameToName.put("U", "age5");
        fieldNameToName.put("V", "age7");
        fieldNameToName.put("W", "age15");
        fieldNameToName.put("X", "age25");
        fieldNameToName.put("ae", "level3");
        fieldNameToName.put("af", "level1-8");
        fieldNameToName.put("ag", "level15");
        fieldNameToName.put("aa", "distance1-7");
        fieldNameToName.put("ao", "chest-type");
        fieldNameToName.put("ap", "comparator-mode");
        fieldNameToName.put("as", "piston-type");
        fieldNameToName.put("at", "slab-type");

        try{
            for(Field field : BlockProperties.class.getFields()){
                Object value = field.get(null);
                if(value instanceof IBlockState) {
                    register(fieldNameToName.getOrDefault(field.getName(), ((IBlockState) value).a()),
                            field.getName(), (IBlockState) value);
                }
            }
        }catch (Exception ignored){}
    }

    private static void register(String key, String fieldName, IBlockState<?> blockState){
        if(nameToBlockState.containsKey(key)){
            SuperiorSkyblockPlugin.log("&cWarning: block state " + key + "(" + fieldName + ") already exists. Contact Ome_R!");
        }
        else {
            nameToBlockState.put(key, blockState);
            blockStateToName.put(blockState, key);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setBlock(org.bukkit.Chunk bukkitChunk, Location location, int combinedId, CompoundTag statesTag, CompoundTag tileEntity) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        Chunk chunk = world.getChunkAt(location.getChunk().getX(), location.getChunk().getZ());

        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        IBlockData blockData = Block.getByCombinedId(combinedId);

        if(statesTag != null){
            for(Map.Entry<String, Tag<?>> entry : statesTag.getValue().entrySet()){
                try {
                    IBlockState blockState = nameToBlockState.get(entry.getKey());
                    if(blockState != null) {
                        if (entry.getValue() instanceof ByteTag) {
                            blockData = blockData.set(blockState, ((ByteTag) entry.getValue()).getValue() == 1);
                        } else if (entry.getValue() instanceof IntArrayTag) {
                            int[] data = ((IntArrayTag) entry.getValue()).getValue();
                            blockData = blockData.set(blockState, data[0]);
                        } else if (entry.getValue() instanceof StringTag) {
                            String data = ((StringTag) entry.getValue()).getValue();
                            blockData = blockData.set(blockState, Enum.valueOf(blockState.b(), data));
                        }
                    }
                }catch (Exception ignored){}
            }
        }

        if((blockData.getMaterial().isLiquid() && plugin.getSettings().liquidUpdate) || blockData.getBlock() instanceof BlockBed) {
            world.setTypeAndData(blockPosition, blockData, 3);
            return;
        }

        int indexY = location.getBlockY() >> 4;

        ChunkSection chunkSection = chunk.getSections()[indexY];

        if(chunkSection == null)
            chunkSection = chunk.getSections()[indexY] = new ChunkSection(indexY << 4, chunk.world.worldProvider.g());

        int blockX = location.getBlockX() & 15, blockY = location.getBlockY() & 15, blockZ = location.getBlockZ() & 15;

        chunkSection.setType(blockX, blockY, blockZ, blockData);

        if(bukkitChunk.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL)
            world.c(EnumSkyBlock.SKY, blockPosition);

        if(tileEntity != null) {
            NBTTagCompound tileEntityCompound = (NBTTagCompound) tileEntity.toNBT();
            tileEntityCompound.setInt("x", blockPosition.getX());
            tileEntityCompound.setInt("y", blockPosition.getY());
            tileEntityCompound.setInt("z", blockPosition.getZ());
            world.getTileEntity(blockPosition).load(tileEntityCompound);
        }
    }

    @Override
    public void setBlock(Location location, Material material, byte data) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        setBlock(location.getChunk(), location, getCombinedId(material, data), null, null);

        AxisAlignedBB bb = new AxisAlignedBB(blockPosition.getX() - 60, 0, blockPosition.getZ() - 60,
                blockPosition.getX() + 60, 256, blockPosition.getZ() + 60);

        PacketPlayOutBlockChange packetPlayOutBlockChange = new PacketPlayOutBlockChange(world, blockPosition);

        for(Entity entity : world.getEntities(null, bb)){
            if(entity instanceof EntityPlayer)
                ((EntityPlayer) entity).playerConnection.sendPacket(packetPlayOutBlockChange);
        }
    }

    @Override
    public CompoundTag readBlockStates(Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        IBlockData blockData = world.getType(blockPosition);
        CompoundTag compoundTag = null;

        for(Map.Entry<IBlockState<?>, Comparable<?>> entry : blockData.b().entrySet()){
            if(compoundTag == null)
                compoundTag = new CompoundTag();

            Tag<?> value;
            Class<?> keyClass = entry.getKey().getClass();
            String name = entry.getKey().a();

            if(keyClass.equals(BlockStateBoolean.class)) {
                value = new ByteTag((Boolean) entry.getValue() ? (byte) 1 : 0);
            }
            else if(keyClass.equals(BlockStateInteger.class)) {
                BlockStateInteger key = (BlockStateInteger) entry.getKey();
                value = new IntArrayTag(new int[] {(Integer) entry.getValue(), key.min, key.max});
            }
            else{
                BlockStateEnum<?> key = (BlockStateEnum<?>) entry.getKey();
                name = blockStateToName.get(key);
                value = new StringTag(((Enum<?>) entry.getValue()).name());
            }

            compoundTag.setTag(name, value);
        }

        return compoundTag;
    }

    @Override
    public CompoundTag readTileEntity(Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntity tileEntity = world.getTileEntity(blockPosition);

        if(tileEntity == null)
            return null;

        NBTTagCompound tileEntityCompound = tileEntity.save(new NBTTagCompound());

        tileEntityCompound.remove("x");
        tileEntityCompound.remove("y");
        tileEntityCompound.remove("z");

        return CompoundTag.fromNBT(tileEntityCompound);
    }

    @Override
    public void refreshChunk(org.bukkit.Chunk bukkitChunk) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();

        PacketPlayOutMapChunk packetPlayOutMapChunk = new PacketPlayOutMapChunk(chunk, 65535);

        AxisAlignedBB bb = new AxisAlignedBB((bukkitChunk.getX() << 4) - 60, 0, (bukkitChunk.getZ() << 4) - 60,
                (bukkitChunk.getX() << 4) + 60, 256, (bukkitChunk.getZ() << 4) + 60);

        Executor.ensureMain(() -> {
            for(Entity entity : chunk.getWorld().getEntities(null, bb)){
                if(entity instanceof EntityPlayer)
                    ((EntityPlayer) entity).playerConnection.sendPacket(packetPlayOutMapChunk);
            }
        });
    }

    @Override
    public void refreshLight(org.bukkit.Chunk bukkitChunk) {
        Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();

        for(int i = 0; i < 16; i++) {
            ChunkSection chunkSection = chunk.getSections()[i];
            if(chunkSection == null) {
                chunkSection = new ChunkSection(i << 4, chunk.world.worldProvider.g());
                chunk.getSections()[i] = chunkSection;
            }

            if (chunk.world.worldProvider.g())
                Arrays.fill(chunkSection.getSkyLightArray().asBytes(), (byte) 15);
        }

        chunk.initLighting();
    }

    @Override
    public int getCombinedId(Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        IBlockData blockData =  world.getType(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        return Block.getCombinedId(blockData);
    }

    @Override
    public int getCombinedId(Material material, byte data) {
        return Block.getCombinedId(data == 0 ? CraftMagicNumbers.getBlock(material).getBlockData() :
                CraftMagicNumbers.getBlock(material, data));
    }

    @Override
    public int compareMaterials(Material o1, Material o2) {
        if(o1.isBlock() && o2.isBlock()) {
            int firstMaterial = Block.getCombinedId(CraftMagicNumbers.getBlock(o1).getBlockData());
            int secondMaterial = Block.getCombinedId(CraftMagicNumbers.getBlock(o2).getBlockData());
            return Integer.compare(firstMaterial, secondMaterial);
        }
        else{
            return o1.name().compareTo(o2.name());
        }
    }

    @Override
    public org.bukkit.Chunk getChunkIfLoaded(ChunkPosition chunkPosition) {
        Chunk chunk = ((CraftWorld) chunkPosition.getWorld()).getHandle().getChunkProviderServer()
                .getChunkIfLoaded(chunkPosition.getX(), chunkPosition.getZ());
        return chunk == null ? null : chunk.bukkitChunk;
    }

    @Override
    public CompletableFuture<BiPair<ChunkPosition, KeyMap<Integer>, Set<Location>>> calculateChunk(ChunkPosition chunkPosition) {
        ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(chunkPosition.getX(), chunkPosition.getZ());

        CompletableFuture<BiPair<ChunkPosition, KeyMap<Integer>, Set<Location>>> completableFuture = new CompletableFuture<>();

        runActionOnChunk(chunkPosition.getWorld(), chunkCoords, false, chunk -> {
            KeyMap<Integer> blockCounts = new KeyMap<>();
            Set<Location> spawnersLocations = new HashSet<>();

            for(ChunkSection chunkSection : chunk.getSections()){
                if(chunkSection != null && chunkSection != Chunk.a){
                    for (BlockPosition bp : BlockPosition.b(0, 0, 0, 15, 15, 15)) {
                        IBlockData blockData = chunkSection.getType(bp.getX(), bp.getY(), bp.getZ());
                        if (blockData.getBlock() != Blocks.AIR) {
                            Location location = new Location(chunkPosition.getWorld(), (chunkCoords.x << 4) + bp.getX(), chunkSection.getYPosition() + bp.getY(), (chunkCoords.z << 4) + bp.getZ());
                            Material type = CraftMagicNumbers.getMaterial(blockData.getBlock());
                            Key blockKey = Key.of(type.name(), location);
                            blockCounts.put(blockKey, blockCounts.getOrDefault(blockKey, 0) + 1);
                            if (type == Material.SPAWNER) {
                                spawnersLocations.add(location);
                            }
                        }
                    }
                }
            }

            completableFuture.complete(new BiPair<>(chunkPosition, blockCounts, spawnersLocations));
        }, null);

        return completableFuture;
    }

    @Override
    public void deleteChunk(Island island, ChunkPosition chunkPosition) {
        ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(chunkPosition.getX(), chunkPosition.getZ());
        runActionOnChunk(chunkPosition.getWorld(), chunkCoords, true, chunk -> {
            Arrays.fill(chunk.getSections(), Chunk.a);

            if(chunk instanceof Chunk) {
                Arrays.fill(((Chunk) chunk).entitySlices, new UnsafeList<>());

                new HashSet<>(((Chunk) chunk).tileEntities.keySet()).forEach(((Chunk) chunk).world::n);
                ((Chunk) chunk).tileEntities.clear();
            }
            else{
                ((ProtoChunk) chunk).r().clear();
                ((ProtoChunk) chunk).s().clear();
            }

            ChunksTracker.markEmpty(island, chunkPosition, false);
        }, chunk -> refreshChunk(chunk.bukkitChunk));
    }

    @Override
    public void setChunkBiome(ChunkPosition chunkPosition, Biome biome, List<Player> playersToUpdate) {
        ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(chunkPosition.getX(), chunkPosition.getZ());
        runActionOnChunk(chunkPosition.getWorld(), chunkCoords, true, chunk -> {
            BiomeBase biomeBase = CraftBlock.biomeToBiomeBase(biome);
            Arrays.fill(chunk.getBiomeIndex(), biomeBase);
            if(chunk instanceof Chunk)
                ((Chunk) chunk).markDirty();
        },
        chunk -> {
            PacketPlayOutUnloadChunk unloadChunkPacket = new PacketPlayOutUnloadChunk(chunkCoords.x, chunkCoords.z);
            PacketPlayOutMapChunk mapChunkPacket = new PacketPlayOutMapChunk(chunk, 65535);

            playersToUpdate.forEach(player -> {
                PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
                playerConnection.sendPacket(unloadChunkPacket);
                playerConnection.sendPacket(mapChunkPacket);
            });
        });
    }

    private void runActionOnChunk(org.bukkit.World bukkitWorld, ChunkCoordIntPair chunkCoords, boolean saveChunk, Consumer<IChunkAccess> chunkConsumer, Consumer<Chunk> updateChunk){
        WorldServer world = ((CraftWorld) bukkitWorld).getHandle();
        IChunkLoader chunkLoader = chunkLoadersMap.computeIfAbsent(bukkitWorld.getUID(), uuid -> CHUNK_LOADER.get(world.getChunkProvider()));

        Chunk chunk = world.getChunkIfLoaded(chunkCoords.x, chunkCoords.z);

        if(chunk != null){
            chunkConsumer.accept(chunk);
            if(updateChunk != null)
                updateChunk.accept(chunk);
        }

        else{
            Executor.async(() -> {
                try{
                    ProtoChunk protoChunk = chunkLoader.b(world, chunkCoords.x, chunkCoords.z, null);
                    chunkConsumer.accept(protoChunk);
                    if(saveChunk)
                        chunkLoader.saveChunk(world, protoChunk);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            });
        }
    }

    @Override
    public int tickIslands(int random) {
        List<Pair<Island, List<org.bukkit.Chunk>>> activeChunks = new ArrayList<>();
        List<BiPair<WorldServer, BlockPosition, IBlockData>> blocksToTick = new ArrayList<>();
        org.bukkit.World normalWorld = plugin.getGrid().getIslandsWorld(org.bukkit.World.Environment.NORMAL),
                netherWorld = plugin.getGrid().getIslandsWorld(org.bukkit.World.Environment.NETHER),
                endWorld = plugin.getGrid().getIslandsWorld(org.bukkit.World.Environment.THE_END);
        int[] globalRandomTickSpeeds = new int[] {
                normalWorld == null ? 0 : ((CraftWorld) normalWorld).getHandle().getGameRules().c("randomTickSpeed"),
                netherWorld == null ? 0 : ((CraftWorld) netherWorld).getHandle().getGameRules().c("randomTickSpeed"),
                endWorld == null ? 0 : ((CraftWorld) endWorld).getHandle().getGameRules().c("randomTickSpeed")
        };

        plugin.getGrid().getIslands().stream()
                .filter(island -> island.getCropGrowthMultiplier() > 1 && !island.getAllPlayersInside().isEmpty())
                .forEach(island -> activeChunks.add(new Pair<>(island, island.getLoadedChunks(true, true))));

        for(Pair<Island, List<org.bukkit.Chunk>> chunkPair : activeChunks){
            Island island = chunkPair.getKey();
            double islandCropGrowthMultiplier = island == null ? 0 : island.getCropGrowthMultiplier() - 1;

            for(org.bukkit.Chunk bukkitChunk : chunkPair.getValue()) {
                Chunk chunk = ((CraftChunk) bukkitChunk).getHandle();
                WorldServer chunkWorld = (WorldServer) chunk.world;
                int chunkRandomTickSpeed = (int) (globalRandomTickSpeeds[chunkWorld.getWorld().getEnvironment().ordinal()] * islandCropGrowthMultiplier);

                int chunkX = chunk.locX * 16;
                int chunkZ = chunk.locZ * 16;

                if (chunkRandomTickSpeed > 0) {
                    for (ChunkSection chunkSection : chunk.getSections()) {
                        if (chunkSection != Chunk.a && chunkSection.b()) {
                            for (int i = 0; i < chunkRandomTickSpeed; i++) {
                                random = random * 3 + 1013904223;
                                int factor = random >> 2;
                                int x = factor & 15;
                                int z = factor >> 8 & 15;
                                int y = factor >> 16 & 15;
                                IBlockData blockData = chunkSection.getType(x, y, z);
                                if (blockData.t() && plugin.getSettings().cropsToGrow.contains(CraftMagicNumbers.getMaterial(blockData.getBlock()).name())) {
                                    blocksToTick.add(new BiPair<>(chunkWorld, new BlockPosition(x + chunkX, y + chunkSection.getYPosition(), z + chunkZ), blockData));
                                }
                            }
                        }
                    }
                }
            }
        }

        Executor.sync(() -> blocksToTick.forEach(pair -> {
            Block block = pair.getZ().getBlock();
            RANDOM_TICK.set(block, true);
            pair.getZ().b(pair.getX(), pair.getY(), ThreadLocalRandom.current());
            RANDOM_TICK.set(block, false);
        }));

        return random;
    }

    @Override
    public void handleSignPlace(Island island, Location location) {
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        TileEntitySign tileEntitySign = (TileEntitySign) worldServer.getTileEntity(blockPosition);
        String[] lines = new String[4];
        System.arraycopy(CraftSign.revertComponents(tileEntitySign.lines), 0, lines, 0, lines.length);
        for(int i = 0; i < 4; i++)
            lines[i] = StringUtils.stripColors(lines[i]);
        BlocksListener.IMP.onSignPlace(island.getOwner(), island, location, lines, false);
        IChatBaseComponent[] newLines = CraftSign.sanitizeLines(lines);
        System.arraycopy(newLines, 0, tileEntitySign.lines, 0, 4);
    }

    @Override
    public Material getMaterial(int combinedId) {
        return CraftMagicNumbers.getMaterial(Block.getByCombinedId(combinedId).getBlock());
    }

    @Override
    public byte getData(int combinedId) {
        return 0;
    }

    @Override
    public Key getMinecartBlock(Minecart minecart) {
        return Key.of(minecart.getDisplayBlockData().getMaterial(), (byte) 0);
    }

}
