package com.zaig100.dargon.game;

/**
 * Инструменты разработчика, которые влияют на правила игры. Действуют на всё прохождение (все
 * уровни сессии). Любое их использование помечает прохождение ({@link Progress#cheats()}): для него
 * выключаются достижения и не пишется статистика.
 */
public final class Cheats {

    /** Сквозь стены и закрытые двери. */
    public boolean noclip;

    /** Урон не действует. */
    public boolean immortal;

    /** Скрытое видно (как зелье «показать ловушки»). */
    public boolean revealAll;

    /** Тёмные уровни светлые. */
    public boolean noDarkness;

    /** Ускорение игрового времени: 1, 2, 4. */
    public int timeScale = 1;

    public boolean anyActive() {
        return noclip || immortal || revealAll || noDarkness || timeScale != 1;
    }

    /** Применить к уровню (при входе на него и при переключении). */
    public void apply(LevelRuntime level) {
        level.health().immortal = immortal || level.health().immortalForLevel;
        level.state().setRevealAll(revealAll);
        level.state().setDark(level.state().definition().dark() && !noDarkness);
    }
}
