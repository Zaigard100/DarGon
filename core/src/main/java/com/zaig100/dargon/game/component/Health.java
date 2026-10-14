package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/** Здоровье, щит и последствия урона. */
public final class Health implements Component {
    public int hp;
    public int max;

    /** Прочность щита (0 — щита нет). */
    public int shield;

    /** Сколько ещё секунд урон не действует (после удара). */
    public float invulnerable;

    /** Бессмертие (пасхалка {@code inf}): урон не действует. */
    public boolean immortal;

    /** Бессмертие от кубика: до конца уровня (не зависит от инструментов разработчика). */
    public boolean immortalForLevel;

    /** Вид последнего урона: по нему выбирается картинка смерти. */
    public String lastDamageKind;

    /** Секунды с последнего урона — для красной вспышки. */
    public float sinceDamage = Float.MAX_VALUE;

    public boolean hasShield() {
        return shield > 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    @Override
    public void reset() {
        hp = 0;
        max = 0;
        shield = 0;
        invulnerable = 0;
        immortal = false;
        immortalForLevel = false;
        lastDamageKind = null;
        sinceDamage = Float.MAX_VALUE;
    }
}
