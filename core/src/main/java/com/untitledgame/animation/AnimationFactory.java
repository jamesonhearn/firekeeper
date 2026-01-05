package com.untitledgame.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.assets.Tileset;
import com.untitledgame.logic.Direction;

/**
 * Factory for creating AnimationControllers for different entity types.
 * Centralizes animation setup logic following libGDX best practices.
 */
public class AnimationFactory {

    private static final int WALK_TICKS = 1;
    private static final int RUN_TICKS = 1;   // Same as walk, processing differentiates speed
    private static final int ATTACK_TICKS = 1;
    private static final int DEATH_TICKS = 2;
    private static final int IDLE_TICKS = 1;
    private static final int DODGE_TICKS = 1;

    /**
     * Create an AnimationController for the player avatar.
     */
    public static AnimationController createPlayerController(TextureAtlas atlas) {
        AnimationController controller = new AnimationController();
        
        // Register IDLE animations (single frame from first frame of walk)
        registerPlayerAnimation(controller, atlas, AnimationType.IDLE, "idle", IDLE_TICKS, Animation.PlayMode.LOOP);
        
        // Register WALK animations
        registerPlayerAnimation(controller, atlas, AnimationType.WALK, "walk", WALK_TICKS, Animation.PlayMode.LOOP);
        
        // Register RUN animations (same frames as walk, faster timing)
        registerPlayerAnimation(controller, atlas, AnimationType.RUN, "walk", RUN_TICKS, Animation.PlayMode.LOOP);
        
        // Register ATTACK animations
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_basic", ATTACK_TICKS, Animation.PlayMode.NORMAL);
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_secondary", ATTACK_TICKS, Animation.PlayMode.NORMAL);
        registerPlayerAnimation(controller, atlas, AnimationType.ATTACK, "melee_spin", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        registerPlayerAnimation(controller, atlas, AnimationType.KICK, "kick", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        // Register TAKE_DAMAGE animations
        registerPlayerAnimation(controller, atlas, AnimationType.TAKE_DAMAGE, "takedamage", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        // Register BLOCK animations (parrying/shield blocking)
        registerPlayerAnimation(controller, atlas, AnimationType.BLOCK, "block", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        // Register DEATH animations
        registerPlayerAnimation(controller, atlas, AnimationType.DEATH, "die", DEATH_TICKS, Animation.PlayMode.NORMAL);
        
        return controller;
    }

    /**
     * Create an AnimationController for an NPC.
     * @param atlas The texture atlas containing NPC sprite sheets
     * @param variant The NPC variant (currently all variants use the same animation set)
     */
    public static AnimationController createNpcController(TextureAtlas atlas, int variant) {
        AnimationController controller = new AnimationController();
        
        // Note: All NPC variants currently use the same animation set from "npc" sprite sheets
        // The variant parameter is retained for future use when different NPC types have different animations
        
        // Register IDLE animations
        registerNpcAnimation(controller, atlas, AnimationType.IDLE, "idle", IDLE_TICKS, Animation.PlayMode.LOOP);
        
        // Register WALK animations
        registerNpcAnimation(controller, atlas, AnimationType.WALK, "walk", WALK_TICKS, Animation.PlayMode.LOOP);
        
        // Register ATTACK animations (NPCs have attack1 and attack2, we'll use attack1)
        // Changed to LOOP mode to support attack cooldown system
        registerNpcAnimation(controller, atlas, AnimationType.ATTACK, "attack1", ATTACK_TICKS, Animation.PlayMode.LOOP);

        // Register TAKE_DAMAGE animations
        registerNpcAnimation(controller, atlas, AnimationType.TAKE_DAMAGE, "takedamage", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        // Register KICK animations (Counter-attack animation)
        registerNpcAnimation(controller, atlas, AnimationType.KICK, "kick", ATTACK_TICKS, Animation.PlayMode.NORMAL);

        // Register DODGE animations (Rolling spritesheet)
        registerNpcAnimation(controller, atlas, AnimationType.DODGE1, "rolling", DODGE_TICKS, Animation.PlayMode.NORMAL);
        registerNpcAnimation(controller, atlas, AnimationType.DODGE2, "slide", DODGE_TICKS, Animation.PlayMode.NORMAL);

        // Register DEATH animations
        registerNpcAnimation(controller, atlas, AnimationType.DEATH, "die", DEATH_TICKS, Animation.PlayMode.NORMAL);
        
        return controller;
    }

    /**
     * Register player animations for all four directions.
     */
    private static void registerPlayerAnimation(AnimationController controller, TextureAtlas atlas,
                                                AnimationType type, String animationName,
                                                int ticksPerFrame, Animation.PlayMode playMode) {
        for (Direction direction : Direction.values()) {
            TextureRegion[] frames = Tileset.loadAnimationFrames(atlas, "player", animationName, direction, 15);
            if (frames.length > 0) {
                Animation<TextureRegion> animation = AnimationController.createAnimation(frames, ticksPerFrame, playMode);
                controller.registerAnimation(type, direction, animation);
            }
        }
    }

    /**
     * Register NPC animations for all four directions.
     */
    private static void registerNpcAnimation(AnimationController controller, TextureAtlas atlas,
                                            AnimationType type, String animationName,
                                            int ticksPerFrame, Animation.PlayMode playMode) {
        for (Direction direction : Direction.values()) {
            TextureRegion[] frames = Tileset.loadAnimationFrames(atlas, "npc", animationName, direction, 15);
            if (frames.length > 0) {
                Animation<TextureRegion> animation = AnimationController.createAnimation(frames, ticksPerFrame, playMode);
                controller.registerAnimation(type, direction, animation);
            }
        }
    }
}
