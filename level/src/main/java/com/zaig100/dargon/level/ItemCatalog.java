package com.zaig100.dargon.level;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Каталог предметов. Стопка предмета в уровне — {@code {"id": "potion", "count": 2}} плюс
 * дополнительные поля, объявленные в {@code fields} типа предмета.
 */
public final class ItemCatalog {

    /** Закрытый набор эффектов; каждый реализуется в игре отдельным обработчиком. */
    public static final List<String> EFFECTS =
            List.of(
                    "coins",
                    "heal",
                    "shield",
                    "light",
                    "trap_sight",
                    "speed",
                    "slow",
                    "key",
                    "egg");

    private static final Set<String> ITEM_KEYS =
            Set.of("name", "icon", "maxStack", "effect", "args", "fields", "description");

    private final Map<String, ItemDefinition> items;

    private ItemCatalog(Map<String, ItemDefinition> items) {
        this.items = items;
    }

    /** Пустой каталог (для тестов, где предметы не нужны). */
    public static ItemCatalog empty() {
        return new ItemCatalog(Map.of());
    }

    /**
     * Разбирает {@code items.json}.
     *
     * @throws LevelFormatException если файл некорректен
     */
    public static ItemCatalog parse(String source, String file) {
        Problems problems = new Problems(file);
        JsonValue root = JsonValues.parse(source, file);
        Map<String, ItemDefinition> items = new LinkedHashMap<>();
        if (!root.isObject()) {
            problems.error("", "ожидался объект");
            problems.throwIfErrors();
        }
        JsonValues.checkKeys(root, "", Set.of("$schema", "items"), problems);
        JsonValue list = JsonValues.get(root, "items");
        if (list == null || !list.isObject()) {
            problems.error("items", "ожидался объект с типами предметов");
            problems.throwIfErrors();
        }
        ItemCatalog catalog = new ItemCatalog(Collections.unmodifiableMap(items));
        for (JsonValue item = list.child; item != null; item = item.next) {
            ItemDefinition definition =
                    parseItem(item, JsonValues.child("items", item.name), problems, catalog);
            if (definition != null) {
                items.put(definition.id(), definition);
            }
        }
        problems.throwIfErrors();
        return catalog;
    }

    private static ItemDefinition parseItem(
            JsonValue item, String path, Problems problems, ItemCatalog catalog) {
        if (!item.isObject()) {
            problems.error(path, "ожидался объект");
            return null;
        }
        if (!item.name.matches("[a-z][a-z0-9_]*")) {
            problems.error(path, "идентификатор предмета — строчные латинские буквы, цифры и '_'");
        }
        JsonValues.checkKeys(item, path, ITEM_KEYS, problems);
        String icon = JsonValues.getString(item, "icon", null);
        if (icon == null) {
            problems.error(path, "нет поля 'icon'");
        }
        String effect = JsonValues.getString(item, "effect", null);
        if (effect == null || !EFFECTS.contains(effect)) {
            problems.error(
                    JsonValues.child(path, "effect"),
                    "эффект должен быть одним из: " + String.join(", ", EFFECTS));
        }
        JsonValue maxStackJson = JsonValues.get(item, "maxStack");
        int maxStack = 99;
        if (maxStackJson != null) {
            if (!JsonValues.isInteger(maxStackJson) || maxStackJson.asInt() < 1) {
                problems.error(
                        JsonValues.child(path, "maxStack"), "ожидалось целое число не меньше 1");
            } else {
                maxStack = maxStackJson.asInt();
            }
        }
        JsonValue args = JsonValues.get(item, "args");
        if (args == null) {
            args = new JsonValue(JsonValue.ValueType.object);
        } else if (!args.isObject()) {
            problems.error(JsonValues.child(path, "args"), "ожидался объект");
        }
        Map<String, ParamSpec> fields = new LinkedHashMap<>();
        JsonValue fieldsJson = JsonValues.get(item, "fields");
        if (fieldsJson != null) {
            String fieldsPath = JsonValues.child(path, "fields");
            for (JsonValue field = fieldsJson.child; field != null; field = field.next) {
                if (field.name.equals("id") || field.name.equals("count")) {
                    problems.error(
                            JsonValues.child(fieldsPath, field.name), "имя поля зарезервировано");
                    continue;
                }
                ParamSpec spec =
                        ParamSpec.parse(
                                field.name,
                                field,
                                JsonValues.child(fieldsPath, field.name),
                                problems,
                                catalog);
                if (spec != null) {
                    fields.put(field.name, spec);
                }
            }
        }
        String name =
                JsonValues.getString(item, "name", ParamType.TEXT_KEY_PREFIX + "item." + item.name);
        return new ItemDefinition(
                item.name, name, icon, maxStack, effect, args, Collections.unmodifiableMap(fields));
    }

    public ItemDefinition get(String id) {
        return items.get(id);
    }

    public Collection<ItemDefinition> all() {
        return items.values();
    }

    /** Проверяет стопку предмета {@code {"id": ..., "count": ..., ...}}. */
    public void validateStack(JsonValue stack, String path, Problems problems) {
        if (!stack.isObject()) {
            problems.error(
                    path,
                    "ожидался предмет {\"id\": ..., \"count\": ...}, получено "
                            + JsonValues.describe(stack));
            return;
        }
        String id = JsonValues.getString(stack, "id", null);
        ItemDefinition definition = id == null ? null : items.get(id);
        if (definition == null) {
            problems.error(
                    JsonValues.child(path, "id"),
                    (id == null
                            ? "нет поля 'id'"
                            : "неизвестный предмет '"
                                    + id
                                    + "'"
                                    + JsonValues.suggest(id, items.keySet())));
            return;
        }
        JsonValue count = JsonValues.get(stack, "count");
        if (count != null
                && (!JsonValues.isInteger(count)
                        || count.asInt() < 1
                        || count.asInt() > definition.maxStack())) {
            problems.error(
                    JsonValues.child(path, "count"),
                    "количество должно быть целым от 1 до "
                            + definition.maxStack()
                            + ", получено "
                            + JsonValues.describe(count));
        }
        for (JsonValue field = stack.child; field != null; field = field.next) {
            if (field.name.equals("id") || field.name.equals("count")) {
                continue;
            }
            ParamSpec spec = definition.fields().get(field.name);
            if (spec == null) {
                problems.error(
                        JsonValues.child(path, field.name),
                        "у предмета '"
                                + id
                                + "' нет поля '"
                                + field.name
                                + "'"
                                + JsonValues.suggest(field.name, definition.fields().keySet()));
            } else {
                spec.validate(field, JsonValues.child(path, field.name), problems, this);
            }
        }
        for (ParamSpec spec : definition.fields().values()) {
            if (spec.isRequired() && JsonValues.get(stack, spec.name()) == null) {
                problems.error(
                        path, "у предмета '" + id + "' обязательно поле '" + spec.name() + "'");
            }
        }
    }
}
