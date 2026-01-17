package com.untitledgame.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.assets.DirectionMode;
import com.untitledgame.assets.Tileset;
import com.untitledgame.logic.Direction;

/**
 * Factory for creating AnimationControllers for different entity types.
 * Centralizes animation setup logic following libGDX best practices.
 */
public class AnimationFactory {

    private static final int WALK_TICKS_PLAYER = 3;
    private static final int WALK_TICKS_NPC = 6;
    private static final int RUN_TICKS = 2;   // Same as walk, processing differentiates speed
    private static final int ATTACK_TICKS_PLAYER = 1;
    private static final int ATTACK_TICKS_NPC = 2;
    private static final int DEATH_TICKS = 2;
    private static final int IDLE_TICKS = 1;
    private static final int DODGE_TICKS = 1;

    /**
     * Create an AnimationController for the player avatar.
     * Uses 8-directional animations by default.
     */
    public static AnimationController createPlayerController(TextureAtlas atlas) {
        return createPlayerController(atlas, DirectionMode.EIGHT_DIRECTIONAL);
    }

    /**
     * Create an AnimationController for the player avatar with specified direction mode.
     * @param atlas The texture atlas containing player sprite sheets
     * @param directionMode The direction mode for animations (4-dir, 8-dir, or 3-dir mirrored)
     */
    public static AnimationController createPlayerController(TextureAtlas atlas, DirectionMode directionMode) {
        AnimationController controller = new AnimationController();
        
        // Register IDLE animations (single frame from first frame of walk)
        registerPlayerAnimation(controller, atlas, AnimationType.IDLE, "idle", IDLE_TICKS, Animation.PlayMode.LOOP, directionMode);

        // Register WALK animations
        registerPlayerAnimation(controller, atlas, AnimationType.WALK, "walk", WALK_TICKS_PLAYER, Animation.PlayMode.LOOP, directionMode);

        // Register RUN animations (same frames as walk, faster timing)
        registerPlayerAnimation(controller, atlas, AnimationType.RUN, "walk", RUN_TICKS, Animation.PlayMode.LOOP, directionMode);
        
        // Register ATTACK animations
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_basic", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_secondary", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_spin", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_run", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);

        registerPlayerAnimation(controller, atlas, AnimationType.KICK, "kick", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);

        // Register TAKE_DAMAGE animations
        registerPlayerAnimation(controller, atlas, AnimationType.TAKE_DAMAGE, "take_damage", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);

        // Register BLOCK animations (parrying/shield blocking)
        registerPlayerAnimation(controller, atlas, AnimationType.BLOCK, "block_start", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);
        registerPlayerAnimation(controller, atlas, AnimationType.BLOCK, "block_end", ATTACK_TICKS_PLAYER, Animation.PlayMode.NORMAL, directionMode);

        // Register DEATH animations
        registerPlayerAnimation(controller, atlas, AnimationType.DEATH, "death", DEATH_TICKS, Animation.PlayMode.NORMAL, directionMode);
        
