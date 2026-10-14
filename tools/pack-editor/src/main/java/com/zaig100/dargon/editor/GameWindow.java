package com.zaig100.dargon.editor;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.DarGonGame;
import com.zaig100.dargon.Platform;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Пробный запуск: игра открывается вторым окном внутри редактора — без новой Java, мгновенно. Пак
 * читается прямо из каталога проекта. Ошибка в игре закрывает только её окно, а не редактор.
 */
final class GameWindow {

    private static final int SCALE = 6;

    private Lwjgl3Window window;

    boolean isOpen() {
        return window != null;
    }

    /**
     * @param failed вызывается, если игра упала: окно закрыто, редактор продолжает работать
     */
    void open(Path pack, String level, Consumer<String> failed) {
        close();
        Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
        config.setTitle("DarGon — пробный запуск: " + level);
        config.setWindowedMode(DarGonGame.VIEW_WIDTH * SCALE, DarGonGame.VIEW_HEIGHT * SCALE);
        config.setResizable(true);
        config.setWindowIcon("icons/icon.png");
        config.setWindowListener(
                new Lwjgl3WindowAdapter() {
                    @Override
                    public boolean closeRequested() {
                        // Закрыли крестиком: окна больше нет — не трогать его потом.
                        window = null;
                        return true;
                    }
                });
        EditorPlatform platform = new EditorPlatform();
        Guard game =
                new Guard(
                        new DarGonGame(platform, level, pack.toAbsolutePath().toString()),
                        error -> {
                            failed.accept(error);
                            close();
                        });
        window = ((Lwjgl3Application) Gdx.app).newWindow(game, config);
        platform.window = window;
    }

    void close() {
        if (window != null) {
            window.closeWindow();
            window = null;
        }
    }

    /** Платформа игры в редакторе: «Выход» закрывает окно игры. */
    private final class EditorPlatform implements Platform {
        Lwjgl3Window window;

        @Override
        public String name() {
            return "editor";
        }

        @Override
        public boolean isTouchPrimary() {
            return false;
        }

        @Override
        public FileHandle dataDir() {
            return Gdx.files.external(".dargon");
        }

        @Override
        public void exit() {
            if (window != null) {
                window.closeWindow();
                if (GameWindow.this.window == window) {
                    GameWindow.this.window = null;
                }
            }
        }
    }

    /** Ловит исключения игры, чтобы они не роняли редактор. */
    private static final class Guard implements ApplicationListener {
        private final ApplicationListener game;
        private final Consumer<String> failed;
        private boolean broken;

        Guard(ApplicationListener game, Consumer<String> failed) {
            this.game = game;
            this.failed = failed;
        }

        private void run(Runnable step) {
            if (broken) {
                return;
            }
            try {
                step.run();
            } catch (RuntimeException e) {
                broken = true;
                Gdx.app.error("DarGon", "Пробный запуск упал", e);
                failed.accept(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        @Override
        public void create() {
            run(game::create);
        }

        @Override
        public void resize(int width, int height) {
            run(() -> game.resize(width, height));
        }

        @Override
        public void render() {
            run(game::render);
        }

        @Override
        public void pause() {
            run(game::pause);
        }

        @Override
        public void resume() {
            run(game::resume);
        }

        @Override
        public void dispose() {
            try {
                game.dispose();
            } catch (RuntimeException e) {
                Gdx.app.error("DarGon", "Ошибка при закрытии пробного запуска", e);
            }
        }
    }
}
