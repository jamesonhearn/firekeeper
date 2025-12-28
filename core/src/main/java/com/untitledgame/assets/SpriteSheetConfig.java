package com.untitledgame.assets;

public class SpriteSheetConfig {
    private final String path;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int rowCount;
    private final String keyPrefix;


    public SpriteSheetConfig(String path, int frameWidth, int frameHeight, int frameCount, String keyPrefix) {
        this(path, frameWidth, frameHeight, frameCount, 1, keyPrefix);
    }


    public SpriteSheetConfig(String path, int frameWidth, int frameHeight, int frameCount, int rowCount, String keyPrefix) {
        this.path = path;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.rowCount = rowCount;
        this.keyPrefix = keyPrefix;
    }

    public String getPath() {
        return path;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getFrameKey(int frameIndex) {
        return keyPrefix + "_" + frameIndex;
    }

    public String getFrameKey(int row, int frameIndex) {
        return keyPrefix + "_" + row + "_" + frameIndex;
    }
}

