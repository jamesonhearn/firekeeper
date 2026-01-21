package com.untitledgame.logic;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.untitledgame.animation.AnimationController;
import com.untitledgame.animation.AnimationType;

/**
 * Player-controlled entity with limited lives and a tracked spawn point.
 * Uses centralized AnimationController for consistent animation management with NPCs.
 */
public class Avatar extends Entity {

    public enum MeleeAttackType {
        BASIC,
        SECONDARY,
        SPIN
    }

    public static final double HITBOX_HALF = 0.24;
    private int lives;
    private Position spawnPoint;
    private int attacksSinceLastSpin = 0;
    private int attacksUntilNextSpin = 2 + (int)(Math.random() * 2); // 2-3 attacks
    private MeleeAttackType currentMeleeType = MeleeAttackType.BASIC;


    // Centralized animation system using shared AnimationController
    private final AnimationController animationController;

    // Attack state tracking
    private boolean attacking = false;
    private boolean attackQueued = false;
    private Direction attackFacing = Direction.DOWN;

    // Kick state tracking
    private boolean kickInProgress = false;

    // Parry state tracking
    private boolean parryInProgress = false;
    private double parryWindowRemainingMs = 0.0;
    private static final double PARRY_WINDOW_MS = 200.0;

    // Dash state tracking
    private boolean dashInProgress = false;
    private double dashDistanceRemaining = 0.0;
    private static final double AVATAR_DASH_DISTANCE = 6.0;
    private Direction dashDirection = Direction.DOWN;


    public Avatar(int x, int y, int lives, HealthComponent health, AnimationController animationController) {
        super(x, y, health);
        this.lives = Math.max(0, lives);
        this.spawnPoint = new Position(x, y);
        this.animationController = animationController;

        // Set initial animation to idle, facing down
        if (animationController != null) {
            animationController.setAnimation(AnimationType.IDLE, Direction.DOWN);
        }
    }

    public int lives() {
        return lives;
    }

