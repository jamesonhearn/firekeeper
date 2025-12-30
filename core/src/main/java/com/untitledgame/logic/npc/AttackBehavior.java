package com.untitledgame.logic.npc;

import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;

public class AttackBehavior implements AiBehavior {
    private static final double MELEE_HALF_EXTENT = 0.45;
    private Direction desired;

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
    }

    @Override
    public void onTick(Npc owner, WorldView view) {
        double dx = view.avatar().posX() - owner.posX();
        double dy = view.avatar().posY() - owner.posY();

        double distSq = dx * dx + dy * dy;

        // Always face player
        owner.setFacing(directionToward(dx, dy, owner.facing()));

        // Get the NPC's attack distance configuration
        double minAttackDist = owner.npcType().getMinAttackDistance();
        double maxAttackDist = owner.npcType().getMaxAttackDistance();
        double minAttackDistSq = minAttackDist * minAttackDist;
        double maxAttackDistSq = maxAttackDist * maxAttackDist;

        // --- TOO CLOSE THEN BACK OFF ---
        if (distSq < minAttackDistSq) {
            double distance = Math.sqrt(distSq);
            if (distance > 1e-6) {
                double backoffSpeed = 5.0;
                owner.setVelocity((-dx / distance) * backoffSpeed, (-dy / distance) * backoffSpeed);
            } else {
                owner.setVelocity(0, 0);
            }
            return;
        }

        // --- TOO FAR THEN MOVE CLOSER (only if not actively attacking) ---
        if (distSq > maxAttackDistSq && !owner.isAttacking()) {
            double distance = Math.sqrt(distSq);
            if (distance > 1e-6) {
                double approachSpeed = 5.0;
                owner.setVelocity((dx / distance) * approachSpeed, (dy / distance) * approachSpeed);
            } else {
                owner.setVelocity(0, 0);
            }
            return;
        }

        // --- IN RANGE THEN STOP & ATTACK ---
        owner.setVelocity(0, 0);

        if (view.overlapsAvatar(owner.posX(), owner.posY(),
                MELEE_HALF_EXTENT, MELEE_HALF_EXTENT)) {
            owner.markAttacking();
        }
    }


    private Direction directionToward(double dx, double dy, Direction fallback) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (dy != 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        return fallback;
    }

    @Override
    public Direction desiredMove() {
        return null;
    }
}
