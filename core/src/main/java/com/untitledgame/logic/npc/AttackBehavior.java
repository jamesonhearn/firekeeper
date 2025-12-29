package com.untitledgame.logic.npc;

import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;

public class AttackBehavior implements AiBehavior {
    private static final double MELEE_HALF_EXTENT = 0.45;
    private Direction desired;

    private static final double MIN_ATTACK_DIST_SQ = 0.9 * 0.9;
    private static final double MAX_ATTACK_DIST_SQ = 1.8 * 1.8;

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

        // --- TOO CLOSE → BACK OFF ---
        if (distSq < MIN_ATTACK_DIST_SQ) {
            double len = Math.sqrt(distSq);
            if (len > 1e-6) {
                double backoffSpeed = 5.0; // or owner.getMoveSpeed() if you add it
                owner.setVelocity((-dx / len) * backoffSpeed, (-dy / len) * backoffSpeed);
            } else {
                owner.setVelocity(0, 0);
            }
            return;
        }

        // --- TOO FAR → DO NOTHING (SeekBehavior will handle movement) ---
        if (distSq > MAX_ATTACK_DIST_SQ) {
            owner.setVelocity(0, 0);
            return;
        }

        // --- IN RANGE → STOP & ATTACK ---
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
