package com.zaig100.dargon.tools.migrator;

import java.util.Map;
import java.util.TreeMap;

/**
 * Собирает тексты уровней в файлы локализации пака {@code pack.properties} (en) и {@code pack_ru}.
 */
final class TextBundle {

    final Map<String, String> english = new TreeMap<>();
    final Map<String, String> russian = new TreeMap<>();

    /** Регистрирует текст и возвращает ссылку {@code "@key"}. */
    String add(String key, String englishText) {
        english.put(key, englishText);
        russian.put(key, Texts.russian(englishText));
        return "@" + key;
    }

    static String toProperties(Map<String, String> texts, String comment) {
        StringBuilder sb = new StringBuilder("# ").append(comment).append('\n');
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(escape(entry.getValue())).append('\n');
        }
        return sb.toString();
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == ' ' && sb.length() == 0) {
                sb.append("\\ ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
