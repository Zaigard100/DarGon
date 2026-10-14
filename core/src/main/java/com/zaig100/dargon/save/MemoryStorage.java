package com.zaig100.dargon.save;

import java.util.HashMap;
import java.util.Map;

/** Хранилище в памяти (тесты, режим без диска). */
public final class MemoryStorage implements Storage {

    private final Map<String, String> files = new HashMap<>();

    @Override
    public String read(String name) {
        return files.get(name);
    }

    @Override
    public void write(String name, String content) {
        String old = files.get(name);
        if (old != null) {
            files.put(name + ".bak", old);
        }
        files.put(name, content);
    }

    @Override
    public void delete(String name) {
        files.remove(name);
        files.remove(name + ".bak");
    }

    /** Все файлы (для тестов: подменить или испортить). */
    public Map<String, String> files() {
        return files;
    }
}
