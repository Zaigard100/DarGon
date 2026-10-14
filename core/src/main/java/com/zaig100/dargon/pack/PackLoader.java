package com.zaig100.dargon.pack;

import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.PlayerState;
import com.zaig100.dargon.input.ActionState;
import com.zaig100.dargon.level.AssetReader;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.PackDefinition;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Загружает пак для игры: проверяет совместимость с версией игры, разбирает {@code pack.json} и все
 * уровни. Ошибки собираются заранее — сломанный пак не выбирается, вместо падения посреди игры.
 */
public final class PackLoader {

    /** Пак не загрузился: несовместим с версией игры или сломан (текст — что именно). */
    public static final class PackException extends Exception {
        private final PackDefinition.Incompatibility incompatibility;

        public PackException(String message, PackDefinition.Incompatibility incompatibility) {
            super(message);
            this.incompatibility = incompatibility;
        }

        /** Несовместимость с версией игры или {@code null}, если пак сломан. */
        public PackDefinition.Incompatibility incompatibility() {
            return incompatibility;
        }
    }

    private PackLoader() {}

    /**
     * @param engine файлы игры (предметы, префабы)
     * @param entry пак из {@link PackRepository}
     * @param gameVersion версия игры для проверки совместимости
     */
    public static GameData load(AssetReader engine, PackRepository.Entry entry, String gameVersion)
            throws PackException {
        if (!entry.readable()) {
            throw new PackException(entry.error(), null);
        }
        PackDefinition.Incompatibility problem = entry.info().check(gameVersion);
        if (problem != null) {
            throw new PackException(problem.name(), problem);
        }
        GameData data;
        try {
            data = GameData.load(engine, entry.files());
            for (String level : data.levels()) {
                data.loadLevel(level);
            }
        } catch (LevelFormatException | IllegalStateException e) {
            throw new PackException(e.getMessage(), null);
        }
        for (String name : data.pack().textures()) {
            byte[] png = entry.files().bytes(texturePath(name));
            if (png == null) {
                throw new PackException("нет файла " + texturePath(name), null);
            }
            if (!isPng(png)) {
                throw new PackException(texturePath(name) + " — не PNG", null);
            }
        }
        // Пробный запуск каждого уровня: ошибки в данных компонентов своих префабов видны сразу.
        for (String level : data.levels()) {
            try {
                LevelRuntime.start(
                                data.loadLevel(level),
                                PlayerState.newGame(data.items()),
                                new ActionState())
                        .dispose();
            } catch (RuntimeException e) {
                throw new PackException("уровень " + level + ": " + e.getMessage(), null);
            }
        }
        return data;
    }

    /** Текстуры пака для {@code GameAssets.usePackTextures}: имя → PNG. */
    public static Map<String, byte[]> textures(PackFiles files, GameData data) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String name : data.pack().textures()) {
            byte[] png = files.bytes(texturePath(name));
            if (png != null) {
                result.put(name, png);
            }
        }
        return result;
    }

    public static String texturePath(String name) {
        return GameData.PACK_TEXTURES_DIR + name + ".png";
    }

    private static boolean isPng(byte[] data) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
