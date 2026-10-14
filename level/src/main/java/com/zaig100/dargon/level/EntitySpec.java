package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сущность уровня после разбора: префаб, место, значения всех параметров (с подставленными
 * значениями по умолчанию) и обработчики событий.
 */
public final class EntitySpec {

    private final int index;
    private final Prefab prefab;
    private final String id;
    private final List<String> tags;
    private final GridPoint at;
    private final boolean enabled;
    private final Map<String, JsonValue> params;
    private final Map<String, String> handlers;

    EntitySpec(
            int index,
            Prefab prefab,
            String id,
            List<String> tags,
            GridPoint at,
            boolean enabled,
            Map<String, JsonValue> params,
            Map<String, String> handlers) {
        this.index = index;
        this.prefab = prefab;
        this.id = id;
        this.tags = tags;
        this.at = at;
        this.enabled = enabled;
        this.params = params;
        this.handlers = handlers;
    }

    /** Позиция в массиве {@code entities} — для сообщений об ошибках. */
    public int index() {
        return index;
    }

    /** Путь в JSON уровня: {@code entities[3]}. */
    public String path() {
        return JsonValues.index("entities", index);
    }

    public Prefab prefab() {
        return prefab;
    }

    /** Уникальный идентификатор для скриптов; {@code null}, если не задан. */
    public String id() {
        return id;
    }

    public List<String> tags() {
        return tags;
    }

    public GridPoint at() {
        return at;
    }

    /** {@code false} — сущность создаётся выключенной и ждёт, пока скрипт её включит. */
    public boolean enabled() {
        return enabled;
    }

    /** Все параметры префаба, включая значения по умолчанию. Не изменять. */
    public Map<String, JsonValue> params() {
        return params;
    }

    /** Значение параметра (JSON {@code null} для необязательного без значения). */
    public JsonValue param(String name) {
        JsonValue value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "У префаба '" + prefab.id() + "' нет параметра '" + name + "'");
        }
        return value;
    }

    /** Обработчики событий: {@code use} → код DGScript. */
    public Map<String, String> handlers() {
        return handlers;
    }

    /**
     * Данные компонентов с подставленными параметрами. Каждый вызов возвращает новые копии, их
     * можно изменять.
     */
    public Map<String, JsonValue> components() {
        Map<String, JsonValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> component : prefab.components().entrySet()) {
            result.put(component.getKey(), substitute(component.getValue()));
        }
        return result;
    }

    private JsonValue substitute(JsonValue value) {
        if (value.isString() && value.asString().startsWith(Prefab.PARAM_REF)) {
            return JsonValues.copy(param(value.asString().substring(Prefab.PARAM_REF.length())));
        }
        if (value.isObject() || value.isArray()) {
            JsonValue copy = new JsonValue(value.type());
            for (JsonValue child = value.child; child != null; child = child.next) {
                JsonValues.append(copy, child.name, substitute(child));
            }
            return copy;
        }
        return JsonValues.copy(value);
    }

    @Override
    public String toString() {
        return prefab.id() + (id == null ? "" : "#" + id) + "@" + at;
    }
}
