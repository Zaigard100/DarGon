package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Строит JSON Schema файла уровня из префабов и каталога предметов. Схема даёт редактору
 * автодополнение параметров каждого префаба, а CI проверяет ею все уровни.
 *
 * <p>Файл {@code assets/data/level.schema.json} генерируется ({@code level-check --write-schema}),
 * руками его не правят.
 */
public final class LevelSchema {

    private LevelSchema() {}

    /** Схема в виде дерева {@link Map}/{@link List} для {@link JsonPrinter}. */
    public static Map<String, Object> build(GameData data) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("$id", "level.schema.json");
        schema.put("title", "DarGon: уровень");
        schema.put(
                "description",
                "Сгенерировано level-check --write-schema из префабов и items.json. Не править вручную.");
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("formatVersion", "name", "tiles", "spawn"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("$schema", type("string"));
        properties.put("formatVersion", obj("const", LevelDefinition.FORMAT_VERSION));
        properties.put(
                "name",
                describe(ref("text"), "Название уровня: ключ локализации \"@level.05.name\"."));
        properties.put("comment", type("string"));
        properties.put(
                "dark", describe(type("boolean"), "Тёмный уровень: видно только вокруг игрока."));
        properties.put(
                "checkpoint",
                describe(type("boolean"), "Контрольная точка: при входе игра сохраняется."));
        Map<String, Object> legend = type("object");
        legend.put("propertyNames", obj("minLength", 1, "maxLength", 1));
        legend.put("additionalProperties", obj("enum", tileIds()));
        properties.put(
                "legend",
                describe(
                        legend,
                        "Символ карты → тип клетки. По умолчанию: \".\" — floor, \"#\" — wall."));
        Map<String, Object> tiles = type("array");
        tiles.put("minItems", 1);
        tiles.put("items", obj("type", "string", "minLength", 1));
        properties.put("tiles", describe(tiles, "Карта: строки одинаковой длины, Y растёт вниз."));
        Map<String, Object> edges = type("array");
        edges.put("items", obj("type", "string", "pattern", "^[.#1-8]+$"));
        properties.put(
                "edges",
                describe(
                        edges,
                        "Ручные края стен, размером с карту: \".\" — авто, \"#\" — без края,"
                                + " 1–8 — край (порядок WallEdge)."));
        properties.put("spawn", describe(ref("point"), "Точка появления игрока [x, y]."));
        properties.put(
                "script",
                describe(
                        obj("type", "string", "pattern", "\\.dgs$"),
                        "Файл DGScript рядом с уровнем."));
        Map<String, Object> entities = type("array");
        entities.put("items", ref("entity"));
        properties.put("entities", entities);
        schema.put("properties", properties);

        Map<String, Object> defs = new LinkedHashMap<>();
        defs.put("point", point());
        defs.put("text", text());
        defs.put("direction", obj("enum", directions(true)));
        defs.put("direction8", obj("enum", directions(false)));
        defs.put("color", obj("type", "string", "pattern", "^#[0-9A-Fa-f]{6}$"));
        // Схема общая для всех паков: id уровня — только формат, существование проверяет загрузчик.
        defs.put(
                "level",
                obj(
                        "type",
                        "string",
                        "pattern",
                        "^[A-Za-z0-9_]+(/[A-Za-z0-9_]+)*$",
                        "description",
                        "id уровня из списка levels в pack.json"));
        defs.put(
                "damage",
                obj(
                        "anyOf",
                        List.of(
                                obj("type", "integer", "minimum", 0),
                                obj("const", "kill"),
                                obj(
                                        "type",
                                        "array",
                                        "items",
                                        obj("type", "integer", "minimum", 0),
                                        "minItems",
                                        2,
                                        "maxItems",
                                        2))));
        defs.put("handler", handler());
        defs.put("itemStack", itemStack(data.items()));
        defs.put("offers", offers());
        defs.put("entity", entity(data.prefabs(), data.packPrefabs()));
        for (Prefab prefab : data.prefabs().all()) {
            if (data.packPrefabs().contains(prefab.id())) {
                continue; // схема общая для всех паков — только префабы игры
            }
            defs.put("prefab_" + prefab.id(), prefab(prefab));
        }
        schema.put("$defs", defs);
        return schema;
    }

