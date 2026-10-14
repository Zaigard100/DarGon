package com.zaig100.dargon.tools.migrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Отчёт миграции: по каждому уровню — что изменилось и что стоит проверить руками. */
final class Report {

    private final Map<String, List<String>> notes = new LinkedHashMap<>();

    void note(String level, String message) {
        notes.computeIfAbsent(level, k -> new ArrayList<>()).add(message);
    }

    String toMarkdown() {
        StringBuilder sb = new StringBuilder("# Отчёт миграции уровней DarGon3\n");
        for (Map.Entry<String, List<String>> entry : notes.entrySet()) {
            sb.append("\n## ").append(entry.getKey()).append("\n\n");
            for (String note : entry.getValue()) {
                sb.append("- ").append(note).append('\n');
            }
        }
        return sb.toString();
    }
}
