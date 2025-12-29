package com.untitledgame.assets;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.logic.Direction;

import java.util.*;

public final class Tileset {
    private Tileset() {
    }

    // Tile constants for compatibility with old rendering code
    public static TETile FLOOR;
    public static TETile SKULL_FLOOR;

    public static TETile ELEVATOR;
    public static TETile WALL_TOP;
    public static TETile SKULL_WALL_SIDE;
    public static TETile SKULL_WALL_TOP;
    public static TETile WALL_SIDE;
    public static TETile LEFT_WALL;
    public static TETile RIGHT_WALL;
    public static TETile FRONT_WALL_TOP;
    public static TETile BACK_WALL;
    public static TETile LOOT_BAG;
    public static TETile NPC_CORPSE;

    // Avatar animation frames (TextureRegions resolved from atlas)
    // Cardinal directions
    public static TextureRegion[] AVATAR_WALK_UP;
    public static TextureRegion[] AVATAR_WALK_DOWN;
    public static TextureRegion[] AVATAR_WALK_LEFT;
    public static TextureRegion[] AVATAR_WALK_RIGHT;
    // Diagonal directions
    public static TextureRegion[] AVATAR_WALK_UP_RIGHT;
    public static TextureRegion[] AVATAR_WALK_UP_LEFT;
    public static TextureRegion[] AVATAR_WALK_DOWN_RIGHT;
    public static TextureRegion[] AVATAR_WALK_DOWN_LEFT;

    public static TextureRegion[] AVATAR_ATTACK_UP;
    public static TextureRegion[] AVATAR_ATTACK_DOWN;
    public static TextureRegion[] AVATAR_ATTACK_LEFT;
    public static TextureRegion[] AVATAR_ATTACK_RIGHT;
    public static TextureRegion[] AVATAR_ATTACK_UP_RIGHT;
    public static TextureRegion[] AVATAR_ATTACK_UP_LEFT;
    public static TextureRegion[] AVATAR_ATTACK_DOWN_RIGHT;
    public static TextureRegion[] AVATAR_ATTACK_DOWN_LEFT;

    public static TextureRegion[] AVATAR_DEATH_UP;
    public static TextureRegion[] AVATAR_DEATH_DOWN;
    public static TextureRegion[] AVATAR_DEATH_LEFT;
    public static TextureRegion[] AVATAR_DEATH_RIGHT;
    public static TextureRegion[] AVATAR_DEATH_UP_RIGHT;
    public static TextureRegion[] AVATAR_DEATH_UP_LEFT;
    public static TextureRegion[] AVATAR_DEATH_DOWN_RIGHT;
    public static TextureRegion[] AVATAR_DEATH_DOWN_LEFT;

    private static TextureAtlas textureAtlas;

    private static boolean initialized;

    public static void initialize(TextureAtlas atlas) {
        if (initialized) {
            return;
        }
        textureAtlas = atlas;
        Objects.requireNonNull(atlas, "atlas must be provided to initialize tiles");
        FLOOR = tile(atlas, "tiles/skull_floor", "floor", 0);
        ELEVATOR = tile(atlas, "tiles/elevator", "elevator", 1);
        WALL_TOP = tile(atlas, "tiles/top_skull", "wall top", 2);
        WALL_SIDE = tile(atlas, "tiles/cave_wall_base", "wall side", 3);
        LEFT_WALL = tile(atlas, "tiles/cave_wall_left", "left wall", 4);
        RIGHT_WALL = tile(atlas, "tiles/cave_wall_right", "right wall", 5);
        FRONT_WALL_TOP = tile(atlas, "tiles/cave_wall_top", "front wall top", 6);
        BACK_WALL = tile(atlas, "tiles/cave_wall_base", "back wall", 7);
        LOOT_BAG = tile(atlas, "tiles/test", "loot", 8);
        NPC_CORPSE = tile(atlas, "tiles/cave_floor_1", "corpse", 9);


        // Load avatar animations from 8-directional sprite sheets
        // The new sprite sheets use format: player_{animation}_{row}_{frame}
        // Rows: 0=RIGHT, 1=DOWN_RIGHT, 2=DOWN, 3=DOWN_LEFT, 4=LEFT, 5=UP_LEFT, 6=UP, 7=UP_RIGHT
        
        // Walk animations - all 8 directions
        AVATAR_WALK_UP = loadAnimationFrames(atlas, "player", "walk", Direction.UP, 15);
        AVATAR_WALK_DOWN = loadAnimationFrames(atlas, "player", "walk", Direction.DOWN, 15);
        AVATAR_WALK_LEFT = loadAnimationFrames(atlas, "player", "walk", Direction.LEFT, 15);
        AVATAR_WALK_RIGHT = loadAnimationFrames(atlas, "player", "walk", Direction.RIGHT, 15);
        AVATAR_WALK_UP_RIGHT = loadAnimationFrames(atlas, "player", "walk", Direction.UP_RIGHT, 15);
        AVATAR_WALK_UP_LEFT = loadAnimationFrames(atlas, "player", "walk", Direction.UP_LEFT, 15);
        AVATAR_WALK_DOWN_RIGHT = loadAnimationFrames(atlas, "player", "walk", Direction.DOWN_RIGHT, 15);
        AVATAR_WALK_DOWN_LEFT = loadAnimationFrames(atlas, "player", "walk", Direction.DOWN_LEFT, 15);

        // Attack animations - all 8 directions
        AVATAR_ATTACK_UP = loadAnimationFrames(atlas, "player", "melee", Direction.UP, 15);
        AVATAR_ATTACK_DOWN = loadAnimationFrames(atlas, "player", "melee", Direction.DOWN, 15);
        AVATAR_ATTACK_LEFT = loadAnimationFrames(atlas, "player", "melee", Direction.LEFT, 15);
        AVATAR_ATTACK_RIGHT = loadAnimationFrames(atlas, "player", "melee", Direction.RIGHT, 15);
        AVATAR_ATTACK_UP_RIGHT = loadAnimationFrames(atlas, "player", "melee", Direction.UP_RIGHT, 15);
        AVATAR_ATTACK_UP_LEFT = loadAnimationFrames(atlas, "player", "melee", Direction.UP_LEFT, 15);
        AVATAR_ATTACK_DOWN_RIGHT = loadAnimationFrames(atlas, "player", "melee", Direction.DOWN_RIGHT, 15);
        AVATAR_ATTACK_DOWN_LEFT = loadAnimationFrames(atlas, "player", "melee", Direction.DOWN_LEFT, 15);

        // Death animations - all 8 directions
        AVATAR_DEATH_UP = loadAnimationFrames(atlas, "player", "die", Direction.UP, 15);
        AVATAR_DEATH_DOWN = loadAnimationFrames(atlas, "player", "die", Direction.DOWN, 15);
        AVATAR_DEATH_LEFT = loadAnimationFrames(atlas, "player", "die", Direction.LEFT, 15);
        AVATAR_DEATH_RIGHT = loadAnimationFrames(atlas, "player", "die", Direction.RIGHT, 15);
        AVATAR_DEATH_UP_RIGHT = loadAnimationFrames(atlas, "player", "die", Direction.UP_RIGHT, 15);
        AVATAR_DEATH_UP_LEFT = loadAnimationFrames(atlas, "player", "die", Direction.UP_LEFT, 15);
        AVATAR_DEATH_DOWN_RIGHT = loadAnimationFrames(atlas, "player", "die", Direction.DOWN_RIGHT, 15);
        AVATAR_DEATH_DOWN_LEFT = loadAnimationFrames(atlas, "player", "die", Direction.DOWN_LEFT, 15);

        initialized = true;
    }

