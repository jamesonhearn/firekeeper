package com.untitledgame.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum Direction {
    // Cardinal directions
    UP(0, 1),
    DOWN(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    // Diagonal directions
    UP_RIGHT(1, 1),
    UP_LEFT(-1, 1),
    DOWN_RIGHT(1, -1),
    DOWN_LEFT(-1, -1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return this.dx;
    }
    public int getDy() {
        return this.dy;
    }
    
    /**
     * Check if this is a cardinal direction (not diagonal).
     */
    public boolean isCardinal() {
        return this == UP || this == DOWN || this == LEFT || this == RIGHT;
    }
    
    /**
     * Get all cardinal directions only (for NPC pathfinding that moves tile-to-tile).
     */
    public static Direction[] cardinalDirections() {
        return new Direction[]{UP, DOWN, LEFT, RIGHT};
    }
    
    /**
     * Calculate the direction from a velocity vector.
     * Returns the closest of 8 directions based on the angle.
     * @param vx velocity X component
     * @param vy velocity Y component
     * @return the closest Direction, or DOWN if velocity is zero
     */
//    public static Direction fromVelocity(double vx, double vy) {
//        // If no movement, return a default direction
//        if (Math.abs(vx) < 1e-6 && Math.abs(vy) < 1e-6) {
//            return DOWN;
//        }
//
//        // Calculate angle in radians (-PI to PI)
//        double angle = Math.atan2(vy, vx);
//
//        // Convert to degrees and normalize to 0-360
//        double degrees = Math.toDegrees(angle);
//        if (degrees < 0) {
//            degrees += 360;
//        }
//
//        // Map angle to 8 directions
//        // East(RIGHT)=0°, North(UP)=90°, West(LEFT)=180°, South(DOWN)=270°
//        // Divide circle into 8 slices of 45° each
//
//        // Add 22.5 degrees offset so each direction occupies a 45° slice centered on it
//        double adjusted = (degrees + 22.5) % 360;
//        int octant = (int)(adjusted / 45.0);
//
//        return switch(octant) {
//            case 0 -> RIGHT;           // 337.5° - 22.5° (East)
//            case 1 -> UP_RIGHT;        // 22.5° - 67.5° (Northeast)
//            case 2 -> UP;              // 67.5° - 112.5° (North)
//            case 3 -> UP_LEFT;         // 112.5° - 157.5° (Northwest)
//            case 4 -> LEFT;            // 157.5° - 202.5° (West)
//            case 5 -> DOWN_LEFT;       // 202.5° - 247.5° (Southwest)
//            case 6 -> DOWN;            // 247.5° - 292.5° (South)
//            case 7 -> DOWN_RIGHT;      // 292.5° - 337.5° (Southeast)
//            default -> DOWN;
//        };
//    }

    public static Direction fromVelocity(double vx, double vy) {
        // If no movement, return a default direction
        if (Math.abs(vx) < 1e-6 && Math.abs(vy) < 1e-6) {
            return DOWN;
        }

        // angle in degrees [0, 360)
        double degrees = Math.toDegrees(Math.atan2(vy, vx));
        if (degrees < 0) degrees += 360;

        // 4 directions => 90° each.
        // Add 45° so boundaries are centered on RIGHT/UP/LEFT/DOWN
        double adjusted = (degrees + 45.0) % 360.0;
        int quadrant = (int)(adjusted / 90.0);

        return switch (quadrant) {
            case 0 -> RIGHT; // 315°..45° (center 0°)
            case 1 -> UP;    // 45°..135° (center 90°)
            case 2 -> LEFT;  // 135°..225° (center 180°)
            case 3 -> DOWN;  // 225°..315° (center 270°)
            default -> DOWN;
        };
    }
    
    /**
     * Get a random cardinal direction (UP, DOWN, LEFT, RIGHT).
     * Returns only cardinal directions for backward compatibility with pathfinding AI.
     * NPCs use cardinal directions for pathfinding but can animate in 8 directions.
     */
    public static Direction random(Random rng) {
        Direction[] values = cardinalDirections();
        return values[rng.nextInt(values.length)];
    }

    /**
     * Get a shuffled list of cardinal directions.
     * Returns only cardinal directions for backward compatibility with pathfinding AI.
     */
    public static List<Direction> shuffled(Random rng) {
        List<Direction> dirs = new ArrayList<>(Arrays.asList(cardinalDirections()));
        Collections.shuffle(dirs, rng);
        return dirs;
    }
}
