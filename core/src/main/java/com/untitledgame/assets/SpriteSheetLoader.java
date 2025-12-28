package com.untitledgame.assets;


import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;


public class SpriteSheetLoader {


    private Object paths;

    public static void loadSpriteSheets(AssetManager assetManager, TextureAtlas atlas, List<SpriteSheetConfig> configs) {
        for (SpriteSheetConfig config : configs) {
            loadSpriteSheet(assetManager, atlas, config);
        }
    }
    
    
    /**
     * Loads a single sprite sheet and splits it into frames.
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

        if (rowCount == 1) {
            for (int i = 0; i < frameCount; i++) {
                int x = i * frameWidth;
                int y = 0; // each animation is single row

                //Texture region for this frame
                TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                // Add to atlas with key
                String key = config.getFrameKey(i);
                atlas.addRegion(key, region);
            }
        } else {
            // Multi-row sprite sheet
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < frameCount; col++) {
                    int x = col * frameWidth;
                    int y = row * frameHeight;

                    //Texture region for this frame
                    TextureRegion region = new TextureRegion(texture, x, y, frameWidth, frameHeight);

                    // Add to atlas with key
                    String key = config.getFrameKey(row, col);
                    atlas.addRegion(key, region);
                }
            }
        }
    }

    public static List<String> getSpriteSheetPaths(List<SpriteSheetConfig> configs) {
        List<String> paths = new ArrayList<>();
        for (SpriteSheetConfig config : configs) {
            paths.add(config.getPath());
        }
        return paths;
    }
}
