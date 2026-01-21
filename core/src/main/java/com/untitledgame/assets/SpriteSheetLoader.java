package com.untitledgame.assets;


import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;


public class SpriteSheetLoader {


    public static void loadSpriteSheets(AssetManager assetManager, TextureAtlas atlas, List<SpriteSheetConfig> configs) {
        for (SpriteSheetConfig config : configs) {
            loadSpriteSheet(assetManager, atlas, config);
        }
    }
    
    
    /**
     * Loads a single sprite sheet and splits it into frames
     * Handles different direction modes including mirroring for THREE_DIRECTIONAL_MIRRORED.
     *
     * @param assetManager The AssetManager containing the loaded texture
     * @param atlas The TextureAtlas to add the frame regions to
     * @param config The sprite sheet configuration
     */
    public static void loadSpriteSheet(AssetManager assetManager, TextureAtlas atlas, SpriteSheetConfig config) {
        if (!assetManager.isLoaded(config.getPath(), Texture.class)) {
            throw new IllegalStateException("Sprite Sheet not loaded: " + config.getPath());
        }
        Texture texture = assetManager.get(config.getPath(), Texture.class);

        // Slice into individual frames
        int frameWidth = config.getFrameWidth();
        int frameHeight = config.getFrameHeight();
        int frameCount = config.getFrameCount();
        int rowCount = config.getRowCount();
        DirectionMode directionMode = config.getDirectionMode();

        if (rowCount == 1) {
            // Single-row sprite sheet
            for (int i = 0; i < frameCount; i++) {
                int x = i * frameWidth;
                int y = 0;

                TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                String key = config.getFrameKey(i);
                atlas.addRegion(key, region);
            }
        } else if (directionMode == DirectionMode.THREE_DIRECTIONAL_MIRRORED) {
            // Special handling for 3-directional sprite sheets (UP/DOWN/RIGHT, with LEFT mirrored from RIGHT)
            loadThreeDirectionalMirrored(atlas, texture, config, frameWidth, frameHeight, frameCount);
        } else if (directionMode == DirectionMode.FOUR_DIRECTIONAL) {
            // Load 4-directional sprite sheets (UP/DOWN/LEFT/RIGHT)
            loadFourDirectional(atlas, texture, config, frameWidth, frameHeight, frameCount, rowCount);
        } else {
            // Standard multi-row sprite sheet (8-directional)
            loadEightDirectional(atlas, texture, config, frameWidth, frameHeight, frameCount, rowCount);
        }
    }

    /**
     * Load 8-directional sprite sheet.
     */
    private static void loadEightDirectional(TextureAtlas atlas, Texture texture, SpriteSheetConfig config,
                                             int frameWidth, int frameHeight, int frameCount, int rowCount) {
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < frameCount; col++) {
                int x = col * frameWidth;
                int y = row * frameHeight;

                TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                String key = config.getFrameKey(row, col);
                atlas.addRegion(key, region);
            }
        }
    }

    /**
     * Load 4-directional sprite sheet.
     * Assumes rows are: 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
     */
    private static void loadFourDirectional(TextureAtlas atlas, Texture texture, SpriteSheetConfig config,
                                            int frameWidth, int frameHeight, int frameCount, int rowCount) {
        // For 4-directional, we need to map to the standard 8-directional row indices
        // Standard mapping: 0=RIGHT, 2=DOWN, 4=LEFT, 6=UP
        // Source rows:      0=UP,    1=DOWN, 2=LEFT, 3=RIGHT

        for (int srcRow = 0; srcRow < Math.min(4, rowCount); srcRow++) {
            // Map source row to destination row in 8-directional format
            int destRow = mapFourDirectionalToEightDirectional(srcRow);

            for (int col = 0; col < frameCount; col++) {
                int x = col * frameWidth;
                int y = srcRow * frameHeight;

                TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                String key = config.getFrameKey(destRow, col);
                atlas.addRegion(key, region);
            }
        }
    }

    /**
     * Load 3-directional sprite sheet with mirrored LEFT direction.
     * Assumes rows are: 0=UP, 1=DOWN, 2=RIGHT
     * LEFT (row 4) is created by mirroring RIGHT (row 2).
     */
    private static void loadThreeDirectionalMirrored(TextureAtlas atlas, Texture texture, SpriteSheetConfig config,
                                                     int frameWidth, int frameHeight, int frameCount) {
        // Row mapping: 0=UP (dest 6), 1=DOWN (dest 2), 2=RIGHT (dest 0)
        int[] srcRows = {0, 1, 2};
        int[] destRows = {2, 6, 0}; // UP, DOWN, RIGHT

        for (int i = 0; i < srcRows.length; i++) {
            int srcRow = srcRows[i];
            int destRow = destRows[i];

            for (int col = 0; col < frameCount; col++) {
                int x = col * frameWidth;
                int y = srcRow * frameHeight;

                TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                String key = config.getFrameKey(destRow, col);
                atlas.addRegion(key, region);

                // Mirror RIGHT to create LEFT
                if (srcRow == 2) { // RIGHT row
                    TextureRegion mirrored = new TextureRegion(texture, x, y, frameWidth, frameHeight);
                    mirrored.flip(true, false); // Flip horizontally

                    String leftKey = config.getFrameKey(4, col); // LEFT is row 4
                    atlas.addRegion(leftKey, mirrored);
                }
            }
        }
    }

    /**
     * Map 4-directional source row to 8-directional destination row.
     * Source: 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
     * Dest: 0=RIGHT, 2=DOWN, 4=LEFT, 6=UP
     */
    private static int mapFourDirectionalToEightDirectional(int fourDirRow) {
        return switch (fourDirRow) {
            case 0 -> 2; // UP
            case 1 -> 6; // DOWN
            case 2 -> 0; // LEFT
            case 3 -> 4; // RIGHT
            default -> fourDirRow;
        };
    }

    public static List<String> getSpriteSheetPaths(List<SpriteSheetConfig> configs) {
        List<String> paths = new ArrayList<>();
        for (SpriteSheetConfig config : configs) {
            paths.add(config.getPath());
        }
        return paths;
    }
}
