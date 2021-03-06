package com.bgsoftware.superiorskyblock.raiding.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Set;

public class IslandCopier {

    private World destinationWorld;
    private Set<Location> source;
    private Set<Location> destination;

    public IslandCopier(Set<Location> source, Set<Location> destination) {
        this.source = source;
        this.destination = destination;
    }

    public void setDestinationWorld(World world) {
        destinationWorld = world;
    }

    public void copy() {
        //TODO Implementation
    }
}
