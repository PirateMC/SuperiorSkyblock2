package com.bgsoftware.superiorskyblock.raid;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class SuperiorRaid {

    private final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
    private final List<SuperiorPlayer> teamOnePlayers;
    private final List<SuperiorPlayer> teamTwoPlayers;
    private boolean started = false;

    public SuperiorRaid(List<SuperiorPlayer> teamOnePlayers, List<SuperiorPlayer> teamTwoPlayers){
        this.teamOnePlayers = teamOnePlayers;
        this.teamTwoPlayers = teamTwoPlayers;
    }

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

    public List<SuperiorPlayer> getTeamOnePlayers() {
        return teamOnePlayers;
    }

    public List<SuperiorPlayer> getTeamTwoPlayers() {
        return teamTwoPlayers;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isStarted() {
        return started;
    }
}