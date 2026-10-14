package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.zaig100.dargon.editor.model.Json;
import com.zaig100.dargon.editor.model.LevelDoc;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.level.Direction;
import com.zaig100.dargon.level.ItemDefinition;
import com.zaig100.dargon.level.JsonPrinter;
import com.zaig100.dargon.level.ParamSpec;
import com.zaig100.dargon.level.ParamType;
import com.zaig100.dargon.level.Prefab;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Свойства справа: уровня (название на двух языках, флаги, размер карты) или выделенной сущности
 * (параметры префаба — поле по типу, тексты на двух языках, обработчики DGScript). Каждое поле —
 * одна правка для отмены, пока в нём фокус.
 */
final class Inspector extends VisTable {

    private static final float FIELD = 190;
    private static final float LABEL = 120;
    private static final String DEFAULT = "(по умолчанию)";
    private static final String NONE = "(нет)";

    private final EditorApp app;
    private PackProject project;
    private LevelDoc doc;

    /** Снимок для отмены уже сделан в этом поле (сбрасывается, когда фокус уходит). */
    private boolean snapshot;

    Inspector(EditorApp app) {
        this.app = app;
        top().left();
        defaults().left().pad(2);
    }

    void show(PackProject project, LevelDoc doc, int entity) {
        this.project = project;
        this.doc = doc;
        snapshot = false;
        clearChildren();
        if (doc == null) {
            add(new VisLabel("Уровень не выбран"));
            return;
        }
        if (entity < 0 || entity >= doc.entities().size()) {
            showLevel();
        } else {
            showEntity(entity);
        }
    }

    // ------------------------------------------------------------------ уровень

    private void showLevel() {
        heading("Уровень " + doc.id());
        row();
        Map<String, Object> root = doc.root();
        text("Название", () -> root.get("name"), v -> putOrRemove(root, "name", v), "name");
        check("Тёмный (нужен факел)", root, "dark");
        check("Контрольная точка (хардкор)", root, "checkpoint");
        VisTextField width = number(String.valueOf(doc.width()));
        VisTextField height = number(String.valueOf(doc.height()));
        VisTable size = new VisTable();
        size.add(width).width(60);
        size.add(new VisLabel(" × "));
        size.add(height).width(60);
        VisTextButton apply = new VisTextButton("Изменить");
        apply.addListener(
                changed(
                        () -> {
                            int w = parseInt(width.getText(), doc.width());
                            int h = parseInt(height.getText(), doc.height());
                            if (w > 0 && h > 0 && w <= 200 && h <= 200) {
                                doc.checkpoint();
                                doc.resize(w, h, 0, 0);
                                doc.changed();
                                app.levelEdited();
                            }
                        }));
        size.add(apply).padLeft(6);
        labeled("Размер", size);
        VisTable grow = new VisTable();
        grow.defaults().pad(1);
        grow.add(shiftButton("+ столбец слева", 1, 0, 1, 0));
        grow.add(shiftButton("- столбец слева", -1, 0, -1, 0)).row();
        grow.add(shiftButton("+ строка сверху", 0, 1, 0, 1));
        grow.add(shiftButton("- строка сверху", 0, -1, 0, -1));
        add(grow).colspan(2).row();
        textField(
                "Скрипт (.dgs)",
                String.valueOf(root.getOrDefault("script", "")),
                v -> putOrRemove(root, "script", v.isBlank() ? null : v.strip()));
        VisTextButton scripts = new VisTextButton("Редактор скриптов (F6)");
        scripts.addListener(changed(() -> app.openScripts(-1, null)));
        add(scripts).colspan(2).left().row();
        textField(
                "Комментарий",
                String.valueOf(root.getOrDefault("comment", "")),
                v -> putOrRemove(root, "comment", v.isBlank() ? null : v));
        boolean isStart = doc.id().equals(project.pack().get("start"));
        VisTextButton start =
                new VisTextButton(
                        isStart ? "Это первый уровень пака" : "Сделать первым уровнем пака");
        start.setDisabled(isStart);
        start.addListener(
                changed(
                        () -> {
                            project.pack().put("start", doc.id());
                            project.packChanged();
                            app.refreshLevels();
                            app.selectionChanged();
                        }));
        add(start).colspan(2).padTop(8).row();
        hint("Выделите объект на карте или в списке «На карте» — здесь появятся его свойства.");
    }

