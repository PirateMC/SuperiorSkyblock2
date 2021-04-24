package com.bgsoftware.superiorskyblock.raiding;

public enum Direction {
    NORTH(Math.toRadians(0)),
    SOUTH(Math.toRadians(180));

    private double radians;

    Direction(double radians) {
        this.radians = radians;
    }

    public double getRadians() {
        return radians;
    }
}
