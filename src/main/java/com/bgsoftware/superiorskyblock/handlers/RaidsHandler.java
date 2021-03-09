package com.bgsoftware.superiorskyblock.handlers;

import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.raid.SuperiorRaid;

import java.util.ArrayList;
import java.util.List;

public class RaidsHandler {

    private final List<SuperiorRaid> raids = new ArrayList<>();

    public void startRaid(List<SuperiorPlayer> teamOnePlayers, List<SuperiorPlayer> teamTwoPlayers){
        SuperiorRaid superiorRaid = new SuperiorRaid(teamOnePlayers, teamTwoPlayers);

        raids.add(superiorRaid);

        superiorRaid.startRaid();
    }

    public SuperiorRaid getRaidByMember(SuperiorPlayer superiorPlayer){
        return raids.stream()
                .filter(raid -> raid.getTeamOnePlayers().contains(superiorPlayer) || raid.getTeamTwoPlayers().contains(superiorPlayer))
                .findFirst().orElse(null);
    }
}