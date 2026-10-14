package com.zaig100.dargon.script;

/** Место в исходном тексте скрипта: файл, строка и колонка (с единицы). */
public record SourcePosition(String file, int line, int column) {

    @Override
    public String toString() {
        return file + ":" + line + ":" + column;
    }
}
