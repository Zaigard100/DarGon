package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;

/**
 * Данные одного компонента сущности (после подстановки параметров) с типизированным доступом.
 * Ошибки — {@link LevelFormatException} с путём {@code entities[3].components.Emitter.period}.
 */
public final class ComponentData {

    private final JsonValue data;
    private final String file;
    private final String path;

    public ComponentData(JsonValue data, String file, String path) {
        this.data = data;
        this.file = file;
        this.path = path;
    }

    public JsonValue json() {
        return data;
    }

    public String path() {
        return path;
    }

    public boolean has(String key) {
        JsonValue value = JsonValues.get(data, key);
        return value != null && !value.isNull();
    }

    /** Поле как есть; {@code null}, если его нет. */
    public JsonValue get(String key) {
        return JsonValues.get(data, key);
    }

    public int getInt(String key) {
        JsonValue value = require(key);
        if (!JsonValues.isInteger(value)) {
            throw error(key, "ожидалось целое число, получено " + JsonValues.describe(value));
        }
        return value.asInt();
    }

    public int getInt(String key, int fallback) {
        return has(key) ? getInt(key) : fallback;
    }

    public float getFloat(String key) {
        JsonValue value = require(key);
        if (!value.isNumber()) {
            throw error(key, "ожидалось число, получено " + JsonValues.describe(value));
        }
        return value.asFloat();
    }

    public float getFloat(String key, float fallback) {
        return has(key) ? getFloat(key) : fallback;
    }

    public boolean getBool(String key, boolean fallback) {
        if (!has(key)) {
            return fallback;
        }
        JsonValue value = get(key);
        if (!value.isBoolean()) {
            throw error(key, "ожидалось true или false, получено " + JsonValues.describe(value));
        }
        return value.asBoolean();
    }

    public String getString(String key) {
        JsonValue value = require(key);
        if (!value.isString()) {
            throw error(key, "ожидалась строка, получено " + JsonValues.describe(value));
        }
        return value.asString();
    }

    public String getString(String key, String fallback) {
        return has(key) ? getString(key) : fallback;
    }

    public Direction getDirection(String key) {
        Direction direction = Direction.byId(getString(key));
        if (direction == null) {
            throw error(key, "неизвестное направление '" + getString(key) + "'");
        }
        return direction;
    }

    public GridPoint getPoint(String key) {
        JsonValue value = require(key);
        if (!value.isArray()
                || value.size != 2
                || !JsonValues.isInteger(value.child)
                || !JsonValues.isInteger(value.child.next)) {
            throw error(key, "ожидалась клетка [x, y], получено " + JsonValues.describe(value));
        }
        return new GridPoint(value.child.asInt(), value.child.next.asInt());
    }

    private JsonValue require(String key) {
        JsonValue value = JsonValues.get(data, key);
        if (value == null || value.isNull()) {
            throw error(key, "нет обязательного поля");
        }
        return value;
    }

    public LevelFormatException error(String key, String message) {
        return new LevelFormatException(file, JsonValues.child(path, key), message);
    }
}
