package com.zaig100.dargon.level;

import java.util.List;

/**
 * Данные уровня (или префаба, или каталога предметов) некорректны. Содержит все найденные ошибки.
 */
public final class LevelFormatException extends RuntimeException {

    private final transient List<Problem> problems;

    public LevelFormatException(List<Problem> problems) {
        super(describe(problems));
        this.problems = List.copyOf(problems);
    }

    public LevelFormatException(String file, String path, String message) {
        this(List.of(new Problem(Problem.Severity.ERROR, file, path, message)));
    }

    public List<Problem> problems() {
        return problems;
    }

    private static String describe(List<Problem> problems) {
        StringBuilder sb = new StringBuilder();
        for (Problem problem : problems) {
            if (problem.isError()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(problem);
            }
        }
        return sb.toString();
    }
}