    private static List<String> gameIds(PrefabLibrary prefabs, List<String> packPrefabs) {
        List<String> ids = new ArrayList<>();
        for (String id : prefabs.ids()) {
            if (!packPrefabs.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static Map<String, Object> entity(PrefabLibrary prefabs, List<String> packPrefabs) {
        Map<String, Object> entity = type("object");
        entity.put("required", List.of("prefab", "at"));
        Map<String, Object> properties = new LinkedHashMap<>();
        // Префабы игры — с подсказками; свои префабы паков — любой id (параметры проверяет игра).
        properties.put(
                "prefab",
                obj(
                        "anyOf",
                        List.of(
                                obj("enum", new ArrayList<>(gameIds(prefabs, packPrefabs))),
                                obj("type", "string", "pattern", "^[a-z][a-z0-9_]*$"))));
        entity.put("properties", properties);
        List<Object> cases = new ArrayList<>();
        for (Prefab prefab : prefabs.all()) {
            if (packPrefabs.contains(prefab.id())) {
                continue;
            }
            Map<String, Object> when = obj("properties", obj("prefab", obj("const", prefab.id())));
            Map<String, Object> branch = new LinkedHashMap<>();
            branch.put("if", when);
            branch.put("then", ref("prefab_" + prefab.id()));
            cases.add(branch);
        }
        entity.put("allOf", cases);
        return entity;
    }

    private static Map<String, Object> prefab(Prefab prefab) {
        Map<String, Object> schema = type("object");
        if (!prefab.description().isEmpty()) {
            schema.put("description", prefab.description());
        }
        schema.put("additionalProperties", false);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("prefab", obj("const", prefab.id()));
        properties.put(
                "id",
                describe(
                        obj("type", "string", "pattern", "^[A-Za-z][A-Za-z0-9_]*$"),
                        "Уникальный id для скриптов: entity('id')."));
        Map<String, Object> tags = type("array");
        tags.put("uniqueItems", true);
        tags.put("items", obj("type", "string", "pattern", "^[A-Za-z][A-Za-z0-9_]*$"));
        properties.put("tags", describe(tags, "Групповые метки для скриптов."));
        properties.put("at", describe(ref("point"), "Клетка [x, y]."));
        properties.put(
                "enabled",
                describe(type("boolean"), "false — создаётся выключенной, включает скрипт."));
        properties.put("comment", type("string"));
        List<String> required = new ArrayList<>(List.of("prefab", "at"));
        for (ParamSpec param : prefab.params().values()) {
            properties.put(param.name(), param(param));
            if (param.isRequired()) {
                required.add(param.name());
            }
        }
        for (String event : prefab.events()) {
            properties.put(
                    Prefab.handlerKey(event),
                    describe(ref("handler"), "Код DGScript для события " + event + "."));
        }
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    /** Схема значения параметра (или поля предмета). */
    private static Map<String, Object> param(ParamSpec param) {
        Map<String, Object> schema;
        switch (param.type()) {
            case INT:
            case NUMBER:
                schema = type(param.type() == ParamType.INT ? "integer" : "number");
                if (param.min() != null) {
                    schema.put("minimum", number(param.min()));
                }
                if (param.max() != null) {
                    schema.put("maximum", number(param.max()));
                }
                break;
            case BOOL:
                schema = type("boolean");
                break;
            case STRING:
                schema = type("string");
                if (param.values() != null) {
                    schema.put("enum", param.values());
                } else {
                    schema.put("minLength", 1);
                }
                break;
            case ENTITY:
                schema = obj("type", "string", "minLength", 1);
                break;
            case ITEMS:
                schema = obj("type", "array", "items", ref("itemStack"));
                break;
            case ITEM:
                schema = ref("itemStack");
                break;
            default:
                schema = ref(param.type().id());
                break;
        }
        if (param.isOptional()) {
            schema = obj("anyOf", List.of(schema, type("null")));
        }
        Map<String, Object> result = new LinkedHashMap<>(schema);
        if (!param.description().isEmpty()) {
            result.put("description", param.description());
        }
        if (param.defaultValue() != null) {
            result.put("default", plain(param.defaultValue()));
        }
        return result;
    }

    private static Map<String, Object> itemStack(ItemCatalog items) {
        Map<String, Object> stack = type("object");
        stack.put("required", List.of("id"));
        List<String> ids = new ArrayList<>();
        for (ItemDefinition item : items.all()) {
            ids.add(item.id());
        }
        stack.put("properties", obj("id", obj("enum", ids)));
        List<Object> cases = new ArrayList<>();
        for (ItemDefinition item : items.all()) {
            Map<String, Object> then = type("object");
            then.put("additionalProperties", false);
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", obj("const", item.id()));
            properties.put(
                    "count", obj("type", "integer", "minimum", 1, "maximum", item.maxStack()));
            List<String> required = new ArrayList<>(List.of("id"));
            for (ParamSpec field : item.fields().values()) {
                properties.put(field.name(), param(field));
                if (field.isRequired()) {
                    required.add(field.name());
                }
            }
            then.put("properties", properties);
            then.put("required", required);
            Map<String, Object> branch = new LinkedHashMap<>();
            branch.put("if", obj("properties", obj("id", obj("const", item.id()))));
            branch.put("then", then);
            cases.add(branch);
        }
        stack.put("allOf", cases);
        return stack;
    }

    private static Map<String, Object> offers() {
        Map<String, Object> offer = type("object");
        offer.put("additionalProperties", false);
        offer.put("required", List.of("item", "price"));
        offer.put(
                "properties",
                obj(
                        "item",
                        ref("itemStack"),
                        "price",
                        obj("type", "integer", "minimum", 0),
                        "stock",
                        obj(
                                "type",
                                "integer",
                                "minimum",
                                1,
                                "description",
                                "Сколько штук в продаже (по умолчанию 1).")));
        return obj("type", "array", "items", offer);
    }

    private static Map<String, Object> point() {
        Map<String, Object> point = type("array");
        point.put("items", type("integer"));
        point.put("minItems", 2);
        point.put("maxItems", 2);
        return point;
    }

    private static Map<String, Object> text() {
        return obj(
                "description",
                "Ключ локализации \"@...\" или (для черновика) строка / массив строк.",
                "anyOf",
                List.of(
                        obj("type", "string", "minLength", 1),
                        obj("type", "array", "items", type("string"))));
    }

    private static Map<String, Object> handler() {
        return obj(
                "description",
                "Код DGScript: строка или массив строк (по строке кода на элемент).",
                "anyOf",
                List.of(type("string"), obj("type", "array", "items", type("string"))));
    }

    private static List<String> directions(boolean orthogonal) {
        List<String> result = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (!orthogonal || direction.isOrthogonal()) {
                result.add(direction.id());
            }
        }
        return result;
    }

    private static List<String> tileIds() {
        List<String> result = new ArrayList<>();
        for (TileType type : TileType.values()) {
            result.add(type.id());
        }
        return result;
    }

    /** Объект с сохранением порядка ключей: {@code obj("type", "string", "minLength", 1)}. */
    private static Map<String, Object> obj(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> type(String type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        return schema;
    }

    private static Map<String, Object> ref(String def) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$ref", "#/$defs/" + def);
        return schema;
    }

    private static Map<String, Object> describe(Map<String, Object> schema, String description) {
        Map<String, Object> result = new LinkedHashMap<>(schema);
        result.put("description", description);
        return result;
    }

    private static Object number(double value) {
        return value == Math.rint(value) ? (Object) (long) value : (Object) value;
    }

    /** {@link JsonValue} → {@link Map}/{@link List}/примитив для печати. */
    static Object plain(JsonValue value) {
        switch (value.type()) {
            case object:
                Map<String, Object> map = new LinkedHashMap<>();
                for (JsonValue child = value.child; child != null; child = child.next) {
                    map.put(child.name, plain(child));
                }
                return map;
            case array:
                List<Object> list = new ArrayList<>();
                for (JsonValue child = value.child; child != null; child = child.next) {
                    list.add(plain(child));
                }
                return list;
            case stringValue:
                return value.asString();
            case doubleValue:
                return value.asDouble();
            case longValue:
                return value.asLong();
            case booleanValue:
                return value.asBoolean();
            default:
                return null;
        }
    }
}
