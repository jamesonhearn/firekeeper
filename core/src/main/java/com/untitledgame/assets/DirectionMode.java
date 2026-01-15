package com.untitledgame.assets;

/**
 * Defines the number of directional animations supported by a sprite sheet.
 * This allows flexibility in art assets - not all sprite sheets need 8 directions.
 */
public enum DirectionMode {
    /**
     * 4 cardinal directions: UP, DOWN, LEFT, RIGHT
     * Used for simpler sprite sheets with only cardinal direction animations.
     */
    FOUR_DIRECTIONAL(4),

    /**
     * 3 directions with mirroring: UP, DOWN, RIGHT (LEFT is mirrored from RIGHT)
     * Used for sprite sheets that only provide right-facing sprites, with left automatically mirrored.
     */
    THREE_DIRECTIONAL_MIRRORED(3),

    /**
     * 8 directions: UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
     * Used for full 8-directional sprite sheets.
     */
    EIGHT_DIRECTIONAL(8);

    private final int directionCount;

    DirectionMode(int directionCount) {
        this.directionCount = directionCount;
    }

    /**
     * Get the number of directions (rows) in the sprite sheet.
     */
    public int getDirectionCount() {
        return directionCount;
    }
}