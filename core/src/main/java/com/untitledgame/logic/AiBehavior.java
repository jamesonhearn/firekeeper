package com.untitledgame.logic;

import com.untitledgame.logic.npc.Npc;
import com.untitledgame.logic.npc.WorldView;

public interface AiBehavior {
    void onEnterState(Npc owner);
    void onTick(Npc owner, WorldView view);
    Direction desiredMove();
}
