package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Шаблон сущности: набор компонентов с параметрами. Сущность уровня = префаб + значения параметров.
 *
 * <pre>{@code
 * {
 *   "id": "flamethrower",
 *   "placement": "wall",
 *   "params": { "dir": { "type": "direction", "default": "right" } },
 *   "components": { "Emitter": { "kind": "fire", "dir": "$dir" } }
 * }
 * }</pre>
 *
 * <p>Строка {@code "$имя"} в данных компонента заменяется значением параметра целиком (с типом).
 */
public final class Prefab {

    /** Префикс ссылки на параметр в данных компонента. */
    public static final String PARAM_REF = "$";

    private final String id;
    private final String description;
    private final Placement placement;
    private final Set<String> events;
    private final Map<String, ParamSpec> params;
    private final Map<String, JsonValue> components;

    Prefab(
            String id,
            String description,
            Placement placement,
            Set<String> events,
            Map<String, ParamSpec> params,
            Map<String, JsonValue> components) {
        this.id = id;
        this.description = description;
        this.placement = placement;
        this.events = events;
        this.params = params;
        this.components = components;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Placement placement() {
        return placement;
    }

    /**
     * События, на которые в уровне можно повесить обработчики DGScript: {@code use} → {@code
     * onUse}.
     */
    public Set<String> events() {
        return events;
    }

    /** Параметры в порядке объявления. */
    public Map<String, ParamSpec> params() {
        return params;
    }

    /** Данные компонентов со ссылками {@code $param} (не изменять). */
    public Map<String, JsonValue> components() {
        return components;
    }

    public boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    /** Имя ключа обработчика в уровне: {@code use} → {@code onUse}. */
    public static String handlerKey(String event) {
        return "on" + event.substring(0, 1).toUpperCase(Locale.ROOT) + event.substring(1);
    }

    @Override
    public String toString() {
        return "Prefab[" + id + "]";
    }
}
