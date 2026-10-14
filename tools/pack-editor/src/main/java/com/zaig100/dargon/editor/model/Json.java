package com.zaig100.dargon.editor.model;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON редактора — обычные {@link Map} (порядок ключей сохраняется), {@link List}, строки, {@link
 * Long}/{@link Double}, {@link Boolean}. Печать — {@link com.zaig100.dargon.level.JsonPrinter}.
 */
public final class Json {

    private Json() {}

    /** Разбирает текст в дерево карт и списков. */
    public static Object parse(String source) {
        return convert(new JsonReader().parse(source));
    }

    /** Разбирает объект JSON; не объект — исключение. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String source) {
        Object value = parse(source);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("ожидался объект JSON");
        }
        return (Map<String, Object>) value;
    }

    public static Object convert(JsonValue value) {
        if (value == null) {
            return null;
        }
        switch (value.type()) {
            case object:
                Map<String, Object> map = new LinkedHashMap<>();
                for (JsonValue child = value.child; child != null; child = child.next) {
                    map.put(child.name, convert(child));
                }
                return map;
            case array:
                List<Object> list = new ArrayList<>();
                for (JsonValue child = value.child; child != null; child = child.next) {
                    list.add(convert(child));
                }
                return list;
            case stringValue:
                return value.asString();
            case longValue:
                return value.asLong();
            case doubleValue:
                return value.asDouble();
            case booleanValue:
                return value.asBoolean();
            default:
                return null;
        }
    }

    /** Глубокая копия (для снимков отмены и вставки). */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                result.put((String) e.getKey(), copy(e.getValue()));
            }
            return (T) result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(copy(item));
            }
            return (T) result;
        }
        return value;
    }

    /** Целое из числа JSON ({@link Long} или {@link Double}); {@code fallback}, если не число. */
    public static int asInt(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }
}
