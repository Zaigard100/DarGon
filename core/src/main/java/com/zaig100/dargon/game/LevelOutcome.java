package com.zaig100.dargon.game;

/**
 * Чем закончился уровень. Пока уровень идёт — {@link #NONE}.
 *
 * @param next следующий уровень для {@link Kind#EXIT}
 */
public record LevelOutcome(Kind kind, String next) {

    /** Вид исхода. */
    public enum Kind {
        NONE,
        /** Игрок ушёл по лестнице на уровень {@code next}. */
        EXIT,
        /** Лестница без продолжения: игра пройдена. */
        GAME_END
    }

    public static final LevelOutcome NONE = new LevelOutcome(Kind.NONE, null);

    public static LevelOutcome exit(String next) {
        return next == null
                ? new LevelOutcome(Kind.GAME_END, null)
                : new LevelOutcome(Kind.EXIT, next);
    }
}
