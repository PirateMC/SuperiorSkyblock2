package com.bgsoftware.superiorskyblock.raiding.handlers;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.key.Key;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.raiding.SuperiorRaid;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class RaidsHandler {

    private final List<SuperiorRaid> raids = new ArrayList<>();

    public void startRaid(SuperiorRaid superiorRaid){
        raids.add(superiorRaid);

        superiorRaid.startRaid();

        new BukkitRunnable(){

            @Override
            public void run() {
                if (!isRaidActive(superiorRaid)) return;

                endRaid(superiorRaid);
            }

        }.runTaskLater(SuperiorSkyblockPlugin.getPlugin(), (SuperiorSkyblockPlugin.getPlugin().getSettings().raidStartCountdown+SuperiorSkyblockPlugin.getPlugin().getSettings().raidDuration)* 20L);

    }

    public void endRaid(SuperiorRaid superiorRaid){
        superiorRaid.setOver(true);
        raids.remove(superiorRaid);

        if (superiorRaid.getTeamOnePoints() > superiorRaid.getTeamTwoPoints()){
            handleEnd(
                    superiorRaid,
                    superiorRaid.getTeamOnePlayers().keySet().iterator().next().getIsland(),
                    superiorRaid.getTeamTwoPlayers().keySet().iterator().next().getIsland(),
                    false);
        } else if (superiorRaid.getTeamOnePoints() == superiorRaid.getTeamTwoPoints()){

            handleEnd(
                    superiorRaid,
                    superiorRaid.getTeamOnePlayers().keySet().iterator().next().getIsland(),
                    superiorRaid.getTeamTwoPlayers().keySet().iterator().next().getIsland(),
                    true);
        } else {
            handleEnd(
                    superiorRaid,
                    superiorRaid.getTeamTwoPlayers().keySet().iterator().next().getIsland(),
                    superiorRaid.getTeamOnePlayers().keySet().iterator().next().getIsland(),
                    false);
        }
    }

    public void forceEndRaid(SuperiorRaid superiorRaid, SuperiorPlayer looser){
        raids.remove(superiorRaid);

        Island loserIsland = superiorRaid.getTeamOnePlayers().containsKey(looser) ?
                superiorRaid.getTeamOnePlayers().keySet().iterator().next().getIsland() :
                superiorRaid.getTeamTwoPlayers().keySet().iterator().next().getIsland();
        Island winnerIsland = superiorRaid.getTeamOnePlayers().containsKey(looser) ?
                superiorRaid.getTeamTwoPlayers().keySet().iterator().next().getIsland() :
                superiorRaid.getTeamOnePlayers().keySet().iterator().next().getIsland();

        handleEnd(superiorRaid, winnerIsland, loserIsland, false);
    }

    private void handleEnd(SuperiorRaid raid, Island winnersIsland, Island losersIsland, boolean isDraw){

        if (isDraw){
            resetMembers(raid, winnersIsland, Locale.RAID_DRAW);
            resetMembers(raid, losersIsland, Locale.RAID_DRAW);
            return;
        }

        resetMembers(raid, winnersIsland, Locale.RAID_WIN);
        resetMembers(raid, losersIsland, Locale.RAID_LOSE);

        SuperiorSkyblockPlugin.getPlugin().getRaidIslandManager().deleteRaidIsland(winnersIsland.getOwner().getUniqueId());
        SuperiorSkyblockPlugin.getPlugin().getRaidIslandManager().deleteRaidIsland(losersIsland.getOwner().getUniqueId());

        List<ItemStack> winnings = breakValuableBlocks(losersIsland);
        losersIsland.calcIslandWorth(null);

        for (Player player : winnersIsland.getIslandMembers(true).stream()
                .filter(SuperiorPlayer::isOnline)
                .map(SuperiorPlayer::asPlayer)
                .collect(Collectors.toList())){

            Collection<ItemStack> items = player.getInventory().addItem(winnings.toArray(new ItemStack[0])).values();
            winnings.clear();
            winnings.addAll(items);

            if (winnings.isEmpty()) break;

        }

        raid.getTeamOnePlayers().clear();
        raid.getTeamTwoPlayers().clear();

    }

    private void resetMembers(SuperiorRaid raid, Island island, Locale locale){
        island.getIslandMembers(true).stream()
                .filter(SuperiorPlayer::isOnline)
                .forEach(player -> {
                    player.asPlayer().getInventory().clear();
                    player.teleport(island.getTeleportLocation(World.Environment.NORMAL));
                    SuperiorSkyblockPlugin.getPlugin().getNMSAdapter().sendTitle(player.asPlayer(),
                            locale.getMessage(LocaleUtils.getLocale(player)),
                            "", 0, 80, 60);

                    if (raid.getTeamOnePlayers().containsKey(player)){
                        player.asPlayer().getInventory().addItem(Arrays.stream(raid.getTeamOnePlayers().get(player))
                                .filter(Objects::nonNull).toArray(ItemStack[]::new));
                    } else {
                        player.asPlayer().getInventory().addItem(Arrays.stream(raid.getTeamTwoPlayers().get(player))
                                .filter(Objects::nonNull).toArray(ItemStack[]::new));
                    }
                });
    }

    private List<ItemStack> breakValuableBlocks(Island island) {
        List<ItemStack> blocksRemoved = new ArrayList<>();
        int valueRemoved = 0;
        int lostValue = island.getIslandLevel().intValue() / 100 * (new Random().nextInt(6) + 5);

        Location islandCenter = island.getCenter(World.Environment.NORMAL);
        int islandSize = island.getIslandSize();
        World world = islandCenter.getWorld();

        CuboidRegion region = CuboidRegion.fromCenter(BlockVector3.at(islandCenter.getX(), islandCenter.getY(), islandCenter.getZ()), islandSize);

        for (int x = region.getMinimumPoint().getX(); x < region.getMaximumPoint().getX(); x++)
            for (int z = region.getMinimumPoint().getZ(); z < region.getMaximumPoint().getZ(); z++)
                for (int y = region.getMinimumPoint().getY(); y < region.getMaximumPoint().getY(); y++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (block.getType() == Material.AIR) continue;

                    int value = SuperiorSkyblockPlugin.getPlugin().getBlockValues().getBlockWorth(Key.of(block)).intValue();

                    if (value == 0) continue;

                    valueRemoved += value;

                    blocksRemoved.add(new ItemStack(block.getType()));
                    block.setType(Material.AIR);

                    if (valueRemoved >= lostValue) return blocksRemoved;
                }

        return blocksRemoved;
    }

    public SuperiorRaid getRaidByOwner(SuperiorPlayer superiorPlayer){
        return raids.stream()
                .filter(raid -> raid.getTeamOnePlayers().keySet().iterator().next().getIsland().getOwner().equals(superiorPlayer) ||
                                raid.getTeamTwoPlayers().keySet().iterator().next().getIsland().getOwner().equals(superiorPlayer))
                .findFirst().orElse(null);
    }

    public SuperiorRaid getRaidByMember(SuperiorPlayer superiorPlayer){
        return raids.stream()
                .filter(raid -> raid.getTeamOnePlayers().containsKey(superiorPlayer) || raid.getTeamTwoPlayers().containsKey(superiorPlayer))
                .findFirst().orElse(null);
    }

    public boolean isRaidActive(SuperiorRaid superiorRaid){
        return raids.contains(superiorRaid);
    }
}