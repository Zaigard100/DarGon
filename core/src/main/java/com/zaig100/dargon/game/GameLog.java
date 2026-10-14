package com.zaig100.dargon.game;

/** Журнал игровой логики (ошибки скриптов, вывод {@code print}). */
public interface GameLog {

    /** Журнал, который ничего не пишет. */
    GameLog NONE =
            new GameLog() {
                @Override
                public void info(String message) {}

                @Override
                public void error(String message) {}
            };

    void info(String message);

    void error(String message);
}
