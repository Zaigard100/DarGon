package com.zaig100.dargon.script;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Изменяемый словарь со строковыми ключами (порядок вставки сохраняется). Сравнивается по ссылке.
 */
public final class MapValue implements Value {

    private final Map<String, Value> entries = new LinkedHashMap<>();

    /** Живой словарь (изменения видны скрипту). */
    public Map<String, Value> entries() {
        return entries;
    }

    /** Значение по ключу или {@code null}-значение, если ключа нет. */
    public Value get(String key) {
        Value value = entries.get(key);
        return value == null ? NullValue.INSTANCE : value;
    }

    public void put(String key, Value value) {
        entries.put(key, value);
    }

    @Override
    public String typeName() {
        return "map";
    }

    @Override
    public String display() {
        return Values.displayNested(this, 0);
    }

    @Override
    public String toString() {
        return display();
    }
}
