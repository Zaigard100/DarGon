package com.zaig100.dargon.game;

import java.util.Locale;

/** Режим прохождения. */
public enum GameMode {
    /** Смерть — всегда уровень заново; переход на уровень лечит до половины; без достижений. */
    CASUAL,
    /** Сохранение при входе на каждый уровень; 3 смерти подряд — откат к контрольной точке. */
    NORMAL,
    /** Сохранение только на контрольных точках; любая смерть — откат к контрольной точке. */
    HARDCORE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static GameMode byId(String id) {
        for (GameMode mode : values()) {
            if (mode.id().equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
