package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Компоненты, из которых собираются префабы ({@code data/components.json}): поля с типами, как у
 * параметров префаба. По каталогу проверяются префабы паков и строятся формы в редакторе.
 */
public final class ComponentCatalog {

    public static final String FILE = "data/components.json";

    /** Компонент: описание и поля в порядке объявления. */
    public record Component(String name, String description, Map<String, ParamSpec> fields) {}

    private final Map<String, Component> components;
    private final List<String> events;

    private ComponentCatalog(Map<String, Component> components, List<String> events) {
        this.components = components;
        this.events = events;
    }

    /**
     * @throws LevelFormatException если файл некорректен
     */
    public static ComponentCatalog parse(String source, String file, ItemCatalog items) {
        JsonValue root = JsonValues.parse(source, file);
        Problems problems = new Problems(file);
        JsonValues.checkKeys(root, "", Set.of("$schema", "events", "components"), problems);
        List<String> events = new ArrayList<>();
        JsonValue ev = JsonValues.get(root, "events");
        for (JsonValue e = ev == null ? null : ev.child; e != null; e = e.next) {
            events.add(e.asString());
        }
        Map<String, Component> components = new LinkedHashMap<>();
        JsonValue list = JsonValues.get(root, "components");
        if (list == null || !list.isObject()) {
            problems.error("components", "ожидался объект компонентов");
        }
        for (JsonValue c = list == null ? null : list.child; c != null; c = c.next) {
            String path = JsonValues.child("components", c.name);
            JsonValues.checkKeys(c, path, Set.of("description", "fields"), problems);
            Map<String, ParamSpec> fields = new LinkedHashMap<>();
            JsonValue f = JsonValues.get(c, "fields");
            for (JsonValue field = f == null ? null : f.child; field != null; field = field.next) {
                ParamSpec spec =
                        ParamSpec.parse(
                                field.name,
                                field,
                                JsonValues.child(JsonValues.child(path, "fields"), field.name),
                                problems,
                                items);
                if (spec != null) {
                    fields.put(field.name, spec);
                }
            }
            components.put(
                    c.name,
                    new Component(
                            c.name,
                            JsonValues.getString(c, "description", ""),
                            Collections.unmodifiableMap(fields)));
        }
        problems.throwIfErrors();
        return new ComponentCatalog(
                Collections.unmodifiableMap(components), Collections.unmodifiableList(events));
    }

    public Map<String, Component> all() {
        return components;
    }

    public Component get(String name) {
        return components.get(name);
    }

    /** События, на которые можно повесить обработчики DGScript. */
    public List<String> events() {
        return events;
    }

    /**
     * Проверяет компоненты префаба: известные компоненты и поля, типы значений, ссылки {@code
     * $параметр} на существующие параметры, обязательные поля, события.
     */
    public void validate(Prefab prefab, String file, Problems problems, ItemCatalog items) {
        for (String event : prefab.events()) {
            if (!events.contains(event)) {
                problems.error("events", "неизвестное событие '" + event + "'; есть " + events);
            }
        }
        for (Map.Entry<String, JsonValue> entry : prefab.components().entrySet()) {
            String path = JsonValues.child("components", entry.getKey());
            Component component = components.get(entry.getKey());
            if (component == null) {
                problems.error(
                        path,
                        "неизвестный компонент '"
                                + entry.getKey()
                                + "'; есть "
                                + components.keySet());
                continue;
            }
            JsonValue data = entry.getValue();
            if (!data.isObject()) {
                problems.error(path, "ожидался объект полей");
                continue;
            }
            JsonValues.checkKeys(data, path, component.fields().keySet(), problems);
            for (ParamSpec field : component.fields().values()) {
                JsonValue value = data.get(field.name());
                String fieldPath = JsonValues.child(path, field.name());
                if (value == null) {
                    if (field.isRequired()) {
                        problems.error(fieldPath, "не задано обязательное поле");
                    }
                    continue;
                }
                if (value.isString() && value.asString().startsWith(Prefab.PARAM_REF)) {
                    String param = value.asString().substring(Prefab.PARAM_REF.length());
                    if (!prefab.params().containsKey(param)) {
                        problems.error(fieldPath, "нет параметра '" + param + "' у префаба");
                    }
                    continue;
                }
                field.validate(value, fieldPath, problems, items);
            }
        }
    }
}
