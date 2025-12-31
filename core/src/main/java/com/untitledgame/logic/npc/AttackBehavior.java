package com.untitledgame.logic.npc;

import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;

public class AttackBehavior implements AiBehavior {
    private static final double MELEE_HALF_EXTENT = Npc.ATTACK_HALF_EXTENT;
    private static final int ATTACK_DAMAGE = 10;
    // Attack damage is queued at the peak of the attack animation (halfway through)
    // Attack animation is 20 ticks, so peak is at tick 10
    private static final int ATTACK_DAMAGE_TICK = 10;
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

        if (!owner.isAttacking() && owner.canAttack()) {
            double dist = Math.sqrt(distSq);
            if (dist >= minAttackDist && dist <= maxAttackDist) {
                owner.markAttacking();
            }
        }
        if (owner.isAttacking() && !owner.hasQueuedAttackDamage()
                && owner.getAttackAnimationTicks() == ATTACK_DAMAGE_TICK
                && view.overlapsAvatar(owner.posX(), owner.posY(),
                MELEE_HALF_EXTENT, MELEE_HALF_EXTENT)) {
            view.damageAvatar(ATTACK_DAMAGE, owner);
            owner.markAttackDamageQueued();
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
