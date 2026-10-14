package com.zaig100.dargon.game;

/** Числа баланса, которые не лежат в данных уровней и префабов. */
public final class Balance {

    private Balance() {}

    /** Максимум здоровья. */
    public static final int MAX_HP = 40;

    /** Неуязвимость после удара, секунды. */
    public static final float INVULNERABILITY = 0.8f;

    /** Прочность одного щита и максимум суммарной прочности. */
    public static final int SHIELD = 15;

    public static final int SHIELD_MAX = 30;

    /** Пока щит цел: доля урона телу и доля урона щиту. */
    public static final float BODY_SHARE = 0.5f;

    public static final float SHIELD_SHARE = 0.25f;

    /** Шанс погибнуть от удара на 1 HP, даже со щитом. */
    public static final float LAST_STAND_DEATH = 0.1f;

    /** Урон стрелы и огня (разброс). */
    public static final int ARROW_MIN = 17;

    public static final int ARROW_MAX = 23;
    public static final int FIRE_MIN = 26;
    public static final int FIRE_MAX = 34;

    /** Зона лечения чинит щит не выше этого. */
    public static final int ZONE_SHIELD_REPAIR = 10;

    /** Казуал: переход на уровень лечит до этой доли максимума. */
    public static final float CASUAL_HEAL = 0.5f;
}
