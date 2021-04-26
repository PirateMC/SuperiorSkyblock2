package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public final class RaidCooldownHandler {

    private static final int timerDelayInTicks = 20;
    private static Set<RaidCooldown> raidCooldowns = new HashSet<>();
    private static BukkitTask timer;

    public static void addNewCooldown(Island island, CooldownType cooldownType) {
        raidCooldowns.add(new RaidCooldown(island.getUniqueId(), cooldownType.getDuration()));
    }

    public static boolean isCoolingDown(Island island) {
        return raidCooldowns.stream()
                .anyMatch(cooldown -> cooldown.getIslandUuid().equals(island.getUniqueId()));
    }

    public static long getCooldownOf(Island island) {
        return raidCooldowns.stream()
                .filter(cooldown -> cooldown.getIslandUuid().equals(island.getUniqueId()))
                .findAny()
                .get()
                .getDuration();
    }

    public static void startTimer() {
        timer = Bukkit.getScheduler().runTaskTimer(SuperiorSkyblockPlugin.getPlugin(), new BukkitRunnable() {
            @Override
            public void run() {
                raidCooldowns.forEach(RaidCooldown::decrement);
                raidCooldowns.removeIf(RaidCooldown::hasExpired);
            }
        }, timerDelayInTicks, timerDelayInTicks);
    }

    public static void stopTimer() {
        timer.cancel();
    }
}
