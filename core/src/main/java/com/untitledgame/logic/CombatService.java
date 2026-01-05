package com.untitledgame.logic;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.untitledgame.logic.npc.Npc;

/**
 * central controller for combat - entities enqueue damage and the
 * service resolves any armor, invulnerability frames, and death callbacks each tick.
 */
public class CombatService {
    public record DamageEvent(Entity target, Entity source, int amount) { }
    private AssetManager assets = new AssetManager();
    private AudioPlayer effects = new AudioPlayer(assets);
    private final Queue<DamageEvent> damageEvents = new ArrayDeque<>();
    private final Set<Entity> trackedEntities = new HashSet<>();
    private static final double AVATAR_STAGGER_MS = 350.0;
    private static final double NPC_STAGGER_MS = 1000.0;
    private static final String[] TAKE_DAMAGE_MELEE = new String[]{
            "audio/takedamage1.mp3",
            "audio/takedamage2.mp3"
    };

    public interface DamageListener {
        void onDamageApplied(Entity target, Entity source, int attemptedAmount, int appliedAmount);
    }

    public interface ParryChecker {
        boolean isParrying(Entity target);
    }

    public interface DodgeChecker {
        boolean shouldDodge(Entity target, Entity source);
    }
    public interface KickCounterChecker {
        boolean shouldKickCounter(Entity target, Entity source);
    }


    private ParryChecker parryChecker;
    private DodgeChecker dodgeChecker;
    private KickCounterChecker kickCounterChecker;
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

    public void setDodgeChecker(DodgeChecker checker) {
        this.dodgeChecker = checker;
    }

    public void setKickCounterChecker(KickCounterChecker checker) {
        this.kickCounterChecker = checker;
    }

    public void queueDamage(Entity target, Entity source, int amount) {
        if (target == null || target.health() == null) {
            return;
        }
        damageEvents.add(new DamageEvent(target, source, Math.max(0, amount)));
        if (target instanceof Npc) {
            effects.playRandomEffect(TAKE_DAMAGE_MELEE);
        };
    }

    public void setParryChecker(ParryChecker checker) {
        this.parryChecker = checker;
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
        if (dodgeChecker != null && !health.isInvulnerable()
                && dodgeChecker.shouldDodge(event.target(), event.source())) {

            if (damageListener != null) {
                damageListener.onDamageApplied(event.target(), event.source(), event.amount(), 0);
            }
            return;
        }

        if (kickCounterChecker != null && event.target() instanceof Npc
                && kickCounterChecker.shouldKickCounter(event.target(), event.source())) {

            if (event.source() != null && event.source().health() != null) {
                int kickDamage = event.amount();
                event.source().health().damage(kickDamage, event.target());
                applyStagger(event.source());
                applyKnockback(event.source(), event.target());
            }

            if (damageListener != null) {
                damageListener.onDamageApplied(event.target(), event.source(), event.amount(), 0);
            }
            return;
        }

        // Check if target is parrying
        if (parryChecker != null && parryChecker.isParrying(event.target())) {
            // Parry successful! Negate damage and stagger the attacker
            if (event.source() != null) {
                applyStagger(event.source());
            }
            // Notify listener that parry occurred (0 applied damage)
            if (damageListener != null) {
                damageListener.onDamageApplied(event.target(), event.source(), event.amount(), 0);
            }
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


    private void applyKnockback(Entity target, Entity source) {
        if (target == null || source == null) return;

        double dx = target.posX() - source.posX();
        double dy = target.posY() - source.posY();
        double dist = Math.hypot(dx, dy);

        if (dist < 1e-6) return;

        // 3 tiles over 0.25 seconds
        double distance = 3.0;
        double duration = 0.25;

        target.startKnockback(dx, dy, distance, duration);
    }

}
