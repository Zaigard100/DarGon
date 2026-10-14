package com.zaig100.dargon.pack;

import com.zaig100.dargon.level.AssetReader;

/** Файлы одного пака относительно его корня: тексты (уровни, {@code pack.json}) и картинки. */
public interface PackFiles extends AssetReader {

    /** Содержимое файла или {@code null}, если его нет. */
    byte[] bytes(String path);

    /** Где лежит пак — для сообщений об ошибках. */
    String location();
}
