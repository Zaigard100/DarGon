package com.zaig100.dargon.game.achievement;

import com.zaig100.dargon.game.GameMode;
import com.zaig100.dargon.game.Progress;
import com.zaig100.dargon.level.PackDefinition;
import com.zaig100.dargon.level.PackDefinition.Achievement;
import com.zaig100.dargon.level.PackDefinition.Condition;
import com.zaig100.dargon.save.AchievementStore;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Проверяет условия достижений пака по событиям игры.
 *
 * <p>Что случалось на уровне (урон, зелье, факел…), копится за <b>попытку</b>: перезапуск уровня
 * начинает чистую попытку. В прохождение и в отрезки «since…level» попадает только попытка, которая
 * закончилась проходом уровня. Смерть нарушает отрезки с ограничением {@code death}.
 *
 * <p>В казуале и в прохождениях с инструментами разработчика достижения не выдаются и счётчики не
 * растут.
 */
public final class AchievementTracker {

    private final PackDefinition pack;
    private final AchievementStore store;
    private final Consumer<Achievement> onUnlock;
    private final LongSupplier clock;
    private final Set<String> attempt = new HashSet<>();
    private Progress progress;

    public AchievementTracker(
            PackDefinition pack,
            AchievementStore store,
            Consumer<Achievement> onUnlock,
            LongSupplier clock) {
        this.pack = pack;
        this.store = store;
        this.onUnlock = onUnlock;
        this.clock = clock;
    }

    /** Прохождение, к которому относятся события. */
    public void bind(Progress progress) {
        this.progress = progress;
    }

    private boolean active() {
        return progress != null && progress.mode() != GameMode.CASUAL && !progress.cheats();
    }

    private String mode() {
        return progress.mode().id();
    }

    // ------------------------------------------------------------------ события

    /** Начата попытка уровня. */
    public void levelStarted() {
        attempt.clear();
    }

    public void damaged(int amount) {
        if (amount > 0) {
            attempt.add("damage");
        }
    }

    /** Использован предмет: зелье, факел, зелье зоркости. */
    public void itemUsed(String itemId) {
        switch (itemId) {
            case "potion" -> attempt.add("potion");
            case "torch" -> attempt.add("torch");
            case "trap_sight" -> attempt.add("trapSight");
            default -> {}
        }
    }

    public void shieldBroken() {
        attempt.add("shieldBreak");
    }

    public void purchased() {
        attempt.add("purchase");
        if (!active()) {
            return;
        }
        progress.run().purchases++;
        count("purchases", 1);
    }

    public void coinsCollected(int amount) {
        if (active() && amount > 0) {
            count("coins", amount);
        }
    }

    /** Смерть: счётчики, достижения «погибнуть от…», нарушение отрезков без смертей. */
    public void died(String kind) {
        if (!active()) {
            return;
        }
        progress.run().deaths++;
        progress.run().used.add("death");
        for (Achievement a : pack.achievements()) {
            Condition c = a.when();
            if (c.since() != null && c.without().contains("death")) {
                progress.run().segments.computeIfPresent(a.id(), (id, violated) -> true);
            }
            if (c.type().equals("death")
                    && c.allows(mode())
                    && kind != null
                    && kind.equals(c.kind())) {
                unlock(a);
            }
        }
        count("deaths", 1);
    }

    /** Уровень пройден (выход по лестнице). */
    public void levelCompleted(String level) {
        if (!active()) {
            return;
        }
        progress.run().used.addAll(attempt);
        for (Achievement a : pack.achievements()) {
            Condition c = a.when();
            if (!c.type().equals("level") || !c.allows(mode())) {
                continue;
            }
            boolean clean = !intersects(c.without(), attempt);
            if (c.since() == null) {
                if ((c.level() == null || c.level().equals(level)) && clean) {
                    unlock(a);
                }
                continue;
            }
            var segments = progress.run().segments;
            if (level.equals(c.since())) {
                segments.put(a.id(), !clean);
            } else if (segments.containsKey(a.id()) && !clean) {
                segments.put(a.id(), true);
            }
            if (level.equals(c.level()) && Boolean.FALSE.equals(segments.get(a.id()))) {
                unlock(a);
            }
            if (level.equals(c.level())) {
                segments.remove(a.id());
            }
        }
        count("levels", 1);
        attempt.clear();
    }

    /** Игра пройдена (лестница без продолжения). */
    public void gameCompleted() {
        if (!active() || !progress.run().fromStart) {
            return;
        }
        Progress.Run run = progress.run();
        for (Achievement a : pack.achievements()) {
            Condition c = a.when();
            if (!c.type().equals("game") || !c.allows(mode())) {
                continue;
            }
            boolean ok =
                    (c.maxTime() == null || progress.playTime() <= c.maxTime())
                            && (c.maxDeaths() == null || run.deaths <= c.maxDeaths())
                            && !intersects(c.without(), run.used);
            if (ok) {
                unlock(a);
            }
        }
    }

    /** {@code achieve('id')} из скрипта уровня. */
    public void scripted(String id) {
        Achievement a = pack.achievement(id);
        if (active() && a != null && a.when().type().equals("script") && a.when().allows(mode())) {
            unlock(a);
        }
    }

    // ------------------------------------------------------------------ внутреннее

    private void count(String counter, int amount) {
        int value = store.add(counter, amount);
        for (Achievement a : pack.achievements()) {
            Condition c = a.when();
            if (c.type().equals("count") && counter.equals(c.counter()) && value >= c.atLeast()) {
                unlock(a);
            }
        }
    }

    private void unlock(Achievement a) {
        if (store.unlock(a.id(), clock.getAsLong())) {
            onUnlock.accept(a);
        }
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String x : a) {
            if (b.contains(x)) {
                return true;
            }
        }
        return false;
    }
}
