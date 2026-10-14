package com.zaig100.dargon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.function.Consumer;

/**
 * Сервисы, зависящие от платформы. Реализуются лаунчерами (desktop, android).
 *
 * <p>Расширяется по мере надобности: директории хранения, открытие ссылок, вибрация и т.п.
 */
public interface Platform {

    /** Имя платформы для логов и диагностики. */
    String name();

    /** {@code true}, если основной способ ввода — сенсорный экран. */
    boolean isTouchPrimary();

    /** Каталог для сохранений и статистики. */
    FileHandle dataDir();

    /** Выйти из игры. В редакторе паков игра — окно внутри редактора: закрывается только оно. */
    default void exit() {
        Gdx.app.exit();
    }

    /** Короткая вибрация (урон); по умолчанию — ничего. */
    default void vibrate(int milliseconds) {}

    /** Папка установленных паков (каталоги и архивы {@code .dgpack}). */
    default FileHandle packsDir() {
        return dataDir().child("packs");
    }

    /** Есть ли системный выбор файла для добавления пака (Android). */
    default boolean canImportPacks() {
        return false;
    }

    /**
     * Выбрать файл пака системным диалогом и скопировать его в {@link #packsDir()}. {@code done}
     * вызывается в потоке игры: с {@code null} при успехе, с текстом ошибки, или не вызывается,
     * если выбор отменён.
     */
    default void importPack(Consumer<String> done) {
        done.accept("не поддерживается на этой платформе");
    }
}
