package com.zaig100.dargon.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.zaig100.dargon.editor.model.Json;
import com.zaig100.dargon.editor.model.PackProject;
import com.zaig100.dargon.level.ComponentCatalog;
import com.zaig100.dargon.level.GameData;
import com.zaig100.dargon.level.LevelFormatException;
import com.zaig100.dargon.level.ParamSpec;
import com.zaig100.dargon.level.ParamType;
import com.zaig100.dargon.level.Prefab;
import com.zaig100.dargon.level.PrefabLibrary;
import com.zaig100.dargon.level.Problems;
import com.zaig100.dargon.render.GameAssets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Свои префабы пака: список слева, форма справа — параметры (что задаётся в уровне) и компоненты
 * движка с полями; поле — значение или ссылка на параметр. Ошибки видны сразу.
 */
final class PrefabsPanel extends VisTable {

    private static final float FORM = 640;
    private static final List<String> PLACEMENTS = List.of("floor", "wall", "any");
    private static final List<String> PLACEMENT_LABELS =
            List.of("на полу", "на стене или за краем карты", "где угодно");

    private final GameData engine;
    private final GameAssets assets;
    private final PackProject project;
    private final Runnable changed;

    /** Черновик: id → JSON префаба; переименования — для уровней. */
    private final Map<String, Map<String, Object>> draft = new LinkedHashMap<>();

    private final Map<String, String> renames = new LinkedHashMap<>();
    private final List<String> ids = new ArrayList<>();
    private final VisList<String> list = new VisList<>();
    private final VisTable form = new VisTable();
    private final VisLabel error = new VisLabel("", "small");
    private String current;
    private boolean ignore;

    PrefabsPanel(GameData engine, GameAssets assets, PackProject project, Runnable changed) {
        this.engine = engine;
        this.assets = assets;
        this.project = project;
        this.changed = changed;
        for (Map.Entry<String, Map<String, Object>> e : project.prefabs().entrySet()) {
            draft.put(e.getKey(), Json.copy(e.getValue()));
        }
        top().left();
        VisTable left = new VisTable();
        left.top().left();
        left.add(Form.wheel(new VisScrollPane(list))).width(200).height(330).row();
        left.add(button("Новый", this::newPrefab)).left().row();
        List<Form.Option> game = new ArrayList<>();
        for (String id : new java.util.TreeSet<>(engine.prefabs().ids())) {
            if (!draft.containsKey(id)) {
                game.add(new Form.Option(id, id));
            }
        }
        String[] base = {game.isEmpty() ? null : game.get(0).value()};
        VisSelectBox<Form.Option> from = Form.select(game, base[0], v -> base[0] = v);
        left.add(new VisLabel("На основе игрового:")).padTop(6).row();
        left.add(from).width(200).row();
        left.add(button("Скопировать", () -> copyGame(base[0]))).left().row();
        left.add(button("Удалить", this::delete)).left().padTop(6).row();
        add(left).top().padRight(10);
        form.top().left();
        form.defaults().left().pad(2);
        VisTable right = new VisTable();
        right.add(Form.wheel(scroll(form))).width(FORM + 30).height(430).row();
        error.setWrap(true);
        error.setColor(new Color(1, 0.45f, 0.45f, 1));
        right.add(error).width(FORM + 30).row();
        add(right).top();
        list.addListener(
                Form.changed(
                        () -> {
                            if (!ignore && list.getSelectedIndex() >= 0) {
                                show(ids.get(list.getSelectedIndex()));
                            }
                        }));
        refreshList();
        show(ids.isEmpty() ? null : ids.get(0));
    }

    /** Черновик для «Применить»: префабы и переименования. */
    Map<String, Map<String, Object>> draft() {
        return draft;
    }

    Map<String, String> renames() {
        return renames;
    }

