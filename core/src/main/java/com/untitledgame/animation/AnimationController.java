package com.untitledgame.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.logic.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Centralized animation controller that manages animation state for entities.
 * Follows libGDX animation system best practices as per https://libgdx.com/wiki/
 */
public class AnimationController {
    private static final int MS_PER_S = 1000;
    private static final int TICK_MS = 40;

    private final EnumMap<AnimationType, EnumMap<Direction, List<Animation<TextureRegion>>>> animations;
    private AnimationType currentAnimationType;
    private Direction currentDirection;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private int currentAnimationVariant; // Track which variant is active

    public AnimationController() {
        this.animations = new EnumMap<>(AnimationType.class);
        this.currentAnimationType = AnimationType.IDLE;
        this.currentDirection = Direction.DOWN;
        this.stateTime = 0f;
        this.currentAnimationVariant = 0;
    }

    /**
     * Register an animation for a specific type and direction.
     */
    public void registerAnimation(AnimationType type, Direction direction, Animation<TextureRegion> animation) {
        animations.computeIfAbsent(type, k -> new EnumMap<>(Direction.class))
                .computeIfAbsent(direction, d -> new ArrayList<>())
                .add(animation);
    }

    /**
     * Set the current animation type and direction.
     * Automatically handles state time transitions.
     */
    public void setAnimation(AnimationType type, Direction direction) {
        setAnimation(type, direction, 0);
    }

    /**
     * Set the current animation type, direction, and variant.
     * Automatically handles state time transitions.
     */
    public void setAnimation(AnimationType type, Direction direction, int variant) {
        EnumMap<Direction, List<Animation<TextureRegion>>> byDirection = animations.get(type);
        if (byDirection == null) {
            return;
        }

        List<Animation<TextureRegion>> variants = byDirection.get(direction);
        if (variants == null || variants.isEmpty()) {
            return;
        }

        // Clamp variant to available animations
        int safeVariant = Math.max(0, Math.min(variant, variants.size() - 1));
        Animation<TextureRegion> newAnimation = variants.get(safeVariant);
        if (newAnimation == null) {
            return;
        }

        boolean typeChanged = type != currentAnimationType;
        boolean animationChanged = newAnimation != currentAnimation;

        if (typeChanged) {
            // Reset state time for certain transitions
            // Reset when switching TO IDLE, ATTACK, or DEATH to start from frame 0
            if (type == AnimationType.IDLE || type == AnimationType.ATTACK
                    || type == AnimationType.DEATH || type == AnimationType.TAKE_DAMAGE
                    || type == AnimationType.DODGE1 || type == AnimationType.DODGE2
                    || type == AnimationType.KICK || type == AnimationType.BLOCK) {
                stateTime = 0f;
            } else {
                // Carry over state time for smooth transitions (e.g., walk to run)
                stateTime = carryStateTime(currentAnimation, newAnimation, stateTime);
            }
        } else if (animationChanged) {
            // Direction changed, carry over state time
            stateTime = carryStateTime(currentAnimation, newAnimation, stateTime);
        }

        currentAnimationType = type;
        currentDirection = direction;
        currentAnimation = newAnimation;
        currentAnimationVariant = safeVariant;
    }

    /**
     * Update animation state time.
     * @param deltaSeconds Time elapsed since last frame in seconds
     */
    public void update(float deltaSeconds) {
        stateTime += deltaSeconds;
    }

    /**
     * Get the current animation frame.
     */
    public TextureRegion getCurrentFrame() {
        if (currentAnimation == null) {
            return null;
        }
        // Non-looping animations: DEATH, ATTACK, TAKE_DAMAGE, DODGE, KICK, BLOCK
        // Looping animations: IDLE, WALK, RUN
        boolean looping = currentAnimationType == AnimationType.IDLE
                || currentAnimationType == AnimationType.WALK
                || currentAnimationType == AnimationType.RUN;
        return currentAnimation.getKeyFrame(stateTime, looping);
    }

    /**
     * Check if the current animation has finished (for non-looping animations).
     */
    public boolean isAnimationFinished() {
        if (currentAnimation == null) {
            return true;
        }
        return currentAnimation.isAnimationFinished(stateTime);
    }

    /**
     * Get current animation type.
     */
    public AnimationType getCurrentAnimationType() {
        return currentAnimationType;
    }

    /**
     * Get current direction.
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Reset state time to 0.
     */
    public void resetStateTime() {
        stateTime = 0f;
    }

    /**
     * Calculate frame duration in seconds from tick count.
     * This ensures consistent timing across all animations.
     */
    public static float frameDurationSeconds(int ticksPerFrame) {
        return (float) (ticksPerFrame * (TICK_MS / (double) MS_PER_S));
    }

    /**
     * Carry state time from one animation to another for smooth transitions.
     * Clamps the frame index to prevent exceeding the new animation's frame count.
     */
    private float carryStateTime(Animation<TextureRegion> previous, Animation<TextureRegion> next, float previousStateTime) {
        if (previous == null || next == null) {
            return 0f;
        }
        TextureRegion[] nextFrames = next.getKeyFrames();
        if (nextFrames.length == 0) {
            return 0f;
        }
        int frameIndex = previous.getKeyFrameIndex(previousStateTime);
        // Clamp frame index to the new animation's bounds
        int maxFrameIndex = nextFrames.length - 1;
        frameIndex = Math.min(frameIndex, maxFrameIndex);
        return frameIndex * next.getFrameDuration();
    }

    /**
     * Create an animation from texture regions with consistent timing.
     */
    public static Animation<TextureRegion> createAnimation(TextureRegion[] frames, int ticksPerFrame, Animation.PlayMode playMode) {
        float frameDuration = frameDurationSeconds(ticksPerFrame);
        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);
        animation.setPlayMode(playMode);
        return animation;
    }
}
