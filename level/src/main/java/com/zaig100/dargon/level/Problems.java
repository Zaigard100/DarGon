package com.zaig100.dargon.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Накопитель проблем: разбор не останавливается на первой ошибке, а собирает все. */
public final class Problems {

    private final List<Problem> list = new ArrayList<>();
    private final String file;

    public Problems(String file) {
        this.file = file;
    }

    public String file() {
        return file;
    }

    public void error(String path, String message) {
        list.add(new Problem(Problem.Severity.ERROR, file, path, message));
    }

    public void warning(String path, String message) {
        list.add(new Problem(Problem.Severity.WARNING, file, path, message));
    }

    public void addAll(Problems other) {
        list.addAll(other.list);
    }

    public boolean hasErrors() {
        for (Problem problem : list) {
            if (problem.isError()) {
                return true;
            }
        }
        return false;
    }

    public int errorCount() {
        int count = 0;
        for (Problem problem : list) {
            if (problem.isError()) {
                count++;
            }
        }
        return count;
    }

    public List<Problem> list() {
        return Collections.unmodifiableList(list);
    }

    /** Бросает {@link LevelFormatException}, если есть хотя бы одна ошибка. */
    public void throwIfErrors() {
        if (hasErrors()) {
            throw new LevelFormatException(list);
        }
    }
}
