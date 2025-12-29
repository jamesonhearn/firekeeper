package com.untitledgame.assets;

public enum TileType {
    NOTHING("nothing"),
    FLOOR("floor"),
    WALL_SIDE("wall side"),
    WALL_TOP("wall top"),
    LEFT_WALL("left wall"),
    BACK_WALL("back wall"),
    ELEVATOR("elevator");

    private final String description;

    TileType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
