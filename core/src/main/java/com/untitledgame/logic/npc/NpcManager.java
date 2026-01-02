package com.untitledgame.logic.npc;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.untitledgame.animation.AnimationController;
import com.untitledgame.animation.AnimationFactory;
import com.untitledgame.logic.Avatar;
import com.untitledgame.logic.CombatService;
import com.untitledgame.logic.Entity;
import com.untitledgame.logic.HealthComponent;
import com.untitledgame.assets.TileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Coordinator for NPC creation, updates, and lifecycle management.
 * Manages NPC spawning, AI tick updates, collision detection, and combat interactions.
 * NPCs use centralized AnimationController for consistent animation management.
 */
public class NpcManager {
    private final Random rng;
    private final List<Npc> npcs = new ArrayList<>();
    private final List<Corpse> corpses = new ArrayList<>();
    private final CombatService combatService;
    private final TextureAtlas textureAtlas;
    private Consumer<Npc> deathHandler = npc -> { };
    private Runnable attackSoundCallback;
    private final int maxAttempts = 500;
    /**lookup of NPCs by tile for hitbox collision */
    private final Map<Entity.Position, List<Npc>> npcByTile = new HashMap<>();
    private List<Integer> availableVariants = new ArrayList<>(List.of(0));
    private static final double NPC_SPEED = 5.0;

    private static final int DEFAULT_NPC_COUNT = 10;

    public NpcManager(Random rng, CombatService combatService, TextureAtlas textureAtlas) {
        this.rng = rng;
        this.combatService = combatService;
        this.textureAtlas = textureAtlas;
    }

    public List<Npc> npcs() {
        return npcs;
    }

    public double moveSpeed() {
        return NPC_SPEED;
    }

    public void setAvailableVariants(List<Integer> variants) {
        if (variants == null || variants.isEmpty()) {
            availableVariants = new ArrayList<>(List.of(0));
            return;
        }
        availableVariants = new ArrayList<>(variants);
    }


    /**
     * Set the callback to be invoked when NPCs start attacking.
     * @param callback The callback to run when an NPC attacks
     */
    public void setAttackSoundCallback(Runnable callback) {
        this.attackSoundCallback = callback;
    }



    /**
     * Spawn handful of NPCs on random floors, avoiding the avatars starting tile
     */
    public void spawn(TileType[][] world, int avoidX, int avoidY) {
        npcs.clear();
        npcByTile.clear();
        corpses.clear();
        int attempts = 0;
        while (npcs.size() < DEFAULT_NPC_COUNT && attempts < maxAttempts) {
            attempts += 1;
            int x = rng.nextInt(world.length);
            int y = rng.nextInt(world[0].length);
            if (!world[x][y].equals(TileType.FLOOR)) {
                continue;
            }
            if (x == avoidX && y == avoidY) {
                continue;
            }
            int variant = selectVariant();
            long npcSeed = rng.nextLong();
            HealthComponent health = new HealthComponent(10, 10, 0, 8);
            
            // Create animation controller with all animation types
            AnimationController animationController = AnimationFactory.createNpcController(textureAtlas, variant);
            
            Npc npc = new Npc(x, y, new Random(npcSeed), npcSeed, variant, animationController, health);
            health.addDeathCallback(entity -> handleNpcDeath((Npc) entity));
            // Set attack sound callback if available
            if (attackSoundCallback != null) {
                npc.setAttackSoundCallback(attackSoundCallback);
            }
            combatService.register(npc);
            npcs.add(npc);
            addNpcPosition(new Entity.Position(x, y), npc);
        }
    }

    /**
     * advance all NPCs by one tick with collision check against walls, avatar, other npcs.
     */
    public void tick(TileType[][] world, Avatar avatar) {
        rebuildIndex();
        Entity.Position avatarPos = new Entity.Position(avatar.x(), avatar.y());
        Set<Entity.Position> occupied = buildOccupiedSet(avatarPos);
        WorldView sharedView = new WorldView(world, avatar, occupied, combatService);

        for (Npc npc : npcs) {
            npc.tick(sharedView);
        }
    }

