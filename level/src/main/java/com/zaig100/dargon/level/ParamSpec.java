package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Описание параметра префаба (или дополнительного поля предмета).
 *
 * <pre>{@code
 * "period": { "type": "number", "default": 2.0, "min": 0.1, "description": "..." }
 * "target": { "type": "point" }                     // без default — обязательный
 * "next":   { "type": "level", "default": null }    // null — необязательный, по умолчанию нет
 * }</pre>
 */
public final class ParamSpec {

    private static final Set<String> KEYS =
            Set.of("type", "default", "min", "max", "values", "description");

    private final String name;
    private final ParamType type;
    private final JsonValue defaultValue;
    private final Double min;
    private final Double max;
    private final List<String> values;
    private final String description;

    ParamSpec(
            String name,
            ParamType type,
            JsonValue defaultValue,
            Double min,
            Double max,
            List<String> values,
            String description) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.values = values;
        this.description = description;
    }

    /** Разбирает описание; при ошибках возвращает {@code null} и пишет их в {@code problems}. */
    static ParamSpec parse(
            String name, JsonValue spec, String path, Problems problems, ItemCatalog items) {
        if (!spec.isObject()) {
            problems.error(
                    path,
                    "ожидалось описание параметра {\"type\": ...}, получено "
                            + JsonValues.describe(spec));
            return null;
        }
        int errors = problems.errorCount();
        JsonValues.checkKeys(spec, path, KEYS, problems);
        String typeName = JsonValues.getString(spec, "type", null);
        ParamType type = typeName == null ? null : ParamType.byId(typeName);
        if (type == null) {
            List<String> names = new ArrayList<>();
            for (ParamType t : ParamType.values()) {
                names.add(t.id());
            }
            problems.error(
                    JsonValues.child(path, "type"),
                    "неизвестный тип параметра "
                            + (typeName == null ? "(нет поля)" : "'" + typeName + "'")
                            + "; допустимы: "
                            + String.join(", ", names));
            return null;
        }
        Double min = number(spec, "min", path, problems);
        Double max = number(spec, "max", path, problems);
        List<String> values = null;
        JsonValue valuesJson = JsonValues.get(spec, "values");
        if (valuesJson != null) {
            values = new ArrayList<>();
            if (type != ParamType.STRING || !valuesJson.isArray()) {
                problems.error(
                        JsonValues.child(path, "values"),
                        "список допустимых значений задаётся массивом строк и только для type: string");
            } else {
                for (JsonValue v = valuesJson.child; v != null; v = v.next) {
                    values.add(v.asString());
                }
            }
        }
        String description = JsonValues.getString(spec, "description", "");
        ParamSpec result =
                new ParamSpec(
                        name, type, JsonValues.get(spec, "default"), min, max, values, description);
        if (result.defaultValue != null && !result.defaultValue.isNull()) {
            result.validate(
                    result.defaultValue, JsonValues.child(path, "default"), problems, items);
        }
        return problems.errorCount() == errors ? result : null;
    }

    private static Double number(JsonValue spec, String key, String path, Problems problems) {
        JsonValue value = spec.get(key);
        if (value == null) {
            return null;
        }
        if (!value.isNumber()) {
            problems.error(JsonValues.child(path, key), "ожидалось число");
            return null;
        }
        return value.asDouble();
    }

    /** Проверяет значение параметра в уровне. {@code null} допустим, только если default — null. */
    void validate(JsonValue value, String path, Problems problems, ItemCatalog items) {
        if (value.isNull()) {
            if (!isOptional()) {
                problems.error(path, "значение не может быть null");
            }
            return;
        }
        int errors = problems.errorCount();
        type.validate(value, path, problems, items);
        if (problems.errorCount() != errors) {
            return;
        }
        if (value.isNumber()) {
            double d = value.asDouble();
            if (min != null && d < min) {
                problems.error(path, "значение " + format(d) + " меньше минимума " + format(min));
            }
            if (max != null && d > max) {
                problems.error(path, "значение " + format(d) + " больше максимума " + format(max));
            }
        }
        if (values != null && value.isString() && !values.contains(value.asString())) {
            problems.error(
                    path,
                    "недопустимое значение \""
                            + value.asString()
                            + "\"; допустимы: "
                            + String.join(", ", values)
                            + JsonValues.suggest(value.asString(), values));
        }
    }

    private static String format(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }

    public String name() {
        return name;
    }

    public ParamType type() {
        return type;
    }

    /** Обязательный: в описании нет {@code default}. */
    public boolean isRequired() {
        return defaultValue == null;
    }

    /** Необязательный без значения по умолчанию: {@code "default": null}. */
    public boolean isOptional() {
        return defaultValue != null && defaultValue.isNull();
    }

    /** Значение по умолчанию; {@code null}, если параметр обязательный. */
    public JsonValue defaultValue() {
        return defaultValue;
    }

    public Double min() {
        return min;
    }

    public Double max() {
        return max;
    }

    /** Допустимые значения строки; {@code null} — любые. */
    public List<String> values() {
        return values;
    }

    public String description() {
        return description;
    }
}
