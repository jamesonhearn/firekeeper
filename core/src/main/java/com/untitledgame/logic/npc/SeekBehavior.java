package com.untitledgame.logic.npc;

import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SeekBehavior implements AiBehavior {
    private Direction desired;

    // rng for tie-breaking so doesn't need NPC RNG
    private static final Random RAND = new Random();

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
    }
    private static final int COMBAT_DISTANCE_SQUARED = 2;

    @Override
    public void onTick(Npc owner, WorldView view) {
        double dx = view.avatarPosition().x() - owner.posX();
        double dy = view.avatarPosition().y() - owner.posY();

        double distSq = dx * dx + dy * dy;


        if (distSq <= COMBAT_DISTANCE_SQUARED) {
            owner.setVelocity(0, 0);
            return;
        }

        double dist = Math.sqrt(distSq);

        double stopRadius = owner.npcType().getMinAttackDistance() + 0.05;
        if (dist < stopRadius) {
            owner.setVelocity(0, 0);
        }

        if (dist < 1e-6) {
            owner.setVelocity(0, 0);
            return;
        }

        double speed = 5.0;
        owner.setVelocity((dx / dist) * speed, (dy / dist) * speed);

        owner.setFacing(Direction.fromVelocity(dx, dy));
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }

    private int heuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy; // squared distance
    }
}
