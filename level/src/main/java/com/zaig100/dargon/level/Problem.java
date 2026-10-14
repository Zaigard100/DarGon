package com.zaig100.dargon.level;

/**
 * Одна проблема в данных уровня: ошибка или предупреждение с местом.
 *
 * @param file файл (идентификатор уровня, префаба и т.п.)
 * @param path путь внутри JSON: {@code entities[3].period}; пустой — файл целиком
 */
public record Problem(Severity severity, String file, String path, String message) {

    /** Насколько серьёзна проблема. */
    public enum Severity {
        /** Уровень нельзя загрузить. */
        ERROR,
        /** Уровень загрузится, но, скорее всего, автор ошибся. */
        WARNING
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        String where = path.isEmpty() ? file : file + ": " + path;
        String prefix = severity == Severity.ERROR ? "ошибка" : "предупреждение";
        return where + ": " + prefix + ": " + message;
    }
}