    private VisTextButton shiftButton(String label, int dw, int dh, int dx, int dy) {
        VisTextButton b = new VisTextButton(label);
        b.addListener(
                changed(
                        () -> {
                            if (doc.width() + dw < 1 || doc.height() + dh < 1) {
                                return;
                            }
                            doc.checkpoint();
                            doc.resize(doc.width() + dw, doc.height() + dh, dx, dy);
                            doc.changed();
                            app.levelEdited();
                        }));
        return b;
    }

    // ------------------------------------------------------------------ сущность

    private void showEntity(int index) {
        Map<String, Object> e = doc.entities().get(index);
        String prefabId = String.valueOf(e.get("prefab"));
        Prefab prefab = app.data().prefabs().get(prefabId);
        heading(prefabId + "  #" + index);
        row();
        if (prefab != null && prefab.description() != null) {
            hint(prefab.description());
        }
        int[] at = LevelDoc.at(e);
        VisTextField x = number(String.valueOf(at[0]));
        VisTextField y = number(String.valueOf(at[1]));
        Runnable move =
                () ->
                        e.put(
                                "at",
                                LevelDoc.point(
                                        parseInt(x.getText(), at[0]),
                                        parseInt(y.getText(), at[1])));
        bind(x, v -> move.run());
        bind(y, v -> move.run());
        VisTable pos = new VisTable();
        pos.add(x).width(60);
        pos.add(new VisLabel(" , "));
        pos.add(y).width(60);
        labeled("Клетка", pos);
        textField(
                "id",
                String.valueOf(e.getOrDefault("id", "")),
                v -> putOrRemove(e, "id", v.isBlank() ? null : v.strip()));
        VisCheckBox enabled = new VisCheckBox("включена", !Boolean.FALSE.equals(e.get("enabled")));
        enabled.addListener(
                changed(
                        () ->
                                edit(
                                        () ->
                                                putOrRemove(
                                                        e,
                                                        "enabled",
                                                        enabled.isChecked() ? null : false))));
        labeled("", enabled);
        if (prefab != null) {
            for (ParamSpec spec : prefab.params().values()) {
                param(e, spec, index);
            }
            for (String event : prefab.events()) {
                String key = Prefab.handlerKey(event);
                script(key + " (DGScript)", e, key, index);
            }
        }
        textField(
                "Комментарий",
                String.valueOf(e.getOrDefault("comment", "")),
                v -> putOrRemove(e, "comment", v.isBlank() ? null : v));
        VisTable actions = new VisTable();
        VisTextButton copy = new VisTextButton("Копия (Ctrl+D)");
        copy.addListener(changed(app::duplicateSelected));
        VisTextButton delete = new VisTextButton("Удалить (Del)");
        delete.addListener(changed(app::deleteSelected));
        actions.add(copy).padRight(6);
        actions.add(delete);
        add(actions).colspan(2).padTop(8).row();
    }

    private void param(Map<String, Object> e, ParamSpec spec, int index) {
        String name = spec.name() + (spec.isRequired() ? " *" : "");
        String fallback =
                spec.defaultValue() == null || spec.defaultValue().isNull()
                        ? ""
                        : spec.defaultValue()
                                .toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
        Object value = e.get(spec.name());
        switch (spec.type()) {
            case INT, NUMBER -> {
                VisTextField f = number(value == null ? "" : numberText(value));
                f.setMessageText(fallback);
                bindValidated(
                        f,
                        v -> {
                            if (v.isBlank()) {
                                e.remove(spec.name());
                                return true;
                            }
                            try {
                                e.put(
                                        spec.name(),
                                        spec.type() == ParamType.INT
                                                ? (Object) Long.parseLong(v.strip())
                                                : (Object) Double.parseDouble(v.strip()));
                                return true;
                            } catch (NumberFormatException ex) {
                                return false;
                            }
                        });
                labeled(name, f, spec);
            }
            case BOOL -> {
                VisSelectBox<String> box = select(List.of(DEFAULT + " " + fallback, "да", "нет"));
                box.setSelectedIndex(value == null ? 0 : Boolean.TRUE.equals(value) ? 1 : 2);
                box.addListener(
                        changed(
                                () ->
                                        edit(
                                                () -> {
                                                    int i = box.getSelectedIndex();
                                                    putOrRemove(
                                                            e, spec.name(), i == 0 ? null : i == 1);
                                                })));
                labeled(name, box, spec);
            }
            case DIRECTION, DIRECTION8 -> {
                List<String> options = new ArrayList<>();
                options.add(DEFAULT + (fallback.isEmpty() ? "" : " " + fallback.replace("\"", "")));
                for (Direction d : Direction.values()) {
                    if (spec.type() == ParamType.DIRECTION8 || d.isOrthogonal()) {
                        options.add(d.id());
                    }
                }
                choice(name, e, spec, options);
            }
            case LEVEL -> {
                List<String> options = new ArrayList<>();
                options.add(NONE);
                options.addAll(project.levelIds());
                choice(name, e, spec, options);
            }
            case ENTITY -> {
                List<String> options = new ArrayList<>();
                options.add(NONE);
                for (Map<String, Object> other : doc.entities()) {
                    if (other.get("id") instanceof String id && other != e) {
                        options.add(id);
                    }
                }
                choice(name, e, spec, options);
            }
            case STRING, COLOR -> {
                if (spec.values() != null && !spec.values().isEmpty()) {
                    List<String> options = new ArrayList<>();
                    options.add(DEFAULT);
                    options.addAll(spec.values());
                    choice(name, e, spec, options);
                } else {
                    VisTextField f = new VisTextField(value == null ? "" : String.valueOf(value));
                    f.setMessageText(fallback.replace("\"", ""));
                    bind(f, v -> putOrRemove(e, spec.name(), v.isEmpty() ? null : v));
                    labeled(name, f, spec);
                }
            }
            case POINT -> {
                VisTextField f = number(value == null ? "" : pointText(value));
                f.setMessageText("x, y");
                bindValidated(
                        f,
                        v -> {
                            if (v.isBlank()) {
                                e.remove(spec.name());
                                return true;
                            }
                            String[] parts = v.split("[,\\s]+");
                            if (parts.length != 2) {
                                return false;
                            }
                            try {
                                e.put(
                                        spec.name(),
                                        LevelDoc.point(
                                                Integer.parseInt(parts[0].strip()),
                                                Integer.parseInt(parts[1].strip())));
                                return true;
                            } catch (NumberFormatException ex) {
                                return false;
                            }
                        });
                labeled(name, f, spec);
            }
            case TEXT ->
                    text(
                            name,
                            () -> e.get(spec.name()),
                            v -> putOrRemove(e, spec.name(), v),
                            entityKey(e, index, spec.name()));
            case ITEM -> stack(name, e, spec);
            case ITEMS, OFFERS -> stackList(name, e, spec);
            default -> json(name, e, spec);
        }
    }

    private String entityKey(Map<String, Object> e, int index, String param) {
        String owner = e.get("id") instanceof String id ? id : e.get("prefab") + "" + index;
        return owner + "." + param;
    }

    // ------------------------------------------------------------------ поля

    private void choice(String name, Map<String, Object> e, ParamSpec spec, List<String> options) {
        VisSelectBox<String> box = select(options);
        Object value = e.get(spec.name());
        int i = value == null ? 0 : options.indexOf(String.valueOf(value));
        if (i < 0) {
            List<String> extended = new ArrayList<>(options);
            extended.add(String.valueOf(value));
            box.setItems(extended.toArray(new String[0]));
            i = extended.size() - 1;
        }
        box.setSelectedIndex(i);
        box.addListener(
                changed(
                        () ->
                                edit(
                                        () ->
                                                putOrRemove(
                                                        e,
                                                        spec.name(),
                                                        box.getSelectedIndex() == 0
                                                                ? null
                                                                : box.getSelected()))));
        labeled(name, box, spec);
    }

    /** Стопка предметов: правится копия, в уровень попадает одной правкой (для отмены). */
    @SuppressWarnings("unchecked")
    private void stack(String name, Map<String, Object> e, ParamSpec spec) {
        Map<String, Object> working =
                e.get(spec.name()) instanceof Map<?, ?> m
                        ? Json.copy((Map<String, Object>) m)
                        : StackEditor.newStack();
        if (!(e.get(spec.name()) instanceof Map)) {
            VisTextButton set = new VisTextButton("Задать предмет");
            set.addListener(
                    changed(
                            () -> {
                                edit(() -> e.put(spec.name(), Json.copy(working)));
                                app.selectionChanged();
                            }));
            labeled(name, set, spec);
            return;
        }
        stacked(
                name,
                new StackEditor(
                        app.data().items(),
                        working,
                        () -> edit(() -> e.put(spec.name(), Json.copy(working)))),
                spec);
    }

    /** Список стопок или товаров магазина. */
    @SuppressWarnings("unchecked")
    private void stackList(String name, Map<String, Object> e, ParamSpec spec) {
        List<Object> working =
                e.get(spec.name()) instanceof List<?> l
                        ? Json.copy((List<Object>) l)
                        : new ArrayList<>();
        stacked(
                name,
                new StackEditor.ListEditor(
                        app.data().items(),
                        working,
                        spec.type() == ParamType.OFFERS,
                        () ->
                                edit(
                                        () ->
                                                putOrRemove(
                                                        e,
                                                        spec.name(),
                                                        working.isEmpty() && !spec.isRequired()
                                                                ? null
                                                                : Json.copy(working)))),
                spec);
    }

    /** Всё сложное без своего редактора — JSON; неверный JSON не применяется (поле краснеет). */
    private void json(String name, Map<String, Object> e, ParamSpec spec) {
        Object value = e.get(spec.name());
        VisTextArea area =
                new VisTextArea(value == null ? "" : JsonPrinter.write(value).strip(), "mono");
        Form.rows(area, value instanceof List<?> l && l.size() > 1 ? Math.min(8, l.size() + 1) : 2);
        area.setMessageText(itemsHint(spec.type()));
        bindValidated(
                area,
                v -> {
                    if (v.isBlank()) {
                        e.remove(spec.name());
                        return true;
                    }
                    try {
                        e.put(spec.name(), Json.parse(v));
                        return true;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                });
        stacked(name, area, spec);
    }

    private String itemsHint(ParamType type) {
        StringBuilder ids = new StringBuilder();
        for (ItemDefinition item : app.data().items().all()) {
            ids.append(ids.length() == 0 ? "" : ", ").append(item.id());
        }
        return switch (type) {
            case ITEM -> "{ \"id\": \"coins\", \"count\": 5 }";
            case ITEMS -> "[{ \"id\": \"potion\" }]";
            case OFFERS -> "[{ \"item\": { \"id\": \"potion\" }, \"price\": 5, \"stock\": 2 }]";
            default -> ids.toString();
        };
    }

    /** Обработчик DGScript: строка или массив строк — по строке на строку поля. */
    private void script(String name, Map<String, Object> e, String key, int index) {
        Object value = e.get(key);
        String text = "";
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object line : list) {
                sb.append(sb.length() == 0 ? "" : "\n").append(line);
            }
            text = sb.toString();
        } else if (value != null) {
            text = String.valueOf(value);
        }
        VisTextArea area = new VisTextArea(text, "mono");
        Form.rows(area, Math.max(2, Math.min(8, text.split("\n").length + 1)));
        area.setMessageText("entity('door1').enable()");
        bind(
                area,
                v -> {
                    String[] lines = v.split("\n");
                    if (v.isBlank()) {
                        e.remove(key);
                    } else if (lines.length == 1) {
                        e.put(key, v.strip());
                    } else {
                        // Отступы и пустые строки сохраняются: номера строк — как в отладчике.
                        List<Object> list = new ArrayList<>();
                        for (String line : lines) {
                            list.add(line.stripTrailing());
                        }
                        while (!list.isEmpty() && ((String) list.get(list.size() - 1)).isEmpty()) {
                            list.remove(list.size() - 1);
                        }
                        e.put(key, list);
                    }
                });
        stacked(name, area, null);
        VisTextButton open = new VisTextButton("В редакторе скриптов (F6)");
        open.addListener(changed(() -> app.openScripts(index, key)));
        add(open).colspan(2).left().row();
    }

    /**
     * Текст для игрока на двух языках. Значение-ключ {@code @…} — тексты в {@code i18n/pack*};
     * обычная строка при первой правке превращается в ключ {@code level.<уровень>.<suffix>}.
     */
    private void text(
            String name,
            java.util.function.Supplier<Object> value,
            Consumer<Object> set,
            String suffix) {
        VisTable box = new VisTable();
        box.defaults().left().pad(1);
        for (String lang : List.of("ru", "en")) {
            String current = textOf(value.get(), lang);
            VisTextArea area = new VisTextArea(current);
            Form.rows(area, Math.max(1, Math.min(5, current.split("\n").length)));
            bind(
                    area,
                    v -> {
                        Object val = value.get();
                        String key;
                        if (val instanceof String s && s.startsWith("@")) {
                            key = s.substring(1);
                        } else {
                            key =
                                    project.freeKey(
                                            "level." + doc.id().replace('/', '.') + "." + suffix);
                            for (String other : PackProject.LANGUAGES.keySet()) {
                                project.text(other, key, textOf(val, other));
                            }
                            set.accept("@" + key);
                        }
                        project.text(lang, key, v);
                    });
            box.add(new VisLabel(lang)).width(22);
            box.add(area).width(FIELD - 26).row();
        }
        Object v = value.get();
        String hintText =
                v instanceof String s && s.startsWith("@") ? s.substring(1) : "новый текст";
        VisLabel key = new VisLabel(hintText, "small");
        key.setColor(Color.GRAY);
        box.add();
        box.add(key).row();
        labeled(name, box);
    }

    private String textOf(Object value, String lang) {
        if (value instanceof String s && s.startsWith("@")) {
            String t = project.text(lang, s.substring(1));
            return t == null ? "" : t;
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object line : list) {
                sb.append(sb.length() == 0 ? "" : "\n").append(line);
            }
            return sb.toString();
        }
        return value == null ? "" : String.valueOf(value);
    }

    private void check(String label, Map<String, Object> root, String key) {
        VisCheckBox box = new VisCheckBox(label, Boolean.TRUE.equals(root.get(key)));
        box.addListener(
                changed(() -> edit(() -> putOrRemove(root, key, box.isChecked() ? true : null))));
        add(box).colspan(2).row();
    }

    private void textField(String label, String value, Consumer<String> set) {
        VisTextField f = new VisTextField(value);
        bind(f, set);
        labeled(label, f);
    }

    private VisTextField number(String text) {
        return new VisTextField(text);
    }

    private <T> VisSelectBox<T> select(List<T> items) {
        VisSelectBox<T> box = new VisSelectBox<>();
        Array<T> array = new Array<>();
        for (T item : items) {
            array.add(item);
        }
        box.setItems(array);
        return box;
    }

    /** Поле применяется при каждом изменении; одна правка для отмены, пока в поле фокус. */
    private void bind(VisTextField field, Consumer<String> set) {
        bindValidated(
                field,
                v -> {
                    set.accept(v);
                    return true;
                });
    }

    private void bindValidated(VisTextField field, java.util.function.Predicate<String> set) {
        field.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        edit(() -> field.setInputValid(set.test(field.getText())));
                    }
                });
        field.addListener(
                new FocusListener() {
                    @Override
                    public void keyboardFocusChanged(
                            FocusEvent event, Actor actor, boolean focused) {
                        if (!focused) {
                            snapshot = false;
                        }
                    }
                });
    }

    /** Правка документа: снимок для отмены — один на поле. */
    private void edit(Runnable change) {
        if (!snapshot) {
            doc.checkpoint();
            snapshot = true;
        }
        change.run();
        doc.changed();
        app.levelEdited();
    }

    private static void putOrRemove(Map<String, Object> map, String key, Object value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    // ------------------------------------------------------------------ раскладка

    private void heading(String text) {
        VisLabel label = new VisLabel(text);
        label.setColor(new Color(1, 0.85f, 0.4f, 1));
        add(label).colspan(2).padBottom(4);
    }

    private void hint(String text) {
        VisLabel label = new VisLabel(text, "small");
        label.setWrap(true);
        label.setColor(Color.LIGHT_GRAY);
        add(label).colspan(2).width(FIELD + LABEL).padBottom(4).row();
    }

    private void labeled(String name, Actor field) {
        VisLabel label = new VisLabel(name);
        label.setAlignment(Align.left);
        add(label).width(LABEL).top().padTop(4);
        add(field).width(FIELD).row();
    }

    private void labeled(String name, Actor field, ParamSpec spec) {
        labeled(name, field);
        if (spec != null && spec.description() != null) {
            add();
            VisLabel d = new VisLabel(spec.description(), "small");
            d.setWrap(true);
            d.setColor(Color.GRAY);
            add(d).width(FIELD).row();
        }
    }

    private void stacked(String name, Actor field, ParamSpec spec) {
        add(new VisLabel(name)).colspan(2).padTop(4).row();
        add(field).colspan(2).width(FIELD + LABEL).row();
        if (spec != null && spec.description() != null) {
            VisLabel d = new VisLabel(spec.description(), "small");
            d.setWrap(true);
            d.setColor(Color.GRAY);
            add(d).colspan(2).width(FIELD + LABEL).row();
        }
    }

    private static ChangeListener changed(Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String numberText(Object value) {
        if (value instanceof Double d && d == Math.rint(d)) {
            return d.longValue() + ".0";
        }
        return String.valueOf(value);
    }

    private static String pointText(Object value) {
        if (value instanceof List<?> l && l.size() == 2) {
            return l.get(0) + ", " + l.get(1);
        }
        return String.valueOf(value);
    }
}
