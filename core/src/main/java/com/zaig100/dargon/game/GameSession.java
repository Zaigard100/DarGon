package com.zaig100.dargon.game;

import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelDefinition;
import java.util.function.LongSupplier;

/**
 * Прохождение: текущий уровень и {@link Progress}. Каждый вход на уровень и каждая смерть
 * сохраняются через {@link Saver} (в хардкоре файл всё равно хранит только контрольную точку).
 */
public final class GameSession {

    /** Куда сохранять прогресс. */
    @FunctionalInterface
    public interface Saver {
        void save(Progress progress);

        Saver NONE = progress -> {};
    }

    private final GameData data;
    private final ActionState input;
    private final GameLog log;
    private final Saver saver;
    private final LongSupplier clock;
    private Progress progress;
    private LevelRuntime level;
    private final Cheats cheats = new Cheats();

    public GameSession(GameData data, ActionState input) {
        this(data, input, GameLog.NONE, Saver.NONE, System::currentTimeMillis);
    }

    public GameSession(
            GameData data, ActionState input, GameLog log, Saver saver, LongSupplier clock) {
        this.data = data;
        this.input = input;
        this.log = log;
        this.saver = saver;
        this.clock = clock;
    }

    /** Новая игра с первого уровня. */
    public LevelRuntime newGame() {
        return newGame(GameMode.NORMAL);
    }

    public LevelRuntime newGame(GameMode mode) {
        return newGame(mode, data.start(), PlayerState.newGame(data.items(), mode));
    }

    /**
     * Новая игра с уровня {@code start} (первый уровень пака или открытый отрезок) и стартовым
     * набором {@code player}.
     */
    public LevelRuntime newGame(GameMode mode, String start, PlayerState player) {
        progress = Progress.newGame(mode, data.pack().id(), start, player, clock.getAsLong());
        progress.run().fromStart = start.equals(data.start());
        return start(progress.entry().copy(), true);
    }

    /** Продолжить сохранённое прохождение. */
    public LevelRuntime resume(Progress saved) {
        progress = saved;
        return start(saved.resumeTarget(), false);
    }

    /** Начать с произвольного уровня (для разработки): как новая игра, но с другого уровня. */
    public LevelRuntime enter(String levelId, PlayerState player) {
        progress =
                Progress.newGame(
                        GameMode.NORMAL, data.pack().id(), levelId, player, clock.getAsLong());
        return start(progress.entry().copy(), true);
    }

    /** «Начать уровень заново» из паузы (в хардкоре — к контрольной точке). */
    public LevelRuntime restart() {
        return start(progress.restartTarget(), false);
    }

    /**
     * Игрок умер: решение (заново или откат) принимается и сохраняется сразу. Сам переход — {@link
     * #afterDeath()}, когда игрок нажмёт «Заново».
     */
    public Progress.Death died() {
        Progress.Death death = progress.died();
        saver.save(progress);
        return death;
    }

    /** Переход после смерти: текущий уровень заново или контрольная точка. */
    public LevelRuntime afterDeath() {
        return start(progress.entry().copy(), false);
    }

    /**
     * Переход по исходу уровня на следующий. Щит снимается, как в DarGon3.
     *
     * @return новый уровень или {@code null}, если исход — не выход на другой уровень
     */
    public LevelRuntime advance() {
        LevelOutcome outcome = level.outcome();
        if (outcome.kind() != LevelOutcome.Kind.EXIT) {
            return null;
        }
        return start(new Progress.Snapshot(outcome.next(), level.state().player()), true);
    }

    /** Время игры (идёт, только пока мир не на паузе). */
    public void addPlayTime(double seconds) {
        progress.addPlayTime(seconds);
    }

    /** Сохранить сейчас (выход в меню): обновляется время игры, уровень — по-прежнему вход. */
    public void save() {
        if (progress != null) {
            saver.save(progress);
        }
    }

    private LevelRuntime start(Progress.Snapshot target, boolean passed) {
        // Сначала прочитать новый уровень: при ошибке в файле текущий остаётся.
        LevelDefinition definition = data.loadLevel(target.level());
        if (level != null) {
            level.dispose();
        }
        input.clear();
        PlayerState player = target.player().copy();
        if (passed && progress.mode() == GameMode.CASUAL) {
            // Казуал: переход на уровень лечит до половины.
            player.hp = Math.max(player.hp, (int) (Balance.MAX_HP * Balance.CASUAL_HEAL));
        }
        progress.entered(target.level(), definition.checkpoint(), player, passed);
        saver.save(progress);
        String script =
                definition.scriptPath() == null ? null : data.readScript(definition.scriptPath());
        level =
                LevelRuntime.start(
                        definition, player, input, script, data::readScript, log, cheats);
        cheats.apply(level);
        return level;
    }

    /** Инструменты разработчика этой сессии. */
    public Cheats cheats() {
        return cheats;
    }

    /** Отметить, что в прохождении использованы инструменты разработчика (и сохранить). */
    public void markCheats() {
        if (!progress.cheats()) {
            progress.markCheats();
            saver.save(progress);
        }
    }

    public LevelRuntime level() {
        return level;
    }

    public Progress progress() {
        return progress;
    }

    public GameData data() {
        return data;
    }
}
