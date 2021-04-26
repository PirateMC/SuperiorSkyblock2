package com.bgsoftware.superiorskyblock.raiding.queue;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.raiding.SuperiorRaid;
import com.bgsoftware.superiorskyblock.raiding.island.RaidSlot;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RaidQueue {

    private final Queue<RaidQueueEntry> raidQueue = new ConcurrentLinkedQueue<>();

    public RaidQueue() {
        Bukkit.getScheduler().runTaskTimer(SuperiorSkyblockPlugin.getPlugin(), () -> {
            if (!raidQueue.isEmpty() && !SuperiorSkyblockPlugin.getPlugin().getRaidIslandManager().isGeneratingSlot()) {
                startRaid(raidQueue.element());
            } else {
                SuperiorSkyblockPlugin.raidDebug("Currently generating island.");
            }
        }, 20, 20);
    }

    public RaidQueueAddResult add(RaidQueueEntry entry) {
        if (!raidQueue.contains(entry))
            return new RaidQueueAddResult(raidQueue.add(entry), raidQueue.size());
        return new RaidQueueAddResult(false, raidQueue.size());
    }

    public boolean contains(RaidQueueEntry entry) {
        return raidQueue.contains(entry);
    }

    public void remove() {
        if (!raidQueue.isEmpty())
            raidQueue.remove();
    }

    private void startRaid(RaidQueueEntry raidQueueEntry) {
        CommandSender commandSender = Bukkit.getPlayer(raidQueueEntry.getKey());
        CommandSender invitationSender = Bukkit.getPlayer(raidQueueEntry.getValue());
        SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();
        Island teamOneIsland = plugin.getGrid().getIsland(raidQueueEntry.getKey());
        Island teamTwoIsland = plugin.getGrid().getIsland(raidQueueEntry.getValue());

        Map<SuperiorPlayer, ItemStack[]> teamOneMembers = new HashMap<>(), teamTwoMembers = new HashMap<>();

        for (SuperiorPlayer superiorPlayer : teamOneIsland.getIslandMembers(true)) {
            if (!superiorPlayer.isOnline()) continue;

            teamOneMembers.put(superiorPlayer, superiorPlayer.asPlayer().getInventory().getContents());
        }

        for (SuperiorPlayer superiorPlayer : teamTwoIsland.getIslandMembers(true)) {
            if (!superiorPlayer.isOnline()) continue;

            teamTwoMembers.put(superiorPlayer, superiorPlayer.asPlayer().getInventory().getContents());
        }

        //TODO Return island generation time estimation based on island size
        RaidSlot raidSlot = plugin.getRaidIslandManager().generateNewRaidSlotAsynchronously(teamOneIsland, teamTwoIsland);

        //TODO Move messages to lang file
        sendMessageToMultiple(ChatColor.YELLOW + "Generating raid islands...", commandSender, invitationSender);
        Bukkit.getScheduler().runTaskAsynchronously(SuperiorSkyblockPlugin.getPlugin(), () -> {
            for (int i = 3; i > 0; i--) {
                int timeLeft = i * 10;
                sendMessageToMultiple(String.format(ChatColor.YELLOW + "Raid starting in %d seconds....", timeLeft), commandSender, invitationSender);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Teleport players to the raid island in 30 seconds to
        // allow raid islands to finish generating.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teamOneMembers.keySet().forEach(member -> member.teleport(raidSlot.getFirstIslandTeleportLocation()));
            teamTwoMembers.keySet().forEach(member -> member.teleport(raidSlot.getSecondIslandTeleportLocation()));
            SuperiorRaid superiorRaid = new SuperiorRaid();
            superiorRaid.setTeamOnePlayers(teamOneMembers);
            superiorRaid.setTeamTwoPlayers(teamTwoMembers);
            superiorRaid.setTeamOneLocation(raidSlot.getFirstIslandTeleportLocation().clone());
            superiorRaid.setTeamTwoLocation(raidSlot.getSecondIslandTeleportLocation().clone());
            superiorRaid.setTeamOneMinLocation(raidSlot.getFirstIslandTeleportLocation().clone().add(-teamOneIsland.getIslandSize(), -teamOneIsland.getIslandSize(), -teamOneIsland.getIslandSize()));
            superiorRaid.setTeamOneMaxLocation(raidSlot.getFirstIslandTeleportLocation().clone().add(teamOneIsland.getIslandSize(), teamOneIsland.getIslandSize(), teamOneIsland.getIslandSize()));
            superiorRaid.setTeamTwoMinLocation(raidSlot.getSecondIslandTeleportLocation().clone().add(-teamTwoIsland.getIslandSize(), -teamTwoIsland.getIslandSize(), -teamTwoIsland.getIslandSize()));
            superiorRaid.setTeamTwoMaxLocation(raidSlot.getSecondIslandTeleportLocation().clone().add(teamTwoIsland.getIslandSize(), teamTwoIsland.getIslandSize(), teamTwoIsland.getIslandSize()));
            plugin.getRaidsHandler().startRaid(superiorRaid);
            remove();
        }, 20 * 30);
    }

    private void sendMessageToMultiple(String message, CommandSender commandSender, CommandSender... senders) {
        commandSender.sendMessage(message);
        Arrays.stream(senders).forEach(sender -> sender.sendMessage(message));
    }
}



