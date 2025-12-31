package com.untitledgame.logic.npc;


import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.animation.AnimationController;
import com.untitledgame.animation.AnimationType;
import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;
import com.untitledgame.logic.Entity;
import com.untitledgame.logic.HealthComponent;
import com.untitledgame.assets.TETile;

import java.util.EnumMap;
import java.util.Random;

/**
 * Minimal NPC representation with random-walk behavior.
 * Instances are updated by {@link NpcManager}.
 * Uses centralized AnimationController for consistent animation management.
 */
public class Npc extends Entity {
    public static final double HITBOX_HALF = 0.30;
    public static final double ATTACK_HALF_EXTENT = 1.5;
    private final Random rng;
    private final long rngSeed;
    private final int variant;
    private final NpcType npcType;
    private int moveTick = 0;

    private static final int SEEK_LIMIT = 15;

    // Centralized animation system using shared timing constants
    private final AnimationController animationController;

    // Attack state tracking
    private boolean attacking = false;
    private int attackCooldownTicks = 0;
    private int attackAnimationTicks = 0; // Tracks how long attack animation has been playing
    private boolean damageQueuedThisAttack = false;

    // Configurable attack cooldown (in ticks, where 1 tick = 50ms)
    // TODO: Consider making these instance variables to allow different attack patterns per NPC type
    // Default: 30 ticks = 1.5 seconds between attacks
    private static final int ATTACK_COOLDOWN_TICKS = 30;
    // Duration to show attack animation before going to cooldown
    // Default: 20 ticks = 1.0 second of attack animation
    private static final int ATTACK_ANIMATION_DURATION_TICKS = 20;

    private final EnumMap<State, AiBehavior> behaviors = new EnumMap<>(State.class);
    private State state = State.IDLE;
    private AiBehavior activeBehavior;

    // Rendering fields
    private double drawX;
    private double drawY;
    private boolean hasWaypoint = false;
    private double waypointX;
    private double waypointY;

    // Tunables for movement pacing.
    private static final int STEP_INTERVAL = 8;    // ticks between movement attempts
    private static final double WAYPOINT_EPSILON = 0.05;

    public Npc(int x, int y, Random rng, long rngSeed, int variant, AnimationController animationController, HealthComponent health) {
        this(x, y, rng, rngSeed, variant, animationController, health, NpcType.DEFAULT);
    }

    public Npc(int x, int y, Random rng, long rngSeed, int variant, AnimationController animationController, HealthComponent health, NpcType npcType) {
        super(x, y, health);
        this.rng = rng;
        this.rngSeed = rngSeed;
        this.variant = variant;
        this.npcType = npcType;
        this.animationController = animationController;
        this.drawX = posX - 0.5;
        this.drawY = posY - 0.5;

        // Set initial animation to idle, facing down
        animationController.setAnimation(AnimationType.IDLE, Direction.DOWN);

        behaviors.put(State.IDLE, new IdleBehavior());
        behaviors.put(State.SEEK, new SeekBehavior());
        behaviors.put(State.ATTACK, new AttackBehavior());
        switchState(State.IDLE);
    }
    public long rngSeed() {
        return rngSeed;
    }

    public int variant() {
        return variant;
    }
    public NpcType npcType() {
        return npcType;
    }

    public Random rng() {
        return rng;
    }

    public boolean isAttacking() {
        return attacking;
    }
    public boolean hasQueuedAttackDamage() {
        return damageQueuedThisAttack;
    }

    public void markAttackDamageQueued() {
        damageQueuedThisAttack = true;
    }

