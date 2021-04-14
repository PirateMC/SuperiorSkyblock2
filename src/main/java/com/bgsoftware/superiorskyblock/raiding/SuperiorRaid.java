package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.key.Key;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Map;

public class SuperiorRaid {

    private final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    private Map<SuperiorPlayer, ItemStack[]> teamOnePlayers, teamTwoPlayers;
    private Location teamOneLocation, teamTwoLocation, teamOneMaxLocation, teamOneMinLocation, teamTwoMaxLocation, teamTwoMinLocation;
    private double teamOnePoints = 0, teamTwoPoints = 0;
    private boolean started = false, isOver = false;

    public void startRaid(){

        new BukkitRunnable(){

            int countdown = plugin.getSettings().raidStartCountdown;

            @Override
            public void run() {

                if (countdown == 0){

                    teamOnePlayers.keySet().stream()
                            .filter(SuperiorPlayer::isOnline)
                            .map(SuperiorPlayer::asPlayer)
                            .forEach(player -> {
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0);

                                player.playSound(player.getLocation(), plugin.getSettings().raidStartSound, 1, 1);
                            });

                    teamTwoPlayers.keySet().stream()
                            .filter(SuperiorPlayer::isOnline)
                            .map(SuperiorPlayer::asPlayer)
                            .forEach(player -> {
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0);

                                player.playSound(player.getLocation(), plugin.getSettings().raidStartSound, 1, 1);
                            });

                    SuperiorRaid.this.runGameCountDownTask();

                    started = true;
                    cancel();
                    return;
                }

                teamOnePlayers.keySet().stream()
                        .filter(SuperiorPlayer::isOnline)
                        .map(SuperiorPlayer::asPlayer)
                        .forEach(player ->
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START_COUNTDOWN.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0));

