package com.untitledgame.assets;

import java.util.ArrayList;
import java.util.List;

public class AnimationSetConfig {


    public enum Direction8 {
        EAST(0),
        SOUTH_EAST(1),
        SOUTH(2),
        SOUTH_WEST(3),
        WEST(4),
        NORTH_WEST(5),
        NORTH(6),
        NORTH_EAST(7);


        private final int row;

        Direction8(int row) {
            this.row = row;
        }

        public int getRow() {
            return row;
        }
    }

    private final String characterType;
    private final String basePath;
    private final int frameWidth;
    private final int frameHeight;
    private final DirectionMode directionMode;

    private final List<AnimationConfig> animations;

    public AnimationSetConfig(String characterType, String basePath, int frameWidth, int frameHeight) {
        this(characterType, basePath, frameWidth, frameHeight, DirectionMode.EIGHT_DIRECTIONAL);
    }

    public AnimationSetConfig(String characterType, String basePath, int frameWidth, int frameHeight, DirectionMode directionMode) {
        this.characterType = characterType;
        this.basePath = basePath;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.directionMode = directionMode;
        this.animations = new ArrayList<>();
    }

    public void addAnimation(String animationType, String filename, int frameCount) {
        animations.add(new AnimationConfig(animationType, filename, frameCount));
    }

    public String getCharacterType() {
        return characterType;
    }

    public List<AnimationConfig> getAnimations() {
        return animations;
    }

    public List<SpriteSheetConfig> createSpriteSheetConfigs() {
        List<SpriteSheetConfig> configs = new ArrayList<>();
        for (AnimationConfig anim : animations) {
            String path = basePath + "/" + anim.filename;
            String keyPrefix = characterType + "_" + anim.animationType;

            configs.add(new SpriteSheetConfig(path, frameWidth, frameHeight, anim.frameCount, directionMode.getDirectionCount(), keyPrefix, directionMode));
        }
        return configs;
    }

    public static class AnimationConfig {
        final String animationType;
        final String filename;
        final int frameCount;

        public AnimationConfig(String animationType, String filename, int frameCount) {
            this.animationType = animationType;
            this.filename = filename;
            this.frameCount = frameCount;
        }

        public String getAnimationType() {
            return animationType;
        }

        public String getFilename() {
            return filename;
        }

        public int getFrameCount() {
            return frameCount;
        }
    }


}
