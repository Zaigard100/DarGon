package com.zaig100.dargon.game;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Локальная статистика по уровням: сколько раз входили, умирали (и от чего), проходили, сколько
 * времени провели и какие предметы потратили. Хранится только на устройстве и нужна, чтобы
 * балансировать игру по данным, а не наугад.
 */
public final class PlayStats {

    /** Статистика одного уровня. */
    public static final class Level {
        public final String id;
        public int entries;
        public int completions;
        public double totalTime;

        /** Лучшее время прохождения, секунды; 0 — ещё не проходили. */
        public double bestTime;

        public final Map<String, Integer> deaths = new TreeMap<>();
        public final Map<String, Integer> itemsUsed = new TreeMap<>();

        public Level(String id) {
            this.id = id;
        }

        public int totalDeaths() {
            int sum = 0;
            for (int n : deaths.values()) {
                sum += n;
            }
            return sum;
        }
    }

    private final Map<String, Level> levels = new TreeMap<>();

    public Level level(String id) {
        return levels.computeIfAbsent(id, Level::new);
    }

    public Collection<Level> levels() {
        return Collections.unmodifiableCollection(levels.values());
    }

    /** Стереть всю статистику. */
    public void clear() {
        levels.clear();
    }

    public void entered(String level) {
        level(level).entries++;
    }

    public void died(String level, String cause, double time) {
        Level l = level(level);
        l.deaths.merge(cause == null ? "unknown" : cause, 1, Integer::sum);
        l.totalTime += time;
    }

    public void completed(String level, double time) {
        Level l = level(level);
        l.completions++;
        l.totalTime += time;
        if (l.bestTime == 0 || time < l.bestTime) {
            l.bestTime = time;
        }
    }

    /** Время на уровне без смерти и прохождения (выход в меню, перезапуск). */
    public void left(String level, double time) {
        level(level).totalTime += time;
    }

    public void used(String level, String item) {
        level(level).itemsUsed.merge(item, 1, Integer::sum);
    }

    public int totalDeaths() {
        int sum = 0;
        for (Level l : levels.values()) {
            sum += l.totalDeaths();
        }
        return sum;
    }

    public double totalTime() {
        double sum = 0;
        for (Level l : levels.values()) {
            sum += l.totalTime;
        }
        return sum;
    }
}
