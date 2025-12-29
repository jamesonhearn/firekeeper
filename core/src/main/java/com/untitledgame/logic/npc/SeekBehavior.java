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
        desired = null;

        var avatarPos = view.avatarPosition();
        int ax = avatarPos.x();
        int ay = avatarPos.y();

        int currentDist = heuristic(owner.x(), owner.y(), ax, ay);

        // Stop seeking if already within combat distance
        // This gives enemies a comfortable attack range
        if (currentDist <= COMBAT_DISTANCE_SQUARED) {
            return;
        }

        // Copy all directions
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));

        // Sort by squared Eucl. distance + random jitter
        directions.sort(Comparator.comparingInt(dir -> {
            int nx = owner.x() + dir.getDx();
            int ny = owner.y() + dir.getDy();
            return heuristic(nx, ny, ax, ay) + RAND.nextInt(3);
        }));

        // choose first valid move
        for (Direction dir : directions) {
            int nx = owner.x() + dir.getDx();
            int ny = owner.y() + dir.getDy();

            int newDist = heuristic(nx, ny, ax, ay);
            if (newDist < COMBAT_DISTANCE_SQUARED) {
                continue;
            }

            if (view.isWalkable(nx, ny) && !view.isOccupied(nx, ny)) {
                desired = dir;
                return;
            }
        }
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
