package com.bgsoftware.superiorskyblock.raiding.handlers;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.raiding.SuperiorRaid;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

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
        raids.remove(superiorRaid);

    }

    public void forceEndRaid(SuperiorRaid superiorRaid, SuperiorPlayer looser){
        raids.remove(superiorRaid);

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