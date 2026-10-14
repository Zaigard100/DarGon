package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.GameSystem;
import com.zaig100.dargon.game.Balance;
import com.zaig100.dargon.game.GameEvents;
import com.zaig100.dargon.game.component.Hazard;
import com.zaig100.dargon.game.component.Health;
import java.util.Random;

/**
 * Урон:
 *
 * <ul>
 *   <li>величина выпадает случайно из диапазона удара;
 *   <li>после удара — {@link Balance#INVULNERABILITY} неуязвимости (кроме урона зон);
 *   <li>щит: сначала урон телу — {@link Balance#BODY_SHARE} удара, затем щиту — {@link
 *       Balance#SHIELD_SHARE} (минимум 1); прочность 0 — щит сломан;
 *   <li>смертельный удар при целом щите оставляет 1 HP; на 1 HP со щитом удар убивает с шансом
 *       {@link Balance#LAST_STAND_DEATH};
 *   <li>{@link Hazard#KILL} (пролом) убивает сразу, щит не спасает.
 * </ul>
 *
 * Любой удар, даже поглощённый, даёт красную вспышку и запоминает вид урона (картинка смерти).
 */
public final class DamageSystem extends GameSystem {

    private ComponentMapper<Health> health;
    private Random random;
    private EntitySet living;

    @Override
    protected void initialize() {
        living = world.entities(Family.all(Health.class));
        events.subscribe(GameEvents.Damage.class, this::apply);
    }

    private void apply(GameEvents.Damage damage) {
        Health h = health.getOrNull(damage.target());
        if (h == null || h.isDead() || h.immortal) {
            return;
        }
        int before = h.hp;
        if (damage.min() == Hazard.KILL) {
            h.hp = 0;
        } else {
            if (!damage.periodic() && h.invulnerable > 0) {
                return;
            }
            int amount = damage.min() + random.nextInt(damage.max() - damage.min() + 1);
            if (damage.shieldable() && h.hasShield()) {
                shielded(h, amount);
            } else {
                h.hp = Math.max(0, h.hp - amount);
            }
            if (!damage.periodic()) {
                h.invulnerable = Balance.INVULNERABILITY;
            }
        }
        h.sinceDamage = 0;
        h.lastDamageKind = damage.kind();
        events.publish(new GameEvents.Hurt(damage.target(), before - h.hp, h.isDead()));
    }

    /** Удар при целом щите: тело, затем щит. */
    private void shielded(Health h, int amount) {
        int body = Math.round(amount * Balance.BODY_SHARE);
        int shield = Math.max(1, (int) (amount * Balance.SHIELD_SHARE));
        if (body >= h.hp) {
            if (h.hp == 1 && random.nextFloat() < Balance.LAST_STAND_DEATH) {
                h.hp = 0;
            } else {
                h.hp = 1;
            }
        } else {
            h.hp -= body;
        }
        h.shield = Math.max(0, h.shield - shield);
    }

    @Override
    protected void update(float delta) {
        // Таймеры вспышки и неуязвимости идут и у мёртвого игрока.
        for (int i = 0; i < living.size(); i++) {
            Health h = health.get(living.get(i));
            if (h.sinceDamage < Float.MAX_VALUE) {
                h.sinceDamage += delta;
            }
            if (h.invulnerable > 0) {
                h.invulnerable = Math.max(0, h.invulnerable - delta);
            }
        }
    }
}
