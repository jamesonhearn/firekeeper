package com.untitledgame.logic.npc;

import com.untitledgame.logic.Avatar;
import com.untitledgame.logic.CombatService;
import com.untitledgame.logic.Entity;
import com.untitledgame.logic.TileType;

import java.util.Set;

public class WorldView {
    private final TileType[][] world;
    private final Set<Entity.Position> occupied;
    private final Avatar avatar;
    private final CombatService combatService;

    public WorldView(TileType[][] world, Avatar avatar, Set<Entity.Position> occupied, CombatService combatService) {
        this.world = world;
        this.avatar = avatar;
        this.occupied = occupied;
        this.combatService = combatService;
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= world.length || y >= world[0].length) {
            return false;
        }
        return world[x][y].equals(TileType.FLOOR) || world[x][y].equals(TileType.ELEVATOR);
    }

    public Avatar avatar() {
        return avatar;
    }

    public void damageAvatar(int amount, Entity source) {
        combatService.queueDamage(avatar, source, amount);
    }

    public boolean isOccupied(int x, int y) {
        return occupied.contains(new Entity.Position(x, y));
    }

    public Entity.Position avatarPosition() {
        return avatar.position();
    }

    public boolean overlapsAvatar(double centerX, double centerY, double halfWidth, double halfHeight) {
        double dx = Math.abs(centerX - avatar.posX());
        double dy = Math.abs(centerY - avatar.posY());
        return dx <= (halfWidth + Avatar.HITBOX_HALF) && dy <= (halfHeight + Avatar.HITBOX_HALF);
    }
}
