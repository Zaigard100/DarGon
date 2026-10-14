package com.zaig100.dargon.game;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Прогресс прохождения и правила смерти.
 *
 * <ul>
 *   <li>При входе на уровень запоминается, с чем игрок вошёл (здоровье, монеты, инвентарь).
 *   <li>Уровень с {@code checkpoint} — контрольная точка: её снимок хранится отдельно. До первой
 *       контрольной точки откат идёт на начало игры.
 *   <li>Обычный режим: 1-я и 2-я смерть подряд — уровень заново, 3-я — откат к контрольной точке.
 *       Счётчик сбрасывается прохождением уровня и откатом.
 *   <li>Хардкор: любая смерть — откат к контрольной точке; сохраняется только контрольная точка.
 *   <li>Казуал: смерть — всегда текущий уровень заново.
 * </ul>
 */
public final class Progress {

    /** Смерть подряд, после которой — откат. */
    public static final int DEATHS_BEFORE_ROLLBACK = 3;

    /** Набор «уровень + с чем вошёл». */
    public record Snapshot(String level, PlayerState player) {
        public Snapshot copy() {
            return new Snapshot(level, player.copy());
        }
    }

    /** Что делать после смерти. */
    public enum Death {
        /** Начать текущий уровень заново. */
        RESTART,
        /** Откат к контрольной точке (или началу игры). */
        ROLLBACK
    }

    private final GameMode mode;
    private final String pack;
    private Snapshot entry;
    private Snapshot checkpoint;
    private int deathsInRow;
    private double playTime;
    private boolean cheats;
    private final Run run = new Run();

    /**
     * Что прохождение помнит для достижений: начато ли с первого уровня, смерти и покупки за
     * прохождение, что использовалось на пройденных уровнях, состояние отрезков «since…level».
     */
    public static final class Run {
        /** Прохождение начато с первого уровня пака (а не с открытого отрезка). */
        public boolean fromStart;

        public int deaths;
        public int purchases;

        /** Что случалось на пройденных уровнях: damage, potion, torch, purchase… */
        public final Set<String> used = new TreeSet<>();

        /** Отрезки достижений: id → нарушено ли условие (нет ключа — отрезок не начат). */
        public final Map<String, Boolean> segments = new TreeMap<>();
    }

    private final long created;

    public Progress(
            GameMode mode,
            String pack,
            Snapshot entry,
            Snapshot checkpoint,
            int deathsInRow,
            double playTime,
            long created) {
        this.mode = mode;
        this.pack = pack;
        this.entry = entry;
        this.checkpoint = checkpoint;
        this.deathsInRow = deathsInRow;
        this.playTime = playTime;
        this.created = created;
    }

    /** Новая игра: старт — он же первая «контрольная точка». */
    public static Progress newGame(
            GameMode mode, String pack, String start, PlayerState player, long now) {
        Snapshot first = new Snapshot(start, player.copy());
        return new Progress(mode, pack, first, first.copy(), 0, 0, now);
    }

    /**
     * Игрок вошёл на уровень.
     *
     * @param passed пришёл с предыдущего уровня (а не перезапуск) — счётчик смертей сбрасывается
     */
    public void entered(String level, boolean isCheckpoint, PlayerState player, boolean passed) {
        entry = new Snapshot(level, player.copy());
        if (isCheckpoint) {
            checkpoint = entry.copy();
        }
        if (passed) {
            deathsInRow = 0;
        }
    }

    /** Игрок умер: решение принимается сразу и сохраняется (выход в меню его не отменяет). */
    public Death died() {
        if (mode == GameMode.CASUAL) {
            return Death.RESTART;
        }
        deathsInRow++;
        if (mode == GameMode.HARDCORE || deathsInRow >= DEATHS_BEFORE_ROLLBACK) {
            deathsInRow = 0;
            entry = checkpoint.copy();
            return Death.ROLLBACK;
        }
        return Death.RESTART;
    }

    /** Сколько ещё смертей до отката (после текущей). */
    public int attemptsLeft() {
        return switch (mode) {
            case CASUAL -> Integer.MAX_VALUE;
            case HARDCORE -> 0;
            default -> DEATHS_BEFORE_ROLLBACK - deathsInRow;
        };
    }

    /** «Начать уровень заново» из паузы: в хардкоре — к контрольной точке. */
    public Snapshot restartTarget() {
        return mode == GameMode.HARDCORE ? checkpoint.copy() : entry.copy();
    }

    /** Откуда продолжать после загрузки: в хардкоре — только с контрольной точки. */
    public Snapshot resumeTarget() {
        return mode == GameMode.HARDCORE ? checkpoint.copy() : entry.copy();
    }

    /**
     * В прохождении пользовались инструментами разработчика: достижения выключены, статистика не
     * пишется. Пометка не снимается.
     */
    public Run run() {
        return run;
    }

    public boolean cheats() {
        return cheats;
    }

    public void markCheats() {
        cheats = true;
    }

    public void addPlayTime(double seconds) {
        playTime += seconds;
    }

    public GameMode mode() {
        return mode;
    }

    public String pack() {
        return pack;
    }

    public Snapshot entry() {
        return entry;
    }

    public Snapshot checkpoint() {
        return checkpoint;
    }

    public int deathsInRow() {
        return deathsInRow;
    }

    public double playTime() {
        return playTime;
    }

    public long created() {
        return created;
    }
}
