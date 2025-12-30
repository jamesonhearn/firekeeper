package com.untitledgame.logic.npc;

/**
 * Configuration for different NPC types.
 * Defines behavior parameters like attack distance for different enemy types.
 */
public class NpcType {
    private static final double MIN_DISTANCE_THRESHOLD = 0.5;

    private final String name;
    private final double attackDistance;
    private final double attackRangeTolerance;

    /**
     * Creates an NPC type configuration.
     *
     * @param name The name of the NPC type
     * @param attackDistance The preferred attack distance from the player (in tiles)
     * @param attackRangeTolerance How much distance variation is acceptable before adjusting
     *                            (e.g., 0.5 means enemy won't move if within ±0.5 tiles of attackDistance)
     */
    public NpcType(String name, double attackDistance, double attackRangeTolerance) {
        this.name = name;
        this.attackDistance = attackDistance;
        this.attackRangeTolerance = attackRangeTolerance;
    }

    public String getName() {
        return name;
    }

    public double getAttackDistance() {
        return attackDistance;
    }

    public double getAttackRangeTolerance() {
        return attackRangeTolerance;
    }

    /**
     * Get the minimum acceptable distance before backing off.
     */
    public double getMinAttackDistance() {
        return Math.max(MIN_DISTANCE_THRESHOLD, attackDistance - attackRangeTolerance);
    }

    /**
     * Get the maximum acceptable distance before advancing.
     */
    public double getMaxAttackDistance() {
        return attackDistance + attackRangeTolerance;
    }

    // Predefined NPC types
    public static final NpcType MELEE = new NpcType("melee", 1.5, 0.5);
    public static final NpcType RANGED = new NpcType("ranged", 5.0, 1.0);

    // Default type for existing NPCs
    public static final NpcType DEFAULT = MELEE;

    /**
     * Get an NPC type by name. Returns DEFAULT if name is null or not recognized.
     */
    public static NpcType fromName(String name) {
        if (name == null) {
            return DEFAULT;
        }
        return switch (name.toLowerCase()) {
            case "melee" -> MELEE;
            case "ranged" -> RANGED;
            default -> DEFAULT;
        };
    }
}