package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.SerializationException;
import java.util.Locale;
import java.util.Set;

/** Вспомогательные функции для {@link JsonValue}: разбор, пути, копирование, описание типов. */
public final class JsonValues {

    private JsonValues() {}

    /**
     * Разбирает JSON. libGDX допускает комментарии {@code //} и {@code /* *\/}.
     *
     * @throws LevelFormatException при синтаксической ошибке
     */
    public static JsonValue parse(String source, String file) {
        try {
            JsonValue root = new JsonReader().parse(source);
            if (root == null) {
                throw new LevelFormatException(file, "", "пустой файл");
            }
            return root;
        } catch (SerializationException e) {
            throw new LevelFormatException(file, "", "некорректный JSON: " + e.getMessage());
        }
    }

    /**
     * Поле объекта по точному имени (в отличие от {@link JsonValue#get(String)}, учитывает
     * регистр).
     */
    public static JsonValue get(JsonValue object, String key) {
        for (JsonValue child = object.child; child != null; child = child.next) {
            if (key.equals(child.name)) {
                return child;
            }
        }
        return null;
    }

    /** Строковое поле по точному имени; {@code fallback}, если поля нет или оно не строка. */
    public static String getString(JsonValue object, String key, String fallback) {
        JsonValue value = get(object, key);
        return value != null && value.isString() ? value.asString() : fallback;
    }

    /** Путь к полю объекта: {@code entities[3]} + {@code period} → {@code entities[3].period}. */
    public static String child(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    /** Путь к элементу массива: {@code entities} + 3 → {@code entities[3]}. */
    public static String index(String path, int index) {
        return path + "[" + index + "]";
    }

    /** Название типа значения для сообщений: «число», «строка»… */
    public static String describe(JsonValue value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isString()) {
            return "строка \"" + value.asString() + "\"";
        }
        if (value.isNumber()) {
            return "число " + value.toJson(JsonWriter.OutputType.json);
        }
        if (value.isBoolean()) {
            return "логическое значение " + value.asBoolean();
        }
        if (value.isArray()) {
            return "массив";
        }
        return "объект";
    }

    /** Целое число без дробной части (в JSON может быть записано как {@code 3} или {@code 3.0}). */
    public static boolean isInteger(JsonValue value) {
        if (value == null || !value.isNumber()) {
            return false;
        }
        if (value.isLong()) {
            return true;
        }
        double d = value.asDouble();
        return d == Math.rint(d) && !Double.isInfinite(d);
    }

    /** Глубокая копия значения (имя корня не копируется). */
    public static JsonValue copy(JsonValue value) {
        JsonValue copy;
        switch (value.type()) {
            case object:
            case array:
                copy = new JsonValue(value.type());
                for (JsonValue child = value.child; child != null; child = child.next) {
                    append(copy, child.name, copy(child));
                }
                return copy;
            case stringValue:
                return new JsonValue(value.asString());
            case doubleValue:
                return new JsonValue(value.asDouble(), value.toString());
            case longValue:
                return new JsonValue(value.asLong(), value.toString());
            case booleanValue:
                return new JsonValue(value.asBoolean());
            default:
                return new JsonValue(JsonValue.ValueType.nullValue);
        }
    }

    /** Добавляет элемент в массив ({@code name == null}) или поле в объект. */
    public static void append(JsonValue parent, String name, JsonValue value) {
        if (name == null) {
            parent.addChild(value);
        } else {
            parent.addChild(name, value);
        }
    }

    /** Сообщает об этих полях объекта, которых нет среди разрешённых. */
    public static void checkKeys(
            JsonValue object, String path, Set<String> allowed, Problems problems) {
        for (JsonValue child = object.child; child != null; child = child.next) {
            if (!allowed.contains(child.name)) {
                problems.error(
                        child(path, child.name), "неизвестное поле" + suggest(child.name, allowed));
            }
        }
    }

    /** Подсказка «, возможно, имелось в виду 'x'» для опечаток. */
    public static String suggest(String name, Iterable<String> candidates) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int distance =
                    distance(name.toLowerCase(Locale.ROOT), candidate.toLowerCase(Locale.ROOT));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best == null || bestDistance > Math.max(2, name.length() / 3)) {
            return "";
        }
        return ", возможно, имелось в виду '" + best + "'";
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] =
                        Math.min(
                                Math.min(current[j - 1] + 1, previous[j] + 1),
                                previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