    /**
     * Advance one tick of NPC simulation: possibly move.
     */
    public void tick(WorldView view) {
        moveTick += 1;

        if (isStaggered()) {
            // Halt movement and attacks while staggered
            attacking = false;
            attackAnimationTicks = 0;
            damageQueuedThisAttack = false;
            setVelocity(0.0, 0.0);
            return;
        }

        State desiredState = selectState(view);
        // Don't change state during an active attack animation
        if (attacking && state == State.ATTACK) {
            desiredState = State.ATTACK;
        }

        boolean stateChanged = desiredState != state;
        if (stateChanged) {
            // Clear attacking flag and cooldown when transitioning away FROM ATTACK state
            if (state == State.ATTACK && desiredState != State.ATTACK) {
                attacking = false;
                attackCooldownTicks = 0;
                attackAnimationTicks = 0;
                damageQueuedThisAttack = false;
            }
            switchState(desiredState);
        }

        // Handle attack cooldown system when in ATTACK state
        if (state == State.ATTACK) {
            if (attackCooldownTicks > 0) {
                // In cooldown period - count down
                attackCooldownTicks--;
                attacking = false; // Show idle animation during cooldown
            } else if (attacking) {
                // Currently attacking - increment animation timer
                attackAnimationTicks++;

                // Check if attack animation duration has elapsed
                if (attackAnimationTicks >= ATTACK_ANIMATION_DURATION_TICKS) {
                    // Transition to cooldown
                    attacking = false;
                    attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
                    attackAnimationTicks = 0;
                }
            }
            // If not attacking and no cooldown, ready to attack (behavior will call markAttacking())
        }

        activeBehavior.onTick(this, view);
        Direction move = null;
        if (moveTick >= STEP_INTERVAL) {
            moveTick = 0;
            move = activeBehavior.desiredMove();
        }

        // Update velocity based on desired move direction
        // NPCs now move smoothly like the player instead of tile-to-tile
        if (move != null) {
            int nx = x() + move.getDx();
            int ny = y() + move.getDy();
            if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
                // Set facing direction - velocity will be calculated from this in Engine.updateNpcMovement()
                facing = move;
            } else {
                // Can't move in desired direction, stop
                // Velocity will be set to zero in Engine.updateNpcMovement()
                setVelocity(0.0, 0.0);
            }
        }
    }

    private State selectState(WorldView view) {
        double dx = view.avatarPosition().x() - x();
        double dy = view.avatarPosition().y() - y();
        double distSq = dx * dx + dy * dy;
        // Use NPC type's max attack distance to determine when to enter attack state
        double maxAttackDistSq = npcType.getMaxAttackDistance() * npcType.getMaxAttackDistance();
        if (distSq <= maxAttackDistSq)
            return State.ATTACK;

        if (distSq < SEEK_LIMIT * SEEK_LIMIT)
            return State.SEEK;

        return State.IDLE;
    }


    private void switchState(State next) {
        state = next;
        activeBehavior = behaviors.get(next);
        activeBehavior.onEnterState(this);
    }

    /**
     * Update animation state time. Should be called each frame with delta time.
     * @param deltaSeconds Time elapsed since last frame in seconds
     */
    public void updateAnimation(float deltaSeconds) {
        // Determine animation type based on state and velocity (not waypoints)
        AnimationType desiredType = AnimationType.IDLE;
        if (isStaggered()) {
            desiredType = AnimationType.TAKE_DAMAGE;
        } else if (attacking) {
            desiredType = AnimationType.ATTACK;
        } else if (Math.abs(velocityX) > 1e-6 || Math.abs(velocityY) > 1e-6) {
            // Moving - use walk animation based on velocity magnitude
            desiredType = AnimationType.WALK;
        }

        // Update animation
        animationController.setAnimation(desiredType, facing);
        animationController.update(deltaSeconds);
    }

    /**
     * Mark that the NPC should perform an attack.
     * This starts the attack animation and sets up the cooldown timer.
     * Can only attack if not in cooldown period.
     */
    public void markAttacking() {
        // Only start a new attack if able to attack
        if (canAttack()) {
            attacking = true;
            attackAnimationTicks = 0; // Start tracking animation duration
            damageQueuedThisAttack = false;
        }
    }
    
    /**
     * Check if the NPC can attack (not currently attacking and not in cooldown).
     */
    public boolean canAttack() {
        return !attacking && attackCooldownTicks == 0;
    }
    
    /**
     * Get the current attack cooldown in ticks.
     */
    public int getAttackCooldownTicks() {
        return attackCooldownTicks;
    }

    /**
     * Returns the current animation frame as a TextureRegion.
     * This is the preferred method for rendering NPCs.
     * @return The current frame of the NPC's animation
     */
    public TextureRegion currentFrame() {
        return animationController.getCurrentFrame();
    }

    /**
     * @deprecated As of version 1.0.0, replaced by {@link #currentFrame()}
     * This method will be removed in version 2.0.0.
     * Use currentFrame() to get the proper animated texture region.
     * Note: This method returns a static fallback tile and does NOT reflect the NPC's actual visual state.
     */
    @Deprecated
    public TETile currentTile() {
        // Return a static NPC tile for backward compatibility
        // This does not reflect the actual animation state
        return com.untitledgame.assets.Tileset.NPC_CORPSE;
    }

    public double drawX() {
        return drawX;
    }

    public double drawY() {
        return drawY;
    }

    public void setDrawX(double x) {
        this.drawX = x;
    }

    public void setDrawY(double y) {
        this.drawY = y;
    }

    public boolean hasWaypoint() {
        return hasWaypoint;
    }

    public double waypointX() {
        return waypointX;
    }

    public double waypointY() {
        return waypointY;
    }

    public void clearWaypoint() {
        hasWaypoint = false;
        setVelocity(0.0, 0.0);
    }

    public void setWaypoint(double wx, double wy) {
        waypointX = wx;
        waypointY = wy;
        hasWaypoint = true;
        double dx = waypointX - posX;
        double dy = waypointY - posY;
        double len = Math.hypot(dx, dy);
        if (len > 1e-4) {
            setVelocity(dx / len, dy / len);
        } else {
            setVelocity(0.0, 0.0);
        }
    }

    public boolean reachedWaypoint() {
        if (!hasWaypoint) {
            return false;
        }
        double dx = waypointX - posX;
        double dy = waypointY - posY;
        return Math.hypot(dx, dy) <= WAYPOINT_EPSILON;
    }

    public void updateSmooth(double speed) {
        // Smoothly interpolate drawX/drawY toward actual x/y
        drawX += ((posX - 0.5) - drawX) * speed;
        drawY += ((posY - 0.5) - drawY) * speed;
    }

    private enum State {
        IDLE,
        SEEK,
        ATTACK
    }
}
