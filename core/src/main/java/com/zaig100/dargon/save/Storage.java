package com.zaig100.dargon.save;

/**
 * Хранилище текстовых файлов игры (сохранения, статистика). Реализации: файлы ({@link
 * FileStorage}), память (тесты).
 */
public interface Storage {

    /** Содержимое файла или {@code null}, если его нет. */
    String read(String name);

    /**
     * Записывает файл атомарно: сначала во временный, затем переименование. Прежняя версия остаётся
     * в {@code name + ".bak"}.
     */
    void write(String name, String content);

    void delete(String name);
}