    public void setSpawnPoint(Position spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public Position spawnPoint() {
        return spawnPoint;
    }

    public void loseLife() {
        if (lives > 0) {
            lives -= 1;
        }
    }

    public void respawn() {
        if (spawnPoint != null) {
            setPosition(spawnPoint.x(), spawnPoint.y());
        }
        if (health != null) {
            health.restoreFull();
        }
        // Reset combat states on respawn
        attacking = false;
        attackQueued = false;
        parryInProgress = false;
        kickInProgress = false;
        parryWindowRemainingMs = 0.0;
        dashInProgress = false;
        dashDistanceRemaining = 0.0;
    }

    // Animation system methods

    /**
     * Update animation state time. Should be called each frame with delta time.
     * @param deltaSeconds Time elapsed since last frame in seconds
     */
    public void updateAnimation(float deltaSeconds) {
        if (animationController == null) {
            return;
        }

        // Determine animation type based on state
        AnimationType desiredType = AnimationType.IDLE;
        int animationVariant = 0;
        if (health != null && health.isDepleted()) {
            desiredType = AnimationType.DEATH;
        } else if (isStaggered()) {
            desiredType = AnimationType.TAKE_DAMAGE;
        } else if (kickInProgress) {
            desiredType = AnimationType.KICK;
        } else if (parryInProgress) {
            desiredType = AnimationType.BLOCK;
            // Select block animation variant based on parry window
            animationVariant = isParrying() ? 0 : 1; // 0=block_start (active parry), 1=block_end
        } else if (attacking) {
            desiredType = AnimationType.ATTACK;
            // Select attack animation based on current melee type
            animationVariant = switch (currentMeleeType) {
                case BASIC -> 0;      // melee_basic
                case SECONDARY -> 1;  // melee_secondary
                case SPIN -> 2;       // melee_spin
            };
        } else if (dashInProgress) {
            desiredType = AnimationType.DASH;
            facing = dashDirection;
        } else if (Math.abs(velocityX) > 1e-6 || Math.abs(velocityY) > 1e-6) {
            desiredType = AnimationType.WALK;
        }

        // Update animation
        animationController.setAnimation(desiredType, facing, animationVariant);
        animationController.update(deltaSeconds);
    }

    /**
     * Returns the current animation frame as a TextureRegion.
     * @return The current frame of the avatar's animation
     */
    public TextureRegion currentFrame() {
        if (animationController == null) {
            return null;
        }
        return animationController.getCurrentFrame();
    }

    /**
     * Check if the current animation has finished (for non-looping animations).
     */
    public boolean isAnimationFinished() {
        if (animationController == null) {
            return true;
        }
        return animationController.isAnimationFinished();
    }

    /**
     * Get the animation controller for this avatar.
     */
    public AnimationController getAnimationController() {
        return animationController;
    }

    // Attack state methods

    public boolean isAttacking() {
        return attacking;
    }

    public void startAttack(Direction facing) {
        if (attacking || parryInProgress || isStaggered()) {
            return;
        }
        // Determine the attack type
        if (attacksSinceLastSpin >= attacksUntilNextSpin && attacksSinceLastSpin > 0) {
            // Time for a spin attack (never on first attack since attacksSinceLastSpin > 0)
            currentMeleeType = MeleeAttackType.SPIN;
            attacksSinceLastSpin = 0;
            // Set next spin interval to 2-3 attacks
            attacksUntilNextSpin = 2 + (int)(Math.random() * 2);
        } else {
            // Randomly choose between basic and secondary
            currentMeleeType = Math.random() < 0.5 ? MeleeAttackType.BASIC : MeleeAttackType.SECONDARY;
            attacksSinceLastSpin++;
        }
        attacking = true;
        attackQueued = true;
        attackFacing = facing;
        if (animationController != null) {
            animationController.resetStateTime();
        }
    }

    public void endAttack() {
        attacking = false;
        attackQueued = false;
    }

    public boolean hasQueuedAttack() {
        return attackQueued;
    }

    public void clearAttackQueue() {
        attackQueued = false;
    }

    public Direction getAttackFacing() {
        return attackFacing;
    }


    public MeleeAttackType getCurrentMeleeType() {
        return currentMeleeType;
    }

    // Parry state methods

    public boolean isParrying() {
        return parryInProgress && parryWindowRemainingMs > 0.0;
    }

    public boolean isParryInProgress() {
        return parryInProgress;
    }

    public void startParry(Direction facing) {
        if (parryInProgress || attacking || isStaggered()) {
            return;
        }
        parryInProgress = true;
        parryWindowRemainingMs = PARRY_WINDOW_MS;
        setFacing(facing);
        if (animationController != null) {
            animationController.resetStateTime();
        }
    }

    public void tickParry(double deltaSeconds) {
        if (parryWindowRemainingMs > 0.0) {
            parryWindowRemainingMs = Math.max(0.0, parryWindowRemainingMs - deltaSeconds * 1000.0);
        }
        // Check if parry animation finished
        if (parryInProgress && animationController != null && animationController.isAnimationFinished()) {
            parryInProgress = false;
            parryWindowRemainingMs = 0.0;
        }
    }

    public void endParry() {
        parryInProgress = false;
        parryWindowRemainingMs = 0.0;
    }

    // Kick state methods

    public boolean isKicking() {
        return kickInProgress;
    }

    public void startKick(Direction facing) {
        if (kickInProgress || attacking || parryInProgress || isStaggered()) {
            return;
        }
        kickInProgress = true;
        setFacing(facing);
        if (animationController != null) {
            animationController.resetStateTime();
        }
    }

    public void endKick() {
        kickInProgress = false;
    }

    // Dash state methods

    public boolean isDashing() {
        return dashInProgress;
    }

    public void startDash(Direction direction) {
        if (dashInProgress || isStaggered()) return;

        dashInProgress = true;
        dashDistanceRemaining = AVATAR_DASH_DISTANCE;

        dashDirection = (direction != null) ? direction : facing; // fallback

        // Optional: also force facing to match dash direction
        setFacing(dashDirection);

        if (animationController != null) animationController.resetStateTime();
    }

    public void tickDash(double movedDistance) {
        dashDistanceRemaining = Math.max(0.0, dashDistanceRemaining - movedDistance);
        if (dashDistanceRemaining <= 1e-4) {
            dashInProgress = false;
        }
    }

    public void endDash() {
        dashInProgress = false;
        dashDistanceRemaining = 0.0;
    }

    public double getDashDistanceRemaining() {
        return dashDistanceRemaining;
    }

    @Override
    protected void onStaggered() {
        super.onStaggered();
        // Cancel ongoing actions when staggered
        attacking = false;
        attackQueued = false;
        parryInProgress = false;
        parryWindowRemainingMs = 0.0;
        kickInProgress = false;
    }
}