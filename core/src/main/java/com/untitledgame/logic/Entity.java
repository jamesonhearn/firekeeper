package com.untitledgame.logic;

/**
 * Class for Entity objects - will use to abstract NPC/Enemy objects and set defined characteristics
 * Will essentially need to port over everything that Avatar has, plus additional details like RNG, movement
 * countdowns (when to go from idle to moving to seeking) and positioning details
 */
public class Entity {
    /** Entity position in world-space tile units, measured at the hitbox center. */
    protected double posX;
    protected double posY;
    protected Direction facing = Direction.DOWN;
    protected double velocityX = 0.0;
    protected double velocityY = 0.0;
    private double staggerRemainingMs = 0.0;
    private boolean staggered = false;
    protected HealthComponent health;


    private boolean knockbackActive;
    private double knockbackRemaining;
    private double knockbackSpeed;
    private double knockbackDirX;
    private double knockbackDirY;

    public Entity(int x, int y) {
        this(x, y, new HealthComponent(1));
    }

    public Entity(int x, int y, HealthComponent health) {
        setPosition(x + 0.5, y + 0.5);
        this.health = health;
    }

    public int x() {
        return (int) Math.floor(posX);
    }
    public int y() {
        return (int) Math.floor(posY);
    }

    public Position position() {
        return new Position(x(), y());
    }

    public double posX() {
        return posX;
    }

    public double posY() {
        return posY;
    }

    public void setPosition(int newX, int newY) {
        setPosition(newX + 0.5, newY + 0.5);
    }

    public void setPosition(double newX, double newY) {
        this.posX = newX;
        this.posY = newY;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public double velocityX() {
        return velocityX;
    }
    public double velocityY() {

        return velocityY;
    }
    public void setVelocity(double newVelocityX, double newVelocityY) {
        this.velocityX = newVelocityX;
        this.velocityY = newVelocityY;
    }

    public HealthComponent health() {

        return health;
    }

    public void startKnockback(double dirX, double dirY, double distance, double durationSeconds) {
        double len = Math.hypot(dirX, dirY);
        if (len < 1e-6) return;

        knockbackDirX = dirX / len;
        knockbackDirY = dirY / len;

        knockbackRemaining = distance;
        knockbackSpeed = distance / durationSeconds;

        knockbackActive = true;
    }

    public boolean updateKnockback(double deltaSeconds) {
        if (!knockbackActive) return false;

        double step = knockbackSpeed * deltaSeconds;
        step = Math.min(step, knockbackRemaining);

        setVelocity(knockbackDirX * knockbackSpeed,
                knockbackDirY * knockbackSpeed);

        knockbackRemaining -= step;

        if (knockbackRemaining <= 0.0001) {
            knockbackActive = false;
            setVelocity(0, 0);
        }

        return true;
    }


    public double staggerRemainingMs() {
        return staggerRemainingMs;
    }

    public boolean isStaggered() {
        return staggered;
    }

    public void setStagger(double durationMs) {
        if (durationMs <= 0) {
            return;
        }
        staggerRemainingMs = Math.max(staggerRemainingMs, durationMs);
        staggered = true;
        onStaggered();
    }

    public void clearStagger() {
        staggerRemainingMs = 0.0;
        staggered = false;
    }

    public void tickStagger(double deltaSeconds) {
        if (staggerRemainingMs <= 0.0) {
            staggerRemainingMs = 0.0;
            staggered = false;
            return;
        }
        staggerRemainingMs = Math.max(0.0, staggerRemainingMs - deltaSeconds * 1000.0);
        if (staggerRemainingMs <= 0.0) {
            staggered = false;
        }
    }

    protected void onStaggered() {
        setVelocity(0.0, 0.0);
    }

    public record Position(int x, int y) { }
}