    public static TETile[] loadNpcSpriteSet(int variant) {
        List<TETile> frames = new ArrayList<>();

        int row = getDirectionRow(Direction.DOWN); // south-facing

        for (int frame = 0; ; frame++) {
            String key = "npc_walk_" + row + "_" + frame;

            // Stop when the atlas no longer contains this frame
            if (textureAtlas == null || textureAtlas.findRegion(key) == null) {
                break;
            }

            TextureRegion region = textureAtlas.findRegion(key);
            frames.add(TETile.fromRegion("npc", region, key, 200 + variant * 100 + frame));
        }

        return frames.toArray(new TETile[0]);
    }

    private static TETile tile(TextureAtlas atlas, String key, String description, int id) {
        TextureRegion region = atlas.findRegion(key);
        return TETile.fromRegion(description, region, key, id);
    }

    /**
     * Get the sprite sheet row index for 8-directional movement in sprite sheets.
     * The sprite sheets have 8 rows starting with E (row 0) and going clockwise.
     * 
     * Row mapping (clockwise from East):
     * 0 = East (RIGHT)
     * 1 = Southeast (DOWN_RIGHT)
     * 2 = South (DOWN)
     * 3 = Southwest (DOWN_LEFT)
     * 4 = West (LEFT)
     * 5 = Northwest (UP_LEFT)
     * 6 = North (UP)
     * 7 = Northeast (UP_RIGHT)
     *
     * @param direction The game direction (8 directions)
     * @return The row index in the sprite sheet (0-7)
     */
    public static int getDirectionRow(Direction direction) {
        return switch (direction) {
            case RIGHT -> 0;
            case DOWN_RIGHT -> 1;
            case DOWN -> 2;
            case DOWN_LEFT -> 3;
            case LEFT -> 4;
            case UP_LEFT -> 5;
            case UP -> 6;
            case UP_RIGHT -> 7;
        };
    }


    private static String directionKey(Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            case LEFT -> "left";
            case RIGHT -> "right";
            case UP_RIGHT -> "up_right";
            case UP_LEFT -> "up_left";
            case DOWN_RIGHT -> "down_right";
            case DOWN_LEFT -> "down_left";
        };
    }
    /**
     * Generate a key for accessing frames from multi-directional sprite sheets.
     * Format: characterType_animationType_row_frame
     *
     * @param characterType "player" or "npc"
     * @param animationType e.g., "idle", "walk", "melee"
     * @param direction The game direction
     * @param frame The frame index
     * @return The atlas key for the frame
     */
    public static String getAnimationFrameKey(String characterType, String animationType, Direction direction, int frame) {
        int row = getDirectionRow(direction);
        return characterType + "_" + animationType + "_" + row + "_" + frame;
    }


    /**
     * Load frames for a specific animation and direction from the new sprite sheets.
     *
     * @param atlas The texture atlas
     * @param characterType "player" or "npc"
     * @param animationType e.g., "idle", "walk", "melee"
     * @param direction The game direction
     * @param maxFrames Maximum number of frames to attempt to load
     * @return Array of texture regions for the animation
     */
    public static TextureRegion[] loadAnimationFrames(TextureAtlas atlas, String characterType,
                                                      String animationType, Direction direction, int maxFrames) {
        List<TextureRegion> frames = new ArrayList<>();
        int row = getDirectionRow(direction);

        for (int i = 0; i < maxFrames; i++) {
            String key = characterType + "_" + animationType + "_" + row + "_" + i;
            TextureRegion region = atlas.findRegion(key);
            if (region != null) {
                frames.add(region);
            } else {
                // Stop when we can't find more frames
                break;
            }
        }

        return frames.toArray(new TextureRegion[0]);
    }

}
