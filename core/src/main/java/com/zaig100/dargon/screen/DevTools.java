package com.zaig100.dargon.screen;

import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.Cheats;
import com.zaig100.dargon.game.LevelOutcome;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.component.LevelExit;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.level.LevelFormatException;
import java.util.Map;

/**
 * Действия режима разработчика над идущей игрой. Каждое действие, меняющее правила или состояние,
 * помечает прохождение: достижения выключаются, статистика не пишется.
 */
final class DevTools {

    private final App app;

    DevTools(App app) {
        this.app = app;
    }

    private Gameplay game() {
        return app.gameplay();
    }

    Cheats cheats() {
        return game().session().cheats();
    }

    /** Переключить инструмент и применить к текущему уровню. */
    void toggle(Runnable change) {
        change.run();
        if (cheats().anyActive()) {
            game().session().markCheats();
        }
        cheats().apply(game().level());
    }

    void nextTimeScale() {
        toggle(() -> cheats().timeScale = cheats().timeScale >= 4 ? 1 : cheats().timeScale * 2);
    }

    void giveCoins() {
        game().session().markCheats();
        game().level().state().player().coins += 100;
    }

    void giveItems() {
        game().session().markCheats();
        var inventory = game().level().state().player().inventory;
        inventory.add(ItemStack.of("potion", 5));
        inventory.add(ItemStack.of("shield", 3));
        inventory.add(ItemStack.of("torch", 2));
        inventory.add(ItemStack.of("trap_sight", 2));
        inventory.add(new ItemStack("speed_potion", 1, Map.of("power", 2.0)));
    }

    /** Пройти уровень: выход по первой лестнице уровня (или конец игры). */
    void completeLevel() {
        game().session().markCheats();
        LevelRuntime level = game().level();
        EntitySet exits = level.world().entities(Family.all(LevelExit.class));
        String next = null;
        for (int i = 0; i < exits.size() && next == null; i++) {
            next = level.world().get(exits.get(i), LevelExit.class).next;
        }
        level.state().finish(LevelOutcome.exit(next));
        app.resume();
    }

    /** Перечитать уровень и скрипт с диска и начать заново. Ошибка формата — в журнал. */
    void reloadLevel() {
        try {
            app.restartLevel();
        } catch (LevelFormatException e) {
            app.log().error(e.getMessage());
            app.screens().push(new DevLogScreen());
        }
    }
}
