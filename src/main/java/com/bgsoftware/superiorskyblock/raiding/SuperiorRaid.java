package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SuperiorRaid {

    private final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    private List<SuperiorPlayer> teamOnePlayers, teamTwoPlayers;
    private Location teamOneLocation, teamTwoLocation;
    private boolean started = false;

    public void startRaid(){

        new BukkitRunnable(){

            int countdown = plugin.getSettings().raidStartCountdown;

            @Override
            public void run() {

                if (countdown == 0){

                    teamOnePlayers.stream()
                            .filter(SuperiorPlayer::isOnline)
                            .map(SuperiorPlayer::asPlayer)
                            .forEach(player -> {
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0);

                                player.playSound(player.getLocation(), plugin.getSettings().raidStartSound, 1, 1);
                            });

                    teamTwoPlayers.stream()
                            .filter(SuperiorPlayer::isOnline)
                            .map(SuperiorPlayer::asPlayer)
                            .forEach(player -> {
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0);

                                player.playSound(player.getLocation(), plugin.getSettings().raidStartSound, 1, 1);
                            });

                    started = true;
                    cancel();
                }

                teamOnePlayers.stream()
                        .filter(SuperiorPlayer::isOnline)
                        .map(SuperiorPlayer::asPlayer)
                        .forEach(player ->
                                plugin.getNMSAdapter().sendTitle(player,
                                        Locale.RAID_START_COUNTDOWN.getMessage(LocaleUtils.getLocale(player), countdown),
                                        "", 0, 20, 0));

                teamTwoPlayers.stream()
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

    public void handleRespawn(SuperiorPlayer superiorPlayer){
        SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

        Location teleportLocation = teamOnePlayers.contains(superiorPlayer) ?
                teamOneLocation : teamTwoLocation;

        superiorPlayer.asPlayer().teleport(teleportLocation);

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

    public Location getSpawnLocation(SuperiorPlayer superiorPlayer){
        return teamOnePlayers.contains(superiorPlayer) ?
                teamOneLocation : teamTwoLocation;
    }

    public void setTeamOnePlayers(List<SuperiorPlayer> teamOnePlayers) {
        this.teamOnePlayers = teamOnePlayers;
    }

    public List<SuperiorPlayer> getTeamOnePlayers() {
        return teamOnePlayers;
    }

    public void setTeamTwoPlayers(List<SuperiorPlayer> teamTwoPlayers) {
        this.teamTwoPlayers = teamTwoPlayers;
    }

    public List<SuperiorPlayer> getTeamTwoPlayers() {
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

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isStarted() {
        return started;
    }
}