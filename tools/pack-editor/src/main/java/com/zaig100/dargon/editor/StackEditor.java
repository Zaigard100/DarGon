package com.zaig100.dargon.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ItemDefinition;
import com.zaig100.dargon.level.ParamSpec;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Стопка предметов {@code {"id": "key", "count": 1, "key": "red", "color": "#FF0000"}}: предмет из
 * списка, количество и поля этого предмета (цвет ключа, сила зелья…) — по его описанию.
 */
final class StackEditor extends VisTable {

    private static Map<String, String> names;

    private final ItemCatalog items;
    private final Map<String, Object> stack;
    private final Runnable changed;

    StackEditor(ItemCatalog items, Map<String, Object> stack, Runnable changed) {
        this.items = items;
        this.stack = stack;
        this.changed = changed;
        left();
        defaults().left().padRight(4);
        build();
    }

    private void build() {
        clearChildren();
        List<Form.Option> options = new ArrayList<>();
        for (ItemDefinition item : items.all()) {
            options.add(new Form.Option(item.id(), name(item.id())));
        }
        String id = String.valueOf(stack.getOrDefault("id", ""));
        add(Form.select(
                        options,
                        id,
                        v -> {
                            stack.put("id", v);
                            // Поля другого предмета не подходят новому.
                            ItemDefinition def = items.get(v);
                            stack.keySet()
                                    .removeIf(
                                            k ->
                                                    !k.equals("id")
                                                            && !k.equals("count")
                                                            && (def == null
                                                                    || !def.fields()
                                                                            .containsKey(k)));
                            build();
                            changed.run();
                        }))
                .width(230);
        add(new VisLabel("×"));
        add(Form.integer(
                        stack.get("count"),
                        1,
                        n -> {
                            put("count", n);
                            changed.run();
                        }))
                .width(50);
        ItemDefinition def = items.get(id);
        if (def != null) {
            for (ParamSpec spec : def.fields().values()) {
                add(new VisLabel(spec.name())).padLeft(6);
                add(field(spec)).width(spec.values() != null ? 150 : 90);
            }
        }
    }

    private com.badlogic.gdx.scenes.scene2d.Actor field(ParamSpec spec) {
        Object value = stack.get(spec.name());
        if (spec.values() != null && !spec.values().isEmpty()) {
            List<Form.Option> options = new ArrayList<>();
            options.add(new Form.Option(null, "(не задано)"));
            for (String v : spec.values()) {
                options.add(new Form.Option(v, v));
            }
            return Form.select(
                    options,
                    value == null ? null : String.valueOf(value),
                    v -> {
                        put(spec.name(), v);
                        changed.run();
                    });
        }
        return switch (spec.type()) {
            case INT ->
                    Form.integer(
                            value,
                            Integer.MIN_VALUE,
                            n -> {
                                put(spec.name(), n);
                                changed.run();
                            });
            case NUMBER ->
                    Form.number(
                            value,
                            n -> {
                                put(spec.name(), n);
                                changed.run();
                            });
            case BOOL ->
                    Form.check(
                            "",
                            Boolean.TRUE.equals(value),
                            b -> {
                                put(spec.name(), b ? true : null);
                                changed.run();
                            });
            default ->
                    Form.text(
                            value == null ? "" : String.valueOf(value),
                            v -> {
                                put(spec.name(), v.isBlank() ? null : v.strip());
                                changed.run();
                            });
        };
    }

    private void put(String key, Object value) {
        if (value == null) {
            stack.remove(key);
        } else {
            stack.put(key, value);
        }
    }

    /** Новая стопка: зелье. */
    static Map<String, Object> newStack() {
        Map<String, Object> stack = new LinkedHashMap<>();
        stack.put("id", "potion");
        return stack;
    }

    /** Название предмета по-русски из текстов игры ({@code item.<id>} до двоеточия). */
    static String name(String id) {
        if (names == null) {
            names = new LinkedHashMap<>();
            FileHandle file = Gdx.files.internal("i18n/ui_ru.properties");
            if (file.exists()) {
                Properties p = new Properties();
                try {
                    p.load(new StringReader(file.readString("UTF-8")));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
                for (String key : p.stringPropertyNames()) {
                    if (key.startsWith("item.")) {
                        names.put(key.substring(5), p.getProperty(key).split(":", 2)[0].strip());
                    }
                }
            }
        }
        String name = names.get(id);
        return name == null ? id : name + " (" + id + ")";
    }

    /**
     * Список стопок или товаров магазина ({@code {"item": {...}, "price": 5, "stock": 2}}) — по
     * строке на элемент, «×» удаляет, «Добавить» — новая строка.
     */
    static final class ListEditor extends VisTable {
        private final ItemCatalog items;
        private final List<Object> list;
        private final boolean offers;
        private final Runnable changed;

        ListEditor(ItemCatalog items, List<Object> list, boolean offers, Runnable changed) {
            this.items = items;
            this.list = list;
            this.offers = offers;
            this.changed = changed;
            left();
            defaults().left().pad(1);
            build();
        }

        @SuppressWarnings("unchecked")
        private void build() {
            clearChildren();
            for (int i = 0; i < list.size(); i++) {
                int index = i;
                Object element = list.get(i);
                if (!(element instanceof Map<?, ?>)) {
                    add(new VisLabel("не объект: " + element)).row();
                    continue;
                }
                Map<String, Object> row = (Map<String, Object>) element;
                VisTable line = new VisTable();
                line.left();
                if (offers) {
                    Object item = row.computeIfAbsent("item", k -> newStack());
                    line.add(new StackEditor(items, (Map<String, Object>) item, changed));
                    line.add(new VisLabel("цена")).padLeft(6);
                    line.add(
                                    Form.integer(
                                            row.get("price"),
                                            0,
                                            n -> {
                                                put(row, "price", n);
                                                changed.run();
                                            }))
                            .width(50);
                    line.add(new VisLabel("запас")).padLeft(6);
                    VisTable stock = new VisTable();
                    stock.add(
                                    Form.integer(
                                            row.get("stock"),
                                            1,
                                            n -> {
                                                put(row, "stock", n);
                                                changed.run();
                                            }))
                            .width(50);
                    line.add(stock);
                } else {
                    line.add(new StackEditor(items, row, changed));
                }
                VisTextButton remove = new VisTextButton("×");
                remove.addListener(
                        Form.changed(
                                () -> {
                                    list.remove(index);
                                    build();
                                    changed.run();
                                }));
                line.add(remove).padLeft(4);
                add(line).row();
            }
            VisTextButton plus = new VisTextButton(offers ? "Добавить товар" : "Добавить предмет");
            plus.addListener(
                    Form.changed(
                            () -> {
                                if (offers) {
                                    Map<String, Object> offer = new LinkedHashMap<>();
                                    offer.put("item", newStack());
                                    offer.put("price", 5L);
                                    list.add(offer);
                                } else {
                                    list.add(newStack());
                                }
                                build();
                                changed.run();
                            }));
            add(plus).row();
        }

        private static void put(Map<String, Object> map, String key, Object value) {
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, value);
            }
        }
    }
}
