package com.zaig100.dargon.level;

import java.util.List;
import java.util.Map;

/**
 * Печать JSON для людей: короткие объекты и массивы — в одну строку, длинные — по элементу на
 * строку. Значения — {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean},
 * {@code null}.
 */
public final class JsonPrinter {

    private static final int LINE_LIMIT = 120;
    private static final String INDENT = "  ";

    private JsonPrinter() {}

    /** Массив, который всегда печатается по элементу на строку (строки карты). */
    public record Multiline(List<?> items) {}

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb, "", 0);
        return sb.append('\n').toString();
    }

    private static void write(Object value, StringBuilder sb, String indent, int column) {
        String inline = inline(value);
        if (inline != null && column + inline.length() <= LINE_LIMIT) {
            sb.append(inline);
            return;
        }
        String inner = indent + INDENT;
        if (value instanceof Map<?, ?> map) {
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(inner);
                String key = quote(entry.getKey().toString()) + ": ";
                sb.append(key);
                write(entry.getValue(), sb, inner, inner.length() + key.length());
                sb.append(++i < map.size() ? ",\n" : "\n");
            }
            sb.append(indent).append('}');
        } else if (value instanceof List<?> || value instanceof Multiline) {
            List<?> list = value instanceof Multiline m ? m.items() : (List<?>) value;
            sb.append("[\n");
            int i = 0;
            for (Object item : list) {
                sb.append(inner);
                write(item, sb, inner, inner.length());
                sb.append(++i < list.size() ? ",\n" : "\n");
            }
            sb.append(indent).append(']');
        } else {
            sb.append(inline);
        }
    }

    /** Значение в одну строку; {@code null}, если внутри есть многострочные строки карты. */
    private static String inline(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Multiline) {
            return null;
        }
        if (value instanceof String s) {
            return quote(s);
        }
        if (value instanceof Double d && d == Math.rint(d) && !d.isInfinite()) {
            return d.longValue() + ".0";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "{}";
            }
            sb.append("{ ");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String item = inline(entry.getValue());
                if (item == null) {
                    return null;
                }
                sb.append(quote(entry.getKey().toString())).append(": ").append(item);
                sb.append(++i < map.size() ? ", " : " }");
            }
            return sb.toString();
        }
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                return "[]";
            }
            sb.append('[');
            int i = 0;
            for (Object item : list) {
                String text = inline(item);
                if (text == null) {
                    return null;
                }
                sb.append(text).append(++i < list.size() ? ", " : "]");
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("Не JSON-значение: " + value.getClass());
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
