package com.zaig100.dargon.game.item;

import com.badlogic.gdx.utils.JsonValue;
import com.zaig100.dargon.level.JsonValues;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Стопка предметов: тип, количество и дополнительные поля (ключ двери, цвет, сила зелья). Значение
 * неизменяемо: изменения возвращают новую стопку.
 */
public final class ItemStack {

    /** Монеты: при подборе сразу попадают в кошелёк. */
    public static final String COINS = "coins";

    private final String id;
    private final int count;
    private final Map<String, Object> fields;

    public ItemStack(String id, int count, Map<String, Object> fields) {
        if (count <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным: " + count);
        }
        this.id = Objects.requireNonNull(id, "id");
        this.count = count;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static ItemStack of(String id, int count) {
        return new ItemStack(id, count, Map.of());
    }

    /**
     * Стопка из JSON уровня: {@code { "id": "key", "key": "red" }}; {@code count} по умолчанию 1.
     */
    public static ItemStack fromJson(JsonValue json) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (JsonValue field = json.child; field != null; field = field.next) {
            if (field.name.equals("id") || field.name.equals("count")) {
                continue;
            }
            fields.put(field.name, scalar(field));
        }
        JsonValue count = JsonValues.get(json, "count");
        return new ItemStack(
                JsonValues.getString(json, "id", null), count == null ? 1 : count.asInt(), fields);
    }

    private static Object scalar(JsonValue value) {
        switch (value.type()) {
            case doubleValue:
                return value.asDouble();
            case longValue:
                return value.asLong();
            case booleanValue:
                return value.asBoolean();
            case nullValue:
                return null;
            default:
                return value.asString();
        }
    }

    public String id() {
        return id;
    }

    public int count() {
        return count;
    }

    public Map<String, Object> fields() {
        return fields;
    }

    /** Поле стопки или {@code null}. */
    public Object field(String name) {
        return fields.get(name);
    }

    public ItemStack withCount(int newCount) {
        return new ItemStack(id, newCount, fields);
    }

    /** Складываются ли стопки в одну ячейку: тот же тип и те же поля. */
    public boolean stacksWith(ItemStack other) {
        return id.equals(other.id) && fields.equals(other.fields);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ItemStack s
                && id.equals(s.id)
                && count == s.count
                && fields.equals(s.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, count, fields);
    }

    @Override
    public String toString() {
        return id + "×" + count + (fields.isEmpty() ? "" : fields.toString());
    }
}
