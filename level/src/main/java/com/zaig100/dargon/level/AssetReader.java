package com.zaig100.dargon.level;

/** Источник файлов данных: в игре — {@code Gdx.files.internal}, в утилитах — файловая система. */
@FunctionalInterface
public interface AssetReader {

    /**
     * Читает текстовый файл (UTF-8).
     *
     * @param path путь относительно каталога {@code assets}, через {@code /}
     * @return содержимое или {@code null}, если файла нет
     */
    String read(String path);

    /** Файлы внутри каталога {@code dir} (с {@code /} на конце): пак в общем каталоге ассетов. */
    default AssetReader under(String dir) {
        return path -> read(dir + path);
    }
}