    public List<Corpse> corpses() {
        return corpses;
    }

    public void damageInArea(double centerX, double centerY, double halfWidth, double halfHeight, Entity source, int amount) {
        if (amount <= 0) {
            return;
        }
        for (Npc npc : npcs) {
            double dx = Math.abs(centerX - npc.posX());
            double dy = Math.abs(centerY - npc.posY());
            if (dx <= (halfWidth + Npc.HITBOX_HALF) && dy <= (halfHeight + Npc.HITBOX_HALF)) {
                combatService.queueDamage(npc, source, amount);
            }
        }
    }

    private Set<Entity.Position> buildOccupiedSet(Entity.Position avatarPos) {
        Set<Entity.Position> occupied = new HashSet<>();

        // Avatar tile ALWAYS blocked
        occupied.add(avatarPos);

        for (Npc npc : npcs) {

            int dx = Math.abs(npc.x() - avatarPos.x());
            int dy = Math.abs(npc.y() - avatarPos.y());
            int dist = dx + dy;

            Entity.Position pos = new Entity.Position(npc.x(), npc.y());

            // Far from player = normal collision (1 NPC per tile)
            if (dist > 4) {
                occupied.add(pos);
            } else if (dist > 2) {
                if (npcCountAt(pos) >= 3) {
                    occupied.add(pos);
                }
            } else {
                continue;
            }
        }

        return occupied;
    }
    private int npcCountAt(Entity.Position pos) {
        List<Npc> list = npcByTile.get(pos);
        return list == null ? 0 : list.size();
    }


    public boolean isNpcAt(int x, int y) {
        return npcByTile.containsKey(new Entity.Position(x, y));
    }



    public Npc npcAtTile(int x, int y) {
        List<Npc> occupants = npcByTile.get(new Entity.Position(x, y));
        if (occupants == null || occupants.isEmpty()) {
            return null;
        }
        return occupants.get(0);
    }


    public List<Npc> npcsAtTile(int x, int y) {
        List<Npc> occupants = npcByTile.get(new Entity.Position(x, y));
        return occupants == null ? List.of() : List.copyOf(occupants);
    }




    public void setDeathHandler(Consumer<Npc> deathHandler) {
        this.deathHandler = deathHandler;
    }

    public void rebuildIndex() {
        npcByTile.clear();
        for (Npc npc : npcs) {
            addNpcPosition(new Entity.Position(npc.x(), npc.y()), npc);
        }
    }

    private int selectVariant() {
        if (availableVariants.isEmpty()) {
            return 0;
        }
        return availableVariants.get(rng.nextInt(availableVariants.size()));
    }

    private void handleNpcDeath(Npc npc) {
        removeNpcPosition(new Entity.Position(npc.x(), npc.y()), npc);
        npcs.remove(npc);
        // Create corpse using static tile representation
        // Future enhancement: Could use death animation final frame or dedicated corpse sprites
        corpses.add(new Corpse(npc.x(), npc.y(), com.untitledgame.assets.Tileset.NPC_CORPSE));
        combatService.unregister(npc);
        deathHandler.accept(npc);
    }

    public void restoreState(List<Npc> restoredNpcs, List<Corpse> restoredCorpses) {
        npcs.clear();
        npcByTile.clear();
        corpses.clear();

        for (Npc npc : restoredNpcs) {
            npcs.add(npc);
            addNpcPosition(new Entity.Position(npc.x(), npc.y()), npc);
            npc.health().addDeathCallback(entity -> handleNpcDeath((Npc) entity));
            combatService.register(npc);
        }

        if (restoredCorpses != null) {
            corpses.addAll(restoredCorpses);
        }
    }

    private void addNpcPosition(Entity.Position position, Npc npc) {
        npcByTile.computeIfAbsent(position, p -> new ArrayList<>()).add(npc);
    }

    private void removeNpcPosition(Entity.Position position, Npc npc) {
        List<Npc> occupants = npcByTile.get(position);
        if (occupants == null) {
            return;
        }
        occupants.remove(npc);
        if (occupants.isEmpty()) {
            npcByTile.remove(position);
        }
    }
}