        return controller;
    }

    /**
     * Create an AnimationController for an NPC.
     * Uses 8-directional animations by default.
     * @param atlas The texture atlas containing NPC sprite sheets
     * @param variant The NPC variant (currently all variants use the same animation set)
     */
    public static AnimationController createNpcController(TextureAtlas atlas, int variant) {
        return createNpcController(atlas, variant, DirectionMode.FOUR_DIRECTIONAL);
    }

    /**
     * Create an AnimationController for an NPC with specified direction mode.
     * @param atlas The texture atlas containing NPC sprite sheets
     * @param variant The NPC variant (currently all variants use the same animation set)
     * @param directionMode The direction mode for animations (4-dir, 8-dir, or 3-dir mirrored)
     */
    public static AnimationController createNpcController(TextureAtlas atlas, int variant, DirectionMode directionMode) {
        AnimationController controller = new AnimationController();
        
        // Note: All NPC variants currently use the same animation set from "npc" sprite sheets
        // The variant parameter is retained for future use when different NPC types have different animations
        
        // Register IDLE animations
        registerNpcAnimation(controller, atlas, AnimationType.IDLE, "idle", IDLE_TICKS, Animation.PlayMode.LOOP, directionMode);
        
        // Register WALK animations
        registerNpcAnimation(controller, atlas, AnimationType.WALK, "walk", WALK_TICKS_NPC, Animation.PlayMode.LOOP, directionMode);
        
        // Register ATTACK animations (NPCs have attack1 and attack2, we'll use attack1)
        // Changed to LOOP mode to support attack cooldown system
        registerNpcAnimation(controller, atlas, AnimationType.ATTACK, "ATTACK", ATTACK_TICKS_NPC, Animation.PlayMode.LOOP, directionMode);

        // Register TAKE_DAMAGE animations
        registerNpcAnimation(controller, atlas, AnimationType.TAKE_DAMAGE, "take_damage", ATTACK_TICKS_NPC, Animation.PlayMode.NORMAL, directionMode);

        // Register KICK animations (Counter-attack animation)
        registerNpcAnimation(controller, atlas, AnimationType.KICK, "kick", ATTACK_TICKS_NPC, Animation.PlayMode.NORMAL, directionMode);

        // Register DODGE animations (Rolling spritesheet)
        registerNpcAnimation(controller, atlas, AnimationType.DODGE1, "rolling", DODGE_TICKS, Animation.PlayMode.NORMAL, directionMode);
        registerNpcAnimation(controller, atlas, AnimationType.DODGE2, "slide", DODGE_TICKS, Animation.PlayMode.NORMAL, directionMode);

        // Register DEATH animations
        registerNpcAnimation(controller, atlas, AnimationType.DEATH, "death", DEATH_TICKS, Animation.PlayMode.NORMAL, directionMode);
        
        return controller;
    }

    /**
     * Register player animations for all directions (uses 8-directional by default).
     * */
    private static void registerPlayerAnimation(AnimationController controller, TextureAtlas atlas,
                                                AnimationType type, String animationName,
                                                int ticksPerFrame, Animation.PlayMode playMode) {
        registerPlayerAnimation(controller, atlas, type, animationName, ticksPerFrame, playMode, DirectionMode.EIGHT_DIRECTIONAL);
    }

    /**
     * Register player animations for all directions with specified direction mode.
     */
    private static void registerPlayerAnimation(AnimationController controller, TextureAtlas atlas,
                                                AnimationType type, String animationName,
                                                int ticksPerFrame, Animation.PlayMode playMode, DirectionMode directionMode) {
        for (Direction direction : Direction.values()) {
            TextureRegion[] frames = Tileset.loadAnimationFrames(atlas, "player", animationName, direction, 15, directionMode);
            if (frames.length > 0) {
                Animation<TextureRegion> animation = AnimationController.createAnimation(frames, ticksPerFrame, playMode);
                controller.registerAnimation(type, direction, animation);
            }
        }
    }

    /**
     * Register NPC animations for all directions (uses 8-directional by default).
     * */
    private static void registerNpcAnimation(AnimationController controller, TextureAtlas atlas,
                                            AnimationType type, String animationName,
                                            int ticksPerFrame, Animation.PlayMode playMode) {
        registerNpcAnimation(controller, atlas, type, animationName, ticksPerFrame, playMode, DirectionMode.EIGHT_DIRECTIONAL);
    }

    /**
     * Register NPC animations for all directions with specified direction mode.
     */
    private static void registerNpcAnimation(AnimationController controller, TextureAtlas atlas,
                                             AnimationType type, String animationName,
                                             int ticksPerFrame, Animation.PlayMode playMode, DirectionMode directionMode) {
        for (Direction direction : Direction.values()) {
            TextureRegion[] frames = Tileset.loadAnimationFrames(atlas, "npc", animationName, direction, 15, directionMode);
            if (frames.length > 0) {
                Animation<TextureRegion> animation = AnimationController.createAnimation(frames, ticksPerFrame, playMode);
                controller.registerAnimation(type, direction, animation);
            }
        }
    }
}
