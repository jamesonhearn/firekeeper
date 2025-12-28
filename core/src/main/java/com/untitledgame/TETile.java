package com.untitledgame;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.untitledgame.logic.TileType;

/**
 * TETile represents a single tile in the game world using libGDX's texture system.
 * Tiles are drawn using TextureRegions from a TextureAtlas for efficient rendering.
 *
 */

public class TETile {
    private final String description;
    private final String atlasKey;
    private final int id;
    private final TextureRegion cachedRegion;
    private static SpriteBatch spriteBatch;
    private static TextureAtlas textureAtlas;
    private static float defaultTileSize = 1.0f;

    /**
     * Constructor for TETile with pre-resolved texture region.
     * @param description The description of the tile, shown in the GUI on hovering over the tile.
     * @param region Pre-loaded texture region from atlas
     * @param atlasKey Atlas key for the texture
     * @param id Unique identifier for this tile type
     */
    public TETile(String description, TextureRegion region, String atlasKey, int id) {
        this.description = description;
        this.cachedRegion = region;
        this.atlasKey = atlasKey;
        this.id = id;
    }

    /**
     * Factory method to create a tile from a texture atlas.
     * @param description The description of the tile
     * @param region Pre-loaded texture region from atlas
     * @param atlasKey Atlas key for the texture
     * @param id Unique identifier for this tile type
     * @return A new TETile instance
     * */
    public static TETile fromRegion(String description, TextureRegion region, String atlasKey, int id) {
        return new TETile(description, region, atlasKey, id);
    }


    /**
     * Draws the tile to the screen using libGDX's SpriteBatch.
     * @param x x coordinate in world units
     * @param y y coordinate in world units
     *
     **/
    public void draw(double x, double y) {
        drawSized(x, y, defaultTileSize);
    }


    /**
     * Draws the tile scaled to a specific size.
     * @param x x coordinate in world units
     * @param y y coordinate in world units
     * @param scale scale multiplier for the tile size
     */
    public void drawScaled(double x, double y, double scale) {
        drawSized(x, y, scale);
    }


    /**
     * Draws the tile scaled to the provided tileSize in world units. Useful when the
     * rendering grid wants to enlarge tiles without changing avatar size or image
     * source assets.
     * @param x x coordinate
     * @param y y coordinate
     * @param tileSize size of one tile in world units
     */
    public void drawSized(double x, double y, double tileSize) {
        if (spriteBatch == null || textureAtlas == null) {
            return;
        }
        TextureRegion region = cachedRegion != null ? cachedRegion : textureRegion(textureAtlas);
        if (region == null) {
            return;
        }
        spriteBatch.draw(region, (float) x, (float) y, (float) tileSize, (float) tileSize);
    }

    public TextureRegion textureRegion(TextureAtlas atlas) {
        if (atlas == null || atlasKey == null) {
            return null;
        }
        return atlas.findRegion(atlasKey);
    }

    public static void configureRendering(SpriteBatch batch, TextureAtlas atlas, float tileSize) {
        spriteBatch = batch;
        textureAtlas = atlas;
        defaultTileSize = tileSize;
    }

    /**
     * Description of the tile. Useful for displaying mouseover text or
     * testing that two tiles represent the same type of thing.
     * @return description of the tile
     */
    public String description() {
        return description;
    }

    /**
     * Gets the TextureRegion associated with this tile.
     * @return the texture region, or null if none is available
     */
    public TextureRegion getRegion() {
        return cachedRegion;
    }

    /**
     * ID number of the tile. Used for equality comparisons.
     * @return id of the tile
     */
    public int id() {
        return id;
    }


    /**
     * Makes a copy of the given 2D tile array, converting TileType to TETile.
     * @param tiles the 2D array to copy
     **/
    public static TETile[][] copyOf(TileType[][] tiles) {
        if (tiles == null) {
            return null;
        }

        TETile[][] copy = new TETile[tiles.length][];

        for (int i = 0; i < tiles.length; i++) {
            copy[i] = new TETile[tiles[i].length];
            for (int j = 0; j < tiles[i].length; j++) {
                copy[i][j] = tileTypeToTETile(tiles[i][j]);
            }
        }

        return copy;
    }

    /**
     * Converts a TileType enum to a TETile object.
     * @param type the TileType to convert
     * @return the corresponding TETile
     */
    private static TETile tileTypeToTETile(TileType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case FLOOR -> com.untitledgame.assets.Tileset.FLOOR;
            case ELEVATOR -> com.untitledgame.assets.Tileset.ELEVATOR;
            case WALL_TOP -> com.untitledgame.assets.Tileset.WALL_TOP;
            case WALL_SIDE -> com.untitledgame.assets.Tileset.WALL_SIDE;
            case LEFT_WALL -> com.untitledgame.assets.Tileset.LEFT_WALL;
            case BACK_WALL -> com.untitledgame.assets.Tileset.BACK_WALL;
            case NOTHING -> null;
        };
    }

    /**
     * Checks if two tiles are equal by comparing their IDs.
     * @param o object to compare with
     * @return boolean representing equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return (o instanceof TETile otherTile && otherTile.id == this.id);
    }
}
