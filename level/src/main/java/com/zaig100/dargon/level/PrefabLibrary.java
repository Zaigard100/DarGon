package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Все префабы игры. Собирается из файлов {@code assets/data/prefabs/*.json}. */
public final class PrefabLibrary {

    private static final Set<String> KEYS =
            Set.of("$schema", "id", "description", "placement", "events", "params", "components");

    private final Map<String, Prefab> prefabs = new TreeMap<>();
    private final ItemCatalog items;

    public PrefabLibrary(ItemCatalog items) {
        this.items = items;
    }

    public ItemCatalog items() {
        return items;
    }

    /**
     * Разбирает и добавляет префаб. Его {@code id} должен совпадать с именем файла без расширения.
     *
     * @throws LevelFormatException если префаб некорректен
     */
    public Prefab add(String source, String file) {
        Problems problems = new Problems(file);
        Prefab prefab = parse(JsonValues.parse(source, file), file, problems);
        problems.throwIfErrors();
        if (prefabs.containsKey(prefab.id())) {
            throw new LevelFormatException(file, "id", "префаб '" + prefab.id() + "' уже объявлен");
        }
        prefabs.put(prefab.id(), prefab);
        return prefab;
    }

    public Prefab get(String id) {
        return prefabs.get(id);
    }

    public Collection<Prefab> all() {
        return Collections.unmodifiableCollection(prefabs.values());
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(prefabs.keySet());
    }

    private Prefab parse(JsonValue root, String file, Problems problems) {
        if (!root.isObject()) {
            problems.error("", "ожидался объект");
            return null;
        }
        JsonValues.checkKeys(root, "", KEYS, problems);
        String id = JsonValues.getString(root, "id", null);
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
            problems.error("id", "идентификатор префаба — строчные латинские буквы, цифры и '_'");
        } else {
            String base = file.replace('\\', '/');
            base = base.substring(base.lastIndexOf('/') + 1);
            if (base.endsWith(".json") && !base.equals(id + ".json")) {
                problems.error("id", "id '" + id + "' не совпадает с именем файла " + base);
            }
        }
        Placement placement = Placement.FLOOR;
        String placementName = JsonValues.getString(root, "placement", null);
        if (placementName != null) {
            placement = Placement.byId(placementName);
            if (placement == null) {
                problems.error("placement", "ожидалось floor, wall или any");
                placement = Placement.FLOOR;
            }
        }
        Set<String> events = new LinkedHashSet<>();
        JsonValue eventsJson = JsonValues.get(root, "events");
        if (eventsJson != null) {
            int i = 0;
            for (JsonValue event = eventsJson.child; event != null; event = event.next, i++) {
                if (!event.isString() || !event.asString().matches("[a-z][a-zA-Z]*")) {
                    problems.error(
                            JsonValues.index("events", i),
                            "имя события — латиница в camelCase со строчной буквы");
                } else {
                    events.add(event.asString());
                }
            }
        }
        Map<String, ParamSpec> params = new LinkedHashMap<>();
        JsonValue paramsJson = JsonValues.get(root, "params");
        if (paramsJson != null) {
            for (JsonValue param = paramsJson.child; param != null; param = param.next) {
                String path = JsonValues.child("params", param.name);
                if (LevelParser.RESERVED_ENTITY_KEYS.contains(param.name)
                        || param.name.matches("on[A-Z].*")) {
                    problems.error(path, "имя параметра зарезервировано");
                    continue;
                }
                ParamSpec spec = ParamSpec.parse(param.name, param, path, problems, items);
                if (spec != null) {
                    params.put(param.name, spec);
                }
            }
        }
        Map<String, JsonValue> components = new LinkedHashMap<>();
        Set<String> used = new LinkedHashSet<>();
        JsonValue componentsJson = JsonValues.get(root, "components");
        if (componentsJson == null || !componentsJson.isObject() || componentsJson.child == null) {
            problems.error("components", "у префаба должен быть хотя бы один компонент");
        } else {
            for (JsonValue component = componentsJson.child;
                    component != null;
                    component = component.next) {
                String path = JsonValues.child("components", component.name);
                if (!component.name.matches("[A-Z][A-Za-z0-9]*")) {
                    problems.error(path, "имя компонента — в PascalCase");
                }
                if (!component.isObject()) {
                    problems.error(path, "данные компонента должны быть объектом");
                    continue;
                }
                collectRefs(component, path, params, used, problems);
                components.put(component.name, component);
            }
        }
        for (String param : params.keySet()) {
            if (!used.contains(param)) {
                problems.warning(
                        JsonValues.child("params", param),
                        "параметр не используется ни одним компонентом");
            }
        }
        return new Prefab(
                id,
                JsonValues.getString(root, "description", ""),
                placement,
                Collections.unmodifiableSet(events),
                Collections.unmodifiableMap(params),
                Collections.unmodifiableMap(components));
    }

    private static void collectRefs(
            JsonValue value,
            String path,
            Map<String, ParamSpec> params,
            Set<String> used,
            Problems problems) {
        if (value.isString() && value.asString().startsWith(Prefab.PARAM_REF)) {
            String name = value.asString().substring(Prefab.PARAM_REF.length());
            if (!params.containsKey(name)) {
                problems.error(
                        path,
                        "ссылка на необъявленный параметр '"
                                + name
                                + "'"
                                + JsonValues.suggest(name, params.keySet()));
            }
            used.add(name);
            return;
        }
        int i = 0;
        for (JsonValue child = value.child; child != null; child = child.next, i++) {
            collectRefs(
                    child,
                    value.isArray()
                            ? JsonValues.index(path, i)
                            : JsonValues.child(path, child.name),
                    params,
                    used,
                    problems);
        }
    }
}
