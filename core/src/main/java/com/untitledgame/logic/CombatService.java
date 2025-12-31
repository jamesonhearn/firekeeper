package com.untitledgame.logic;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import com.untitledgame.logic.npc.Npc;

/**
 * central controller for combat - entities enqueue damage and the
 * service resolves any armor, invulnerability frames, and death callbacks each tick.
 */
public class CombatService {
    public record DamageEvent(Entity target, Entity source, int amount) { }

    private final Queue<DamageEvent> damageEvents = new ArrayDeque<>();
    private final Set<Entity> trackedEntities = new HashSet<>();
    private static final double AVATAR_STAGGER_MS = 350.0;
    private static final double NPC_STAGGER_MS = 400.0;
    public interface DamageListener {
        void onDamageApplied(Entity target, Entity source, int attemptedAmount, int appliedAmount);
    }

    private DamageListener damageListener;
    public void register(Entity entity) {
        if (entity != null) {
            trackedEntities.add(entity);
        }
    }

    public void unregister(Entity entity) {
        trackedEntities.remove(entity);
    }

    public void setDamageListener(DamageListener listener) {
        this.damageListener = listener;
    }

    public void queueDamage(Entity target, Entity source, int amount) {
        if (target == null || target.health() == null) {
            return;
        }
        damageEvents.add(new DamageEvent(target, source, Math.max(0, amount)));
    }

    private void applyStagger(Entity target) {
        if (target == null) {
            return;
        }
        if (target instanceof Avatar avatarTarget) {
            if (avatarTarget.health() != null && !avatarTarget.health().isDepleted()) {
                avatarTarget.setStagger(AVATAR_STAGGER_MS);
            }
            return;
        }
        if (target instanceof Npc npcTarget) {
            if (npcTarget.health() != null && !npcTarget.health().isDepleted()) {
                npcTarget.setStagger(NPC_STAGGER_MS);
            }
            return;
        }
        target.setStagger(NPC_STAGGER_MS);
    }

    /**
     * resolve invulnerability timers and apply queued damage for frame
     */
    public void tick() {
        for (Entity entity : trackedEntities) {
            if (entity.health() != null) {
                entity.health().tickInvulnerability();
            }
        }

        int eventsToProcess = damageEvents.size();
        for (int i = 0; i < eventsToProcess; i += 1) {
            DamageEvent event = damageEvents.poll();
            if (event == null) {
                continue;
            }
            applyDamage(event);
        }
    }

    private void applyDamage(DamageEvent event) {
        HealthComponent health = event.target().health();
        if (health == null) {
            return;
        }
        int applied = health.damage(event.amount(), event.target());
        if (applied > 0) {
            if (health.isDepleted()) {
                event.target().clearStagger();
            } else {
                applyStagger(event.target());
            }
        }
        if (damageListener != null) {
            damageListener.onDamageApplied(event.target(), event.source(), event.amount(), applied);
        }
    }
}
