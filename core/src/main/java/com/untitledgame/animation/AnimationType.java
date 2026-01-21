package com.untitledgame.animation;

/**
 * Defines the different types of animations that entities can perform.
 * Used by both player (Avatar) and NPCs for consistent animation management.
 */
public enum AnimationType {
    IDLE,
    WALK,
    RUN,
    DASH,
    ATTACK,
    DEATH,
    TAKE_DAMAGE,
    DODGE1, DODGE2,
    KICK,
    BLOCK  // Player-specific: shield blocking/parrying
}