    /** Ошибки всех префабов (пустая строка — всё хорошо). */
    String validateAll() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Object>> e : draft.entrySet()) {
            String problem = validate(e.getKey(), e.getValue());
            if (problem != null) {
                sb.append(sb.length() == 0 ? "" : "\n").append(problem);
            }
        }
        return sb.toString();
    }

    private String validate(String id, Map<String, Object> prefab) {
        String path = PackProject.prefabPath(id);
        try {
            if (engine.prefabs().get(id) != null && !engine.packPrefabs().contains(id)) {
                return path + ": id '" + id + "' уже есть в игре";
            }
            Prefab parsed =
                    new PrefabLibrary(engine.items()).add(PackProject.prefabJson(prefab), path);
            Problems problems = new Problems(path);
            engine.components().validate(parsed, path, problems, engine.items());
            problems.throwIfErrors();
            return null;
        } catch (LevelFormatException e) {
            return e.getMessage();
        }
    }

    private void edited() {
        String problem = current == null ? null : validate(current, draft.get(current));
        error.setText(problem == null ? "" : problem);
        changed.run();
    }

    // ------------------------------------------------------------------ список

    private void refreshList() {
        ignore = true;
        ids.clear();
        ids.addAll(draft.keySet());
        Array<String> items = new Array<>();
        for (String id : ids) {
            Object d = draft.get(id).get("description");
            items.add(id + (d == null ? "" : " — " + String.valueOf(d).split("[:.(]", 2)[0]));
        }
        list.setItems(items);
        if (current != null && ids.contains(current)) {
            list.setSelectedIndex(ids.indexOf(current));
        }
        ignore = false;
    }

    private String freeId(String base) {
        String id = base;
        for (int i = 2;
                draft.containsKey(id)
                        || engine.prefabs().get(id) != null && !engine.packPrefabs().contains(id);
                i++) {
            id = base + "_" + i;
        }
        return id;
    }

    private void newPrefab() {
        String id = freeId("my_object");
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", id);
        p.put("description", "Новый объект");
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("SpriteRef", new LinkedHashMap<>(Map.of("region", "x")));
        p.put("components", components);
        draft.put(id, p);
        refreshList();
        show(id);
        edited();
    }

    private void copyGame(String base) {
        if (base == null) {
            return;
        }
        FileHandle file = Gdx.files.internal(GameData.PREFABS_DIR + base + ".json");
        Map<String, Object> p = Json.parseObject(file.readString("UTF-8"));
        p.remove("$schema");
        String id = freeId(base + "_custom");
        p.put("id", id);
        draft.put(id, p);
        refreshList();
        show(id);
        edited();
    }

    private void delete() {
        if (current == null) {
            return;
        }
        draft.remove(current);
        current = ids.size() > 1 ? ids.get(0).equals(current) ? ids.get(1) : ids.get(0) : null;
        refreshList();
        show(current);
        edited();
    }

    // ------------------------------------------------------------------ форма

    private void show(String id) {
        current = id;
        form.clearChildren();
        if (id == null) {
            form.add(
                    Form.hint(
                            "Своих префабов пока нет. «Новый» — с нуля, «Скопировать» — на основе"
                                    + " игрового (например, другие шипы или своя ловушка).",
                            FORM));
            error.setText("");
            return;
        }
        Map<String, Object> p = draft.get(id);
        VisTable head = new VisTable();
        head.left();
        head.add(new SpritePreview(assets, p)).size(48, 48).padRight(8);
        head.add(Form.heading(id));
        form.add(head).colspan(2).row();
        row(
                "id",
                Form.validated(
                        id,
                        v -> {
                            String next = v.strip();
                            if (next.equals(current)) {
                                return true;
                            }
                            if (!next.matches("[a-z][a-z0-9_]*")
                                    || draft.containsKey(next)
                                    || engine.prefabs().get(next) != null
                                            && !engine.packPrefabs().contains(next)) {
                                return false;
                            }
                            rename(current, next);
                            return true;
                        }));
        VisTextArea description =
                new VisTextArea(String.valueOf(p.getOrDefault("description", "")));
        Form.rows(description, 2);
        description.addListener(
                Form.changed(
                        () -> {
                            p.put("description", description.getText());
                            refreshList();
                            edited();
                        }));
        row("Описание", description);
        row(
                "Где ставится",
                Form.select(
                        Form.options(PLACEMENTS, PLACEMENT_LABELS),
                        String.valueOf(p.getOrDefault("placement", "floor")),
                        v -> {
                            if (v.equals("floor")) {
                                p.remove("placement");
                            } else {
                                p.put("placement", v);
                            }
                            edited();
                        }));
        VisTable events = new VisTable();
        for (String event : engine.components().events()) {
            List<?> cur = p.get("events") instanceof List<?> l ? l : List.of();
            events.add(
                            Form.check(
                                    event
                                            + " (в уровне: on"
                                            + Character.toUpperCase(event.charAt(0))
                                            + event.substring(1)
                                            + ")",
                                    cur.contains(event),
                                    b -> {
                                        toggle(p, "events", event, b);
                                        edited();
                                    }))
                    .padRight(8);
        }
        row("События DGScript", events);
        hint(
                "С событием в уровне у объекта появляется обработчик (onUse) — нужен ещё компонент ScriptHandlers.");
        params(p);
        components(p);
        edited();
    }

    private void rename(String old, String next) {
        Map<String, Object> p = draft.remove(old);
        p.put("id", next);
        Map<String, Map<String, Object>> reordered = new LinkedHashMap<>();
        for (String id : ids) {
            reordered.put(id.equals(old) ? next : id, id.equals(old) ? p : draft.get(id));
        }
        draft.clear();
        draft.putAll(reordered);
        // Цепочка a→b→c сводится к a→c.
        String origin = old;
        for (Map.Entry<String, String> e : renames.entrySet()) {
            if (e.getValue().equals(old)) {
                origin = e.getKey();
            }
        }
        renames.remove(origin);
        if (project.prefabs().containsKey(origin)) {
            renames.put(origin, next);
        }
        current = next;
        refreshList();
        edited();
    }

    // ------------------------------------------------------------------ параметры

    @SuppressWarnings("unchecked")
    private void params(Map<String, Object> p) {
        form.add(Form.heading("Параметры (задаются в уровне)")).colspan(2).padTop(8).row();
        Map<String, Object> params =
                p.get("params") instanceof Map<?, ?> m
                        ? (Map<String, Object>) m
                        : new LinkedHashMap<>();
        for (String name : new ArrayList<>(params.keySet())) {
            Map<String, Object> spec = (Map<String, Object>) params.get(name);
            VisTable line = new VisTable();
            line.left();
            line.defaults().padRight(4);
            line.add(
                            Form.validated(
                                    name,
                                    v -> {
                                        String next = v.strip();
                                        if (next.equals(name)) {
                                            return true;
                                        }
                                        if (!next.matches("[a-z][a-zA-Z0-9]*")
                                                || params.containsKey(next)) {
                                            return false;
                                        }
                                        renameParam(p, params, name, next);
                                        return true;
                                    }))
                    .width(110);
            line.add(
                            Form.select(
                                    Form.options(ValueField.typeIds(), ValueField.TYPE_LABELS),
                                    String.valueOf(spec.getOrDefault("type", "int")),
                                    v -> {
                                        spec.put("type", v);
                                        spec.remove("default");
                                        spec.remove("values");
                                        spec.remove("min");
                                        spec.remove("max");
                                        show(current);
                                    }))
                    .width(150);
            line.add(
                    button(
                            "×",
                            () -> {
                                params.remove(name);
                                if (params.isEmpty()) {
                                    p.remove("params");
                                }
                                show(current);
                            }));
            form.add(line).colspan(2).left().row();
            ParamType type = ParamType.byId(String.valueOf(spec.get("type")));
            List<String> values = spec.get("values") instanceof List<?> l ? (List<String>) l : null;
            String mode =
                    !spec.containsKey("default")
                            ? "required"
                            : spec.get("default") == null ? "optional" : "value";
            VisTable def = new VisTable();
            def.left();
            def.add(
                            Form.select(
                                    Form.options(
                                            List.of("required", "value", "optional"),
                                            List.of(
                                                    "обязательный",
                                                    "по умолчанию:",
                                                    "необязательный")),
                                    mode,
                                    v -> {
                                        if (v.equals("required")) {
                                            spec.remove("default");
                                        } else if (v.equals("optional")) {
                                            spec.put("default", null);
                                        } else if (type != null) {
                                            spec.put("default", ValueField.initial(type, values));
                                        }
                                        show(current);
                                    }))
                    .padRight(4);
            if (mode.equals("value") && type != null) {
                def.add(
                        ValueField.of(
                                type,
                                values,
                                spec.get("default"),
                                engine.items(),
                                v -> {
                                    spec.put("default", v);
                                    edited();
                                }));
            }
            row("  значение", def);
            if (type == ParamType.STRING) {
                row(
                        "  варианты",
                        Form.text(
                                values == null ? "" : String.join(", ", values),
                                v -> {
                                    List<Object> list = new ArrayList<>();
                                    for (String part : v.split(",")) {
                                        if (!part.isBlank()) {
                                            list.add(part.strip());
                                        }
                                    }
                                    if (list.isEmpty()) {
                                        spec.remove("values");
                                    } else {
                                        spec.put("values", list);
                                    }
                                    edited();
                                }));
            }
            if (type == ParamType.INT || type == ParamType.NUMBER) {
                VisTable range = new VisTable();
                range.add(new VisLabel("от "));
                range.add(
                                Form.number(
                                        spec.get("min"),
                                        v -> {
                                            put(spec, "min", v);
                                            edited();
                                        }))
                        .width(60);
                range.add(new VisLabel(" до "));
                range.add(
                                Form.number(
                                        spec.get("max"),
                                        v -> {
                                            put(spec, "max", v);
                                            edited();
                                        }))
                        .width(60);
                row("  пределы", range);
            }
            row(
                    "  описание",
                    Form.text(
                            String.valueOf(spec.getOrDefault("description", "")),
                            v -> {
                                put(spec, "description", v.isBlank() ? null : v);
                                edited();
                            }));
        }
        form.add(
                        button(
                                "Добавить параметр",
                                () -> {
                                    String name = "param";
                                    for (int i = 2; params.containsKey(name); i++) {
                                        name = "param" + i;
                                    }
                                    Map<String, Object> spec = new LinkedHashMap<>();
                                    spec.put("type", "int");
                                    spec.put("default", 1L);
                                    params.put(name, spec);
                                    p.put("params", params);
                                    show(current);
                                }))
                .colspan(2)
                .row();
    }

    /** Переименование параметра: ссылки {@code $старое} в компонентах — тоже. */
    @SuppressWarnings("unchecked")
    private void renameParam(
            Map<String, Object> p, Map<String, Object> params, String old, String next) {
        Map<String, Object> reordered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            reordered.put(e.getKey().equals(old) ? next : e.getKey(), e.getValue());
        }
        params.clear();
        params.putAll(reordered);
        if (p.get("components") instanceof Map<?, ?> components) {
            for (Object data : components.values()) {
                if (data instanceof Map<?, ?> fields) {
                    for (Map.Entry<String, Object> f : ((Map<String, Object>) fields).entrySet()) {
                        if (("$" + old).equals(f.getValue())) {
                            f.setValue("$" + next);
                        }
                    }
                }
            }
        }
        edited();
    }

    // ------------------------------------------------------------------ компоненты

    @SuppressWarnings("unchecked")
    private void components(Map<String, Object> p) {
        form.add(Form.heading("Компоненты (из чего собран объект)")).colspan(2).padTop(8).row();
        Map<String, Object> components =
                (Map<String, Object>) p.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> params =
                p.get("params") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        for (String name : new ArrayList<>(components.keySet())) {
            ComponentCatalog.Component component = engine.components().get(name);
            VisTable title = new VisTable();
            title.left();
            title.add(new VisLabel(name)).padRight(6);
            title.add(
                    button(
                            "×",
                            () -> {
                                components.remove(name);
                                show(current);
                            }));
            form.add(title).colspan(2).left().padTop(4).row();
            if (component == null) {
                hint("Неизвестный компонент — удалите его.");
                continue;
            }
            hint(component.description());
            Map<String, Object> fields =
                    components.get(name) instanceof Map<?, ?> f
                            ? (Map<String, Object>) f
                            : new LinkedHashMap<>();
            components.put(name, fields);
            for (ParamSpec spec : component.fields().values()) {
                field(name, fields, spec, params);
            }
        }
        List<Form.Option> available = new ArrayList<>();
        available.add(new Form.Option(null, "(выберите компонент)"));
        for (ComponentCatalog.Component c : engine.components().all().values()) {
            if (!components.containsKey(c.name())) {
                available.add(
                        new Form.Option(
                                c.name(), c.name() + " — " + c.description().split("[:.(]", 2)[0]));
            }
        }
        row(
                "Добавить",
                Form.select(
                        available,
                        null,
                        v -> {
                            if (v == null) {
                                return;
                            }
                            Map<String, Object> fields = new LinkedHashMap<>();
                            for (ParamSpec spec : engine.components().get(v).fields().values()) {
                                if (spec.isRequired()) {
                                    fields.put(
                                            spec.name(),
                                            ValueField.initial(spec.type(), spec.values()));
                                }
                            }
                            components.put(v, fields);
                            show(current);
                        }));
    }

    /** Поле компонента: не задано / значение / из параметра. */
    private void field(
            String component,
            Map<String, Object> fields,
            ParamSpec spec,
            Map<String, Object> params) {
        Object value = fields.get(spec.name());
        boolean ref = value instanceof String s && s.startsWith(Prefab.PARAM_REF);
        String mode = value == null ? "none" : ref ? "param" : "value";
        VisTable line = new VisTable();
        line.left();
        line.add(
                        Form.select(
                                Form.options(
                                        List.of("none", "value", "param"),
                                        List.of(
                                                spec.isRequired() ? "— (нужно!)" : "по умолчанию",
                                                "значение",
                                                "из параметра")),
                                mode,
                                v -> {
                                    switch (v) {
                                        case "none" -> fields.remove(spec.name());
                                        case "value" ->
                                                fields.put(
                                                        spec.name(),
                                                        ValueField.initial(
                                                                spec.type(), spec.values()));
                                        default ->
                                                fields.put(
                                                        spec.name(),
                                                        "$"
                                                                + (params.isEmpty()
                                                                        ? spec.name()
                                                                        : params.keySet()
                                                                                .iterator()
                                                                                .next()));
                                    }
                                    show(current);
                                }))
                .padRight(4);
        if (mode.equals("param")) {
            List<Form.Option> options = new ArrayList<>();
            for (String name : params.keySet()) {
                options.add(new Form.Option("$" + name, name));
            }
            line.add(
                    Form.select(
                            options,
                            String.valueOf(value),
                            v -> {
                                fields.put(spec.name(), v);
                                edited();
                            }));
        } else if (mode.equals("value")) {
            if (component.equals("SpriteRef") && spec.name().equals("region")) {
                line.add(regionSelect(fields));
            } else {
                line.add(
                        ValueField.of(
                                spec.type(),
                                spec.values(),
                                value,
                                engine.items(),
                                v -> {
                                    fields.put(spec.name(), v);
                                    edited();
                                }));
            }
        }
        String label = "  " + spec.name() + (spec.isRequired() ? " *" : "");
        row(label, line);
        if (!spec.description().isEmpty()) {
            hint(spec.description());
        }
    }

    /** Картинка: текстуры пака, затем игровые. */
    private Actor regionSelect(Map<String, Object> fields) {
        List<Form.Option> options = new ArrayList<>();
        for (String name : project.textures()) {
            options.add(new Form.Option(name, name + " (пак)"));
        }
        for (String name : assets.atlasNames()) {
            if (!project.textures().contains(name)) {
                options.add(new Form.Option(name, name));
            }
        }
        return Form.select(
                options,
                fields.get("region") == null ? null : String.valueOf(fields.get("region")),
                v -> {
                    fields.put("region", v);
                    show(current);
                });
    }

    // ------------------------------------------------------------------ мелочи

    @SuppressWarnings("unchecked")
    private static void toggle(Map<String, Object> map, String key, String value, boolean on) {
        List<Object> list =
                map.get(key) instanceof List<?> l ? (List<Object>) l : new ArrayList<>();
        list.remove(value);
        if (on) {
            list.add(value);
        }
        if (list.isEmpty()) {
            map.remove(key);
        } else {
            map.put(key, list);
        }
    }

    private static void put(Map<String, Object> map, String key, Object value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    private void row(String label, Actor field) {
        form.add(new VisLabel(label)).width(150).top().padTop(4);
        if (field instanceof VisTextField || field instanceof VisSelectBox) {
            form.add(field).left().width(FORM - 170).row();
        } else {
            form.add(field).left().row();
        }
    }

    private void hint(String text) {
        form.add();
        form.add(Form.hint(text, FORM - 160)).width(FORM - 160).row();
    }

    private static VisScrollPane scroll(Actor content) {
        VisScrollPane s = new VisScrollPane(content);
        s.setFadeScrollBars(false);
        s.setScrollingDisabled(true, false);
        return s;
    }

    private static VisTextButton button(String text, Runnable action) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(Form.changed(action));
        return b;
    }

    /** Картинка префаба (SpriteRef.region, кадр frame) крупно; ссылка на параметр — «?». */
    static final class SpritePreview extends Actor {
        private final GameAssets assets;
        private final Map<String, Object> prefab;

        SpritePreview(GameAssets assets, Map<String, Object> prefab) {
            this.assets = assets;
            this.prefab = prefab;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            TextureRegion region = region();
            if (region != null) {
                float scale =
                        Math.min(
                                getWidth() / region.getRegionWidth(),
                                getHeight() / region.getRegionHeight());
                batch.setColor(1, 1, 1, parentAlpha);
                batch.draw(
                        region,
                        getX(),
                        getY(),
                        region.getRegionWidth() * scale,
                        region.getRegionHeight() * scale);
            }
        }

        @SuppressWarnings("unchecked")
        private TextureRegion region() {
            if (!(prefab.get("components") instanceof Map<?, ?> c)
                    || !(c.get("SpriteRef") instanceof Map<?, ?> s)) {
                return null;
            }
            Object name = ((Map<String, Object>) s).get("region");
            Object frame = ((Map<String, Object>) s).get("frame");
            if (!(name instanceof String n) || n.startsWith("$")) {
                return assets.region("x");
            }
            return assets.frame(n, frame instanceof Number f ? f.intValue() : 0);
        }
    }
}
