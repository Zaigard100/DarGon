package com.zaig100.dargon.editor;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import java.nio.file.Path;

/** Запуск редактора паков. Аргумент — каталог пака (иначе — последний открытый). */
public final class EditorLauncher {

    private EditorLauncher() {}

    public static void main(String[] args) {
        EditorApp app = new EditorApp(args.length > 0 ? Path.of(args[0]).toAbsolutePath() : null);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DarGon — редактор паков");
        config.setWindowedMode(1440, 900);
        config.setWindowSizeLimits(1100, 700, -1, -1);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(20);
        config.setWindowIcon("icons/icon.png");
        config.setWindowListener(
                new Lwjgl3WindowAdapter() {
                    @Override
                    public boolean closeRequested() {
                        return app.requestClose();
                    }
                });
        new Lwjgl3Application(app, config);
    }
}
