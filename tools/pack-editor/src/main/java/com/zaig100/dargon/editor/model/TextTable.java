package com.zaig100.dargon.editor.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Тексты пака на одном языке ({@code i18n/pack*.properties}): ключи в порядке файла, значения с
 * переводами строк. Пишется в том же виде: {@code ключ = значение}, {@code \n} для переносов.
 */
public final class TextTable {

    private static final Pattern KEY = Pattern.compile("^\\s*([^#!\\s=:][^\\s=:]*)\\s*[=:]");

    private final Map<String, String> entries = new LinkedHashMap<>();

    /** Комментарии и пустые строки перед ключом — остаются на своих местах. */
    private final Map<String, String> before = new LinkedHashMap<>();

    private String footer = "";

    public static TextTable parse(String source) {
        TextTable table = new TextTable();
        if (source == null) {
            return table;
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(source));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        StringBuilder pending = new StringBuilder();
        boolean continued = false;
        String[] lines = source.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.strip();
            boolean last = i == lines.length - 1;
            if (continued) {
                continued = trimmed.endsWith("\\") && !trimmed.endsWith("\\\\");
                continue;
            }
            Matcher m = KEY.matcher(line);
            if (m.find() && properties.containsKey(m.group(1))) {
                String key = m.group(1);
                if (!table.entries.containsKey(key)) {
                    table.entries.put(key, properties.getProperty(key));
                    table.before.put(key, pending.toString());
                }
                pending.setLength(0);
                continued = trimmed.endsWith("\\") && !trimmed.endsWith("\\\\");
            } else if (!(last && line.isEmpty())) {
                pending.append(line).append('\n');
            }
        }
        for (String key : properties.stringPropertyNames()) {
            table.entries.putIfAbsent(key, properties.getProperty(key));
        }
        table.footer = pending.toString();
        return table;
    }

    public String get(String key) {
        return entries.get(key);
    }

    public void put(String key, String value) {
        entries.put(key, value);
    }

    public void remove(String key) {
        entries.remove(key);
        before.remove(key);
    }

    public boolean contains(String key) {
        return entries.containsKey(key);
    }

    public Map<String, String> entries() {
        return entries;
    }

    public String write() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            sb.append(before.getOrDefault(e.getKey(), ""));
            sb.append(e.getKey()).append(" = ").append(escape(e.getValue())).append('\n');
        }
        return sb.append(footer).toString();
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> {}
                default -> sb.append(i == 0 && c == ' ' ? "\\ " : String.valueOf(c));
            }
        }
        return sb.toString();
    }
}
