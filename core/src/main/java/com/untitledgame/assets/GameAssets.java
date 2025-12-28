package com.untitledgame.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameAssets implements AutoCloseable {
    private static final String NOT_LOADED_ERROR = "Assets not loaded yet. Call queueLoad() first, then call update() until it returns true before accessing assets.";
    private static final String ASSETS_ROOT_PATH = ""; // Empty string = root of internal files directory
    private final AssetManager assetManager;
    private final TextureAtlas atlas;
    private final Map<String, String> keyToPath;
    private boolean loaded;
    private boolean queued;

    //
    public GameAssets() {
        this.assetManager = new AssetManager(new InternalFileHandleResolver());
        this.atlas = new TextureAtlas();
        this.keyToPath = new LinkedHashMap<>();
    }

    public void queueLoad() {
        if (queued) {
            // Already queued - this is safe to call multiple times
            return;
        }
        collectPngs();
        for (String path : keyToPath.values()) {
            assetManager.load(path, Texture.class);
        }
        queued = true;
    }

    public boolean update() {
        if (loaded) {
            return true;
        }
        if (!queued) {
            return false;
        }
        if (assetManager.update()) {
            buildAtlas();
            loaded = true;
            return true;
        }
        return false;
    }

    public float getProgress() {
        return assetManager.getProgress();
    }

    private void buildAtlas() {
        for (Map.Entry<String, String> entry : keyToPath.entrySet()) {
            Texture texture = assetManager.get(entry.getValue(), Texture.class);
            atlas.addRegion(entry.getKey(), texture, 0, 0, texture.getWidth(), texture.getHeight());
        }
    }

    public TextureRegion region(String key) {
        if (!loaded) {
            throw new IllegalStateException(NOT_LOADED_ERROR);
        }
        return atlas.findRegion(key);
    }

    public TextureAtlas atlas() {
        if (!loaded) {
            throw new IllegalStateException(NOT_LOADED_ERROR);
        }
        return atlas;
    }
    public boolean isLoaded() {
        return loaded;
    }



    public String pathForKey(String key) {
        return keyToPath.get(key);
    }

    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        atlas.dispose();
        assetManager.dispose();
    }

    private void collectPngs() {
        FileHandle assetsRoot = Gdx.files.internal(ASSETS_ROOT_PATH);
        collectPngsRecursive(assetsRoot, "");
    }

    private void collectPngsRecursive(FileHandle dir, String relativePath) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        FileHandle[] files = dir.list();
        if (files == null) {
            return;
        }
        for (FileHandle file : files) {
            String currentPath = relativePath.isEmpty() ? file.name() : relativePath + "/" + file.name();
            if (file.isDirectory()) {
                collectPngsRecursive(file, currentPath);
            } else if (file.name().toLowerCase().endsWith(".png")) {
                String key = keyFor(currentPath);
                keyToPath.put(key, currentPath);
            }
        }
    }


    private String keyFor(String relativePath) {
        int dotIndex = relativePath.lastIndexOf('.');
        if (dotIndex > 0) {
            return relativePath.substring(0, dotIndex);
        }
        return relativePath;
    }

}
