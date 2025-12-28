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
        desired = null;
        double dx = view.avatar().posX() - owner.posX();
        double dy = view.avatar().posY() - owner.posY();
        owner.setFacing(directionToward(dx, dy, owner.facing()));
        if (view.overlapsAvatar(owner.posX(), owner.posY(), MELEE_HALF_EXTENT, MELEE_HALF_EXTENT)) {
            view.damageAvatar(2, owner);
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
        return desired;
    }
}
