package com.untitledgame.logic.npc;

import com.untitledgame.TETile;

/**
 * Simple marker to keep track of NPC death remnants.
 */
public class Corpse {
    private final int x;
    private final int y;
    private final TETile tileSprite;

    public Corpse(int x, int y, TETile tileSprite) {
        this.x = x;
        this.y = y;
        this.tileSprite = tileSprite;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public TETile tile() {
        return tileSprite;
    }
}
