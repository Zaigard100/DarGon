package com.zaig100.dargon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.zaig100.dargon.DarGonGame;
import com.zaig100.dargon.Platform;

/**
 * Запуск игры на Windows / Linux / macOS. Рабочая директория — папка {@code assets}.
 *
 * <p>Аргумент {@code --level=05} запускает игру сразу с указанного уровня, {@code --pack=<каталог>}
 * — с пака из каталога (пробный запуск из редактора паков).
 */
public final class DesktopLauncher {

    private static final int DEFAULT_SCALE = 6;
    private static final int MIN_REFRESH_RATE = 30;
    private static final int FALLBACK_FPS = 60;

    /** {@code --level=05}: начать с уровня (для разработки). */
    private static final String LEVEL_ARG = "--level=";

    /** {@code --pack=<каталог>}: пак из каталога, без установки (редактор паков). */
    private static final String PACK_ARG = "--pack=";

    private DesktopLauncher() {}

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DarGon");
        config.setWindowedMode(
                DarGonGame.VIEW_WIDTH * DEFAULT_SCALE, DarGonGame.VIEW_HEIGHT * DEFAULT_SCALE);
        config.setWindowSizeLimits(DarGonGame.VIEW_WIDTH * 2, DarGonGame.VIEW_HEIGHT * 2, -1, -1);
        config.setResizable(true);
        config.useVsync(true);
        int refreshRate = Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate;
        // Виртуальные и некоторые внешние дисплеи сообщают 0–1 Гц: тогда ограничиваем 60 кадрами.
        config.setForegroundFPS(refreshRate >= MIN_REFRESH_RATE ? refreshRate + 1 : FALLBACK_FPS);
        config.setWindowIcon("icons/icon.png");
        String level = null;
        String pack = null;
        for (String arg : args) {
            if (arg.startsWith(LEVEL_ARG)) {
                level = arg.substring(LEVEL_ARG.length());
            } else if (arg.startsWith(PACK_ARG)) {
                pack = arg.substring(PACK_ARG.length());
            }
        }
        if (pack != null) {
            config.setTitle("DarGon — пробный запуск");
        }
        new Lwjgl3Application(new DarGonGame(new DesktopPlatform(), level, pack), config);
    }

    private static final class DesktopPlatform implements Platform {
        @Override
        public String name() {
            return "desktop";
        }

        @Override
        public boolean isTouchPrimary() {
            return false;
        }

        /** Сохранения — в домашнем каталоге пользователя ({@code ~/.dargon}). */
        @Override
        public FileHandle dataDir() {
            return Gdx.files.external(".dargon");
        }
    }
}