                teamTwoPlayers.keySet().stream()
                        .filter(SuperiorPlayer::isOnline)
                        .map(SuperiorPlayer::asPlayer)
                        .forEach(player ->
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START_COUNTDOWN.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0));

                countdown--;

            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void runGameCountDownTask(){
        new BukkitRunnable(){

            int gameCountdown = plugin.getSettings().raidDuration;

            @Override
            public void run() {

                if (SuperiorRaid.this.isOver) {
                    cancel();
                    return;
                }

                if (gameCountdown == 0){
                    cancel();
                    return;
                }

                teamOnePlayers.keySet().stream()
                        .filter(SuperiorPlayer::isOnline)
                        .map(SuperiorPlayer::asPlayer)
                        .forEach(player ->
                                plugin.getNMSAdapter().sendActionBar(player,
                                        Locale.RAID_COUNTDOWN.getMessage(LocaleUtils.getLocale(player),
                                                String.format("%d:%d", gameCountdown/60, gameCountdown%60))));

                teamTwoPlayers.keySet().stream()
                        .filter(SuperiorPlayer::isOnline)
                        .map(SuperiorPlayer::asPlayer)
                        .forEach(player ->
                                plugin.getNMSAdapter().sendActionBar(player,
                                        Locale.RAID_COUNTDOWN.getMessage(LocaleUtils.getLocale(player),
                                                String.format("%d:%d", gameCountdown/60, gameCountdown%60))));

                gameCountdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public Location getRespawnLocation(SuperiorPlayer superiorPlayer){
        return teamOnePlayers.containsKey(superiorPlayer) ?
                teamOneLocation : teamTwoLocation;
    }

    public void handleRespawn(SuperiorPlayer superiorPlayer){
        SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

        superiorPlayer.asPlayer().setMetadata("Respawning", new FixedMetadataValue(plugin, ""));

        new BukkitRunnable(){
            int countdown = 5;

            @Override
            public void run() {

                if (!superiorPlayer.asPlayer().isOnline()) {
                    superiorPlayer.asPlayer().removeMetadata("Respawning", plugin);
                    cancel();
                    return;
                }

                if (countdown == 0){

                    plugin.getNMSAdapter().sendTitle(superiorPlayer.asPlayer(),
                            Locale.RAID_RESPAWN.getMessage(LocaleUtils.getLocale(superiorPlayer.asPlayer()), countdown),
                            "", 0, 20, 0);

                    superiorPlayer.asPlayer().playSound(superiorPlayer.asPlayer().getLocation(), plugin.getSettings().raidStartSound, 1, 1);

                    superiorPlayer.asPlayer().removeMetadata("Respawning", plugin);
                    cancel();
                }

                plugin.getNMSAdapter().sendTitle(superiorPlayer.asPlayer(),
                        Locale.RAID_RESPAWN_COUNTDOWN.getMessage(LocaleUtils.getLocale(superiorPlayer.asPlayer()), countdown),
                        "", 0, 20, 0);

                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void handleBreak(SuperiorPlayer superiorPlayer, Block block){

        BigDecimal value = plugin.getBlockValues().getBlockLevel(Key.of(block));

        if (value.intValue() == 0) return;

        Location max, min;
        boolean isTeamOne = false;

        if (teamOnePlayers.containsKey(superiorPlayer)){
            max = teamTwoMaxLocation.clone();
            min = teamTwoMinLocation.clone();
            isTeamOne = true;
        } else {
            max = teamOneMaxLocation.clone();
            min = teamOneMinLocation.clone();
        }

        if (block.getLocation().getX() <= max.getX() && block.getLocation().getX() >= min.getX() &&
                block.getLocation().getY() <= max.getY() && block.getLocation().getY() >= min.getY() &&
                block.getLocation().getZ() <= max.getZ() && block.getLocation().getZ() >= min.getZ()){

            if (isTeamOne){
                teamOnePoints += value.doubleValue();
            } else {
                teamTwoPoints += value.doubleValue();
            }
        }
    }

    public Location getSpawnLocation(SuperiorPlayer superiorPlayer){
        return teamOnePlayers.containsKey(superiorPlayer) ?
                teamOneLocation : teamTwoLocation;
    }

    public void setTeamOnePlayers(Map<SuperiorPlayer, ItemStack[]> teamOnePlayers) {
        this.teamOnePlayers = teamOnePlayers;
    }

    public Map<SuperiorPlayer, ItemStack[]> getTeamOnePlayers() {
        return teamOnePlayers;
    }

    public void setTeamTwoPlayers(Map<SuperiorPlayer, ItemStack[]> teamTwoPlayers) {
        this.teamTwoPlayers = teamTwoPlayers;
    }

    public Map<SuperiorPlayer, ItemStack[]> getTeamTwoPlayers() {
        return teamTwoPlayers;
    }

    public void setTeamOneLocation(Location teamOneLocation) {
        this.teamOneLocation = teamOneLocation;
    }

    public Location getTeamOneLocation() {
        return teamOneLocation;
    }

    public void setTeamTwoLocation(Location teamTwoLocation) {
        this.teamTwoLocation = teamTwoLocation;
    }

    public Location getTeamTwoLocation() {
        return teamTwoLocation;
    }

    public void setTeamOneMaxLocation(Location teamOneMaxLocation) {
        this.teamOneMaxLocation = teamOneMaxLocation;
    }

    public Location getTeamOneMaxLocation() {
        return teamOneMaxLocation;
    }

    public void setTeamOneMinLocation(Location teamOneMinLocation) {
        this.teamOneMinLocation = teamOneMinLocation;
    }

    public Location getTeamOneMinLocation() {
        return teamOneMinLocation;
    }

    public void setTeamTwoMaxLocation(Location teamTwoMaxLocation) {
        this.teamTwoMaxLocation = teamTwoMaxLocation;
    }

    public Location getTeamTwoMaxLocation() {
        return teamTwoMaxLocation;
    }

    public void setTeamTwoMinLocation(Location teamTwoMinLocation) {
        this.teamTwoMinLocation = teamTwoMinLocation;
    }

    public Location getTeamTwoMinLocation() {
        return teamTwoMinLocation;
    }

    public double getTeamOnePoints() {
        return teamOnePoints;
    }

    public double getTeamTwoPoints() {
        return teamTwoPoints;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isStarted() {
        return started;
    }

    public void setOver(boolean over) {
        isOver = over;
    }

    public boolean isOver() {
        return isOver;
    }
}