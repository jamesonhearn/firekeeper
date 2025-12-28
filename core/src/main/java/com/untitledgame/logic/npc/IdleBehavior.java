package com.untitledgame.logic.npc;

import com.untitledgame.logic.AiBehavior;
import com.untitledgame.logic.Direction;

public class IdleBehavior implements AiBehavior {
    private Direction desired;

    @Override
    public void onEnterState(Npc owner) {
        desired = null;
        owner.setVelocity(0, 0);
    }

    @Override
    public void onTick(Npc owner, WorldView view) {
        desired = null;
    }

    @Override
    public Direction desiredMove() {
        return desired;
    }
}
