package com.untitledgame.debug;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.untitledgame.Renderer;
import com.untitledgame.assets.Tileset;
import com.untitledgame.assets.TileType;
import com.untitledgame.logic.World;

import java.util.Random;

/**
 * Minimal map previewer that renders a generated {@link World} with no HUD or gameplay,
 * intended only for debugging.
 */
public class MapPreviewApp extends ApplicationAdapter {
    private static final int VIEW_WIDTH = 50;
    private static final int VIEW_HEIGHT = 35;
    private static final String[] BASE_TILE_TEXTURES = new String[]{
            "tiles/cave_floor_6.png",
            "tiles/elevator.png",
            "tiles/cave_wall_top.png",
            "tiles/cave_wall_base.png",
            "tiles/cave_wall_left.png",
            "tiles/cave_wall_right.png"
    };

    private final long seed;
    private AssetManager assets;
    private Renderer renderer;
    private TextureAtlas atlas;
    private TileType[][] world;
    private Vector2 focus;

    public MapPreviewApp(long seed) {
        this.seed = seed;
    }

    @Override
    public void create() {
        assets = new AssetManager();
        queueTextures();
        assets.finishLoading();
        atlas = buildAtlas();
        Tileset.initialize(atlas);

        world = new World(seed).generate();
        focus = findFirstFloor(world);

        renderer = new Renderer();
        renderer.configureView(World.WIDTH, World.HEIGHT, VIEW_WIDTH, VIEW_HEIGHT, 0);
        renderer.initialize(VIEW_WIDTH, VIEW_HEIGHT, atlas);
        renderer.setAvatarPosition((int) focus.x, (int) focus.y);
    }

    @Override
    public void render() {
        renderer.clearScreen();
        renderer.updateCamera();
        Renderer.RenderContext context = renderer.buildContext(world);
        renderer.beginBatch();
        renderer.drawWorld(world, null, null, null, context, null);
        renderer.endBatch();
    }

    @Override
    public void dispose() {
        if (renderer != null) {
            renderer.dispose();
        }
        if (atlas != null) {
            atlas.dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
    }

    private void queueTextures() {
        for (String path : BASE_TILE_TEXTURES) {
            assets.load(path, Texture.class);
        }
    }

    private TextureAtlas buildAtlas() {
        TextureAtlas built = new TextureAtlas();
        for (String path : BASE_TILE_TEXTURES) {
            Texture texture = assets.get(path, Texture.class);
            String key = stripExtension(path);
            built.addRegion(key, new TextureRegion(texture));
        }
        return built;
    }

    private Vector2 findFirstFloor(TileType[][] world) {
        for (int x = 0; x < world.length; x++) {
            for (int y = 0; y < world[0].length; y++) {
                if (world[x][y] == TileType.FLOOR) {
                    return new Vector2(x, y);
                }
            }
        }
        return new Vector2(World.WIDTH / 2f, World.HEIGHT / 2f);
    }

    private String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot <= 0) {
            return path;
        }
        return path.substring(0, dot);
    }

    public static MapPreviewApp forRandomSeed() {
        return new MapPreviewApp(new Random().nextLong());
    }
}
