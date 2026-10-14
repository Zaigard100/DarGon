package com.zaig100.dargon.editor;

import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.zaig100.dargon.editor.model.Json;
import com.zaig100.dargon.editor.model.PackDraft;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.PackDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Достижения пака: список слева, форма справа. Условие выбирается по типу — показываются только его
 * поля; тексты — сразу на двух языках (ключи {@code ach.<id>.name/desc}).
 */
final class AchievementsPanel extends VisTable {

    private static final List<String> TYPE_LABELS =
            List.of(
                    "Пройти уровень",
                    "Пройти игру с начала",
                    "Счётчик за всё время",
                    "Погибнуть от…",
                    "Из скрипта уровня");
    private static final List<String> DIFFICULTY_LABELS =
            List.of("лёгкое", "среднее", "трудное", "хардкор");
    private static final List<String> RESTRICTION_LABELS =
            List.of(
                    "без урона",
                    "без зелий здоровья",
                    "без факела",
                    "без зелья зоркости",
                    "не сломав щит",
                    "без покупок",
                    "без смертей");
    private static final List<String> COUNTER_LABELS =
            List.of("смертей", "монет собрано", "покупок", "уровней пройдено");
    private static final List<String> MODE_LABELS = List.of("казуал", "обычный", "хардкор");

    /** Чем может погибнуть игрок (вид урона последнего удара). */
    static final List<String> DEATH_KINDS =
            List.of("arrow", "fire", "spikes", "trap", "fall", "explosion", "zone");

    private static final List<String> DEATH_LABELS =
            List.of(
                    "стрела",
                    "огонь",
                    "шипы",
                    "скрытая ловушка",
                    "падение (хрупкий пол)",
                    "взрыв (камикадзе)",
                    "опасная зона");

    private static final float FORM = 560;

    /** Виды смерти: игровые и свои — из урона ({@code Hazard.kind}) префабов пака. */
    @SuppressWarnings("unchecked")
    private List<Form.Option> deathOptions() {
        List<String> values = new ArrayList<>(DEATH_KINDS);
        List<String> labels = new ArrayList<>(DEATH_LABELS);
        for (Map.Entry<String, Map<String, Object>> prefab : draft.project().prefabs().entrySet()) {
            if (prefab.getValue().get("components") instanceof Map<?, ?> components
                    && components.get("Hazard") instanceof Map<?, ?> hazard
                    && hazard.get("kind") instanceof String kind
                    && !values.contains(kind)) {
                values.add(kind);
                labels.add(kind + " (префаб " + prefab.getKey() + ")");
            }
        }
        return Form.options(values, labels);
    }

    private final PackDraft draft;
    private final ItemCatalog items;
    private final Runnable changed;
    private final List<Map<String, Object>> list;
    private final VisList<String> names = new VisList<>();
    private final VisTable form = new VisTable();
    private int current = -1;
    private boolean ignore;

    @SuppressWarnings("unchecked")
    AchievementsPanel(PackDraft draft, ItemCatalog items, Runnable changed) {
        this.draft = draft;
        this.items = items;
        this.changed = changed;
        this.list =
                (List<Map<String, Object>>)
                        (List<?>)
                                draft.pack()
                                        .computeIfAbsent("achievements", k -> new ArrayList<>());
        top().left();
        VisTable left = new VisTable();
        left.top().left();
        VisScrollPane scroll = Form.wheel(new VisScrollPane(names));
        scroll.setFadeScrollBars(false);
        left.add(scroll).width(260).height(420).row();
        VisTable buttons = new VisTable();
        buttons.defaults().pad(1);
        buttons.add(button("Новое", this::addAchievement));
        buttons.add(button("Копия", this::duplicate));
        buttons.add(button("Удалить", this::removeAchievement)).row();
        buttons.add(button("Выше", () -> move(-1)));
        buttons.add(button("Ниже", () -> move(1)));
        left.add(buttons).left().row();
        add(left).top().padRight(10);
        form.top().left();
        form.defaults().left().pad(2);
        VisScrollPane formScroll = Form.wheel(new VisScrollPane(form));
        formScroll.setFadeScrollBars(false);
        formScroll.setScrollingDisabled(true, false);
        add(formScroll).width(FORM + 30).height(460).top();
        names.addListener(
                Form.changed(
                        () -> {
                            if (!ignore) {
                                show(names.getSelectedIndex());
                            }
                        }));
        refreshNames();
        show(list.isEmpty() ? -1 : 0);
    }

    // ------------------------------------------------------------------ список

    private void refreshNames() {
        ignore = true;
        Array<String> items = new Array<>();
        for (Map<String, Object> a : list) {
            String name = draft.resolve(a.get("name"), "ru");
            items.add(a.get("id") + (name.isEmpty() ? "" : " — " + name));
        }
        names.setItems(items);
        if (current >= 0 && current < list.size()) {
            names.setSelectedIndex(current);
        }
        ignore = false;
    }

    private void addAchievement() {
        String id = freeId("new");
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", id);
        a.put("name", "@ach." + id + ".name");
        a.put("description", "@ach." + id + ".desc");
        a.put("difficulty", "easy");
        Map<String, Object> when = new LinkedHashMap<>();
        when.put("type", "level");
        a.put("when", when);
        for (String lang : List.of("ru", "en")) {
            draft.text(
                    lang,
                    "ach." + id + ".name",
                    lang.equals("ru") ? "Новое достижение" : "New achievement");
            draft.text(lang, "ach." + id + ".desc", "");
        }
        list.add(current < 0 ? list.size() : current + 1, a);
        current = current < 0 ? list.size() - 1 : current + 1;
        refreshNames();
        show(current);
        changed.run();
    }

    private void duplicate() {
        if (current < 0) {
            return;
        }
        Map<String, Object> copy = Json.copy(list.get(current));
        String id = freeId(String.valueOf(copy.get("id")));
        copy.put("id", id);
        for (String field : List.of("name", "description")) {
            String suffix = field.equals("name") ? "name" : "desc";
            String key = "ach." + id + "." + suffix;
            for (String lang : List.of("ru", "en")) {
                draft.text(lang, key, draft.resolve(copy.get(field), lang));
            }
            copy.put(field, "@" + key);
        }
        list.add(current + 1, copy);
        current++;
        refreshNames();
        show(current);
        changed.run();
    }

    private void removeAchievement() {
        if (current < 0) {
            return;
        }
        list.remove(current);
        current = Math.min(current, list.size() - 1);
        refreshNames();
        show(current);
        changed.run();
    }

    private void move(int delta) {
        int j = current + delta;
        if (current < 0 || j < 0 || j >= list.size()) {
            return;
        }
        Map<String, Object> a = list.remove(current);
        list.add(j, a);
        current = j;
        refreshNames();
        changed.run();
    }

    private String freeId(String base) {
        String id = base;
        for (int i = 2; idTaken(id, -1); i++) {
            id = base + "_" + i;
        }
        return id;
    }

    private boolean idTaken(String id, int except) {
        for (int i = 0; i < list.size(); i++) {
            if (i != except && id.equals(list.get(i).get("id"))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ форма

    private void show(int index) {
        current = index;
        form.clearChildren();
        if (index < 0 || index >= list.size()) {
            form.add(new VisLabel("Достижений пока нет — «Новое» добавит первое."));
            return;
        }
        Map<String, Object> a = list.get(index);
        int self = index;
        VisTextField id =
                Form.validated(
                        String.valueOf(a.get("id")),
                        v -> {
                            String next = v.strip();
                            if (!next.matches("[a-z][a-z0-9_]*") || idTaken(next, self)) {
                                return false;
                            }
                            rename(a, next);
                            return true;
                        });
        row("id", id);
        for (String lang : List.of("ru", "en")) {
            VisTextField name =
                    Form.text(
                            draft.resolve(a.get("name"), lang),
                            v -> {
                                draft.setText(a, "name", "ach." + a.get("id") + ".name", lang, v);
                                refreshNames();
                                changed.run();
                            });
            row("Название (" + lang + ")", name);
        }
        for (String lang : List.of("ru", "en")) {
            VisTextArea desc = new VisTextArea(draft.resolve(a.get("description"), lang));
            Form.rows(desc, 2);
            desc.addListener(
                    Form.changed(
                            () -> {
                                draft.setText(
                                        a,
                                        "description",
                                        "ach." + a.get("id") + ".desc",
                                        lang,
                                        desc.getText());
                                changed.run();
                            }));
            row("Описание (" + lang + ")", desc);
        }
        row(
                "Сложность",
                Form.select(
                        Form.options(PackDefinition.DIFFICULTIES, DIFFICULTY_LABELS),
                        String.valueOf(a.getOrDefault("difficulty", "medium")),
                        v -> {
                            a.put("difficulty", v);
                            changed.run();
                        }));
        row(
                "",
                Form.check(
                        "скрытое — до получения в списке видно «???»",
                        Boolean.TRUE.equals(a.get("hidden")),
                        b -> {
                            put(a, "hidden", b ? true : null);
                            changed.run();
                        }));
        condition(a);
        reward(a);
    }

    /** Новый id: тексты переезжают на новые ключи. */
    private void rename(Map<String, Object> a, String next) {
        String old = String.valueOf(a.get("id"));
        if (old.equals(next)) {
            return;
        }
        a.put("id", next);
        for (String field : List.of("name", "description")) {
            String suffix = field.equals("name") ? "name" : "desc";
            String oldKey = "ach." + old + "." + suffix;
            if (("@" + oldKey).equals(a.get(field))) {
                String newKey = "ach." + next + "." + suffix;
                draft.moveText(oldKey, newKey);
                a.put(field, "@" + newKey);
            }
        }
        refreshNames();
        changed.run();
    }

    @SuppressWarnings("unchecked")
    private void condition(Map<String, Object> a) {
        Map<String, Object> when =
                (Map<String, Object>) a.computeIfAbsent("when", k -> new LinkedHashMap<>());
        String type = String.valueOf(when.getOrDefault("type", "level"));
        form.add(Form.heading("Условие")).colspan(2).padTop(8).row();
        row(
                "Тип",
                Form.select(
                        Form.options(PackDefinition.CONDITIONS, TYPE_LABELS),
                        type,
                        v -> {
                            Object modes = when.get("modes");
                            when.clear();
                            when.put("type", v);
                            if (modes != null && !v.equals("count")) {
                                when.put("modes", modes);
                            }
                            show(current);
                            changed.run();
                        }));
        List<Form.Option> anyLevel = levels("(любой уровень)");
        switch (type) {
            case "level" -> {
                row("Уровень", levelSelect(when, "level", anyLevel));
                row(
                        "Отрезок: с уровня",
                        levelSelect(when, "since", levels("(нет — только этот уровень)")));
                hint(
                        "С отрезком нужно пройти все уровни от «с уровня» до «Уровень» подряд, с"
                                + " соблюдением ограничений; смерть или нарушение — отрезок заново.");
                restrictions(when);
            }
            case "game" -> {
                VisTextField minutes =
                        Form.number(
                                when.get("maxTime") instanceof Number n
                                        ? n.doubleValue() / 60
                                        : null,
                                m -> {
                                    put(
                                            when,
                                            "maxTime",
                                            m == null ? null : Math.round(m * 60) * 1.0);
                                    changed.run();
                                });
                row("Быстрее, минут", minutes);
                row(
                        "Смертей не больше",
                        Form.integer(
                                when.get("maxDeaths"),
                                0,
                                n -> {
                                    put(when, "maxDeaths", n);
                                    changed.run();
                                }));
                hint("Засчитывается, только если игра пройдена с первого уровня пака.");
                restrictions(when);
            }
            case "count" -> {
                row(
                        "Счётчик",
                        Form.select(
                                Form.options(PackDefinition.COUNTERS, COUNTER_LABELS),
                                (String) when.get("counter"),
                                v -> {
                                    when.put("counter", v);
                                    changed.run();
                                }));
                when.putIfAbsent("counter", PackDefinition.COUNTERS.get(0));
                row(
                        "Не меньше",
                        Form.integer(
                                when.get("atLeast"),
                                1,
                                n -> {
                                    put(when, "atLeast", n);
                                    changed.run();
                                }));
                hint("Счётчики общие для всех прохождений и слотов этого пака.");
            }
            case "death" -> {
                when.putIfAbsent("kind", DEATH_KINDS.get(0));
                row(
                        "От чего",
                        Form.select(
                                deathOptions(),
                                (String) when.get("kind"),
                                v -> {
                                    when.put("kind", v);
                                    changed.run();
                                }));
            }
            case "script" -> hint("В скрипте уровня: achieve('" + a.get("id") + "')");
            default -> {}
        }
        if (!type.equals("count")) {
            modes(when, "modes", "Режимы", "ничего не отмечено — любой, кроме казуала");
        }
    }

    @SuppressWarnings("unchecked")
    private void reward(Map<String, Object> a) {
        form.add(Form.heading("Награда")).colspan(2).padTop(8).row();
        Map<String, Object> reward =
                a.get("reward") instanceof Map<?, ?> r
                        ? (Map<String, Object>) r
                        : new LinkedHashMap<>();
        Runnable store =
                () -> {
                    if (reward.isEmpty()) {
                        a.remove("reward");
                    } else {
                        a.put("reward", reward);
                    }
                    changed.run();
                };
        row("Открыть старт с", levelSelect(reward, "unlockStart", levels("(нет)"), store));
        boolean hasItem = reward.get("startItem") instanceof Map;
        VisTable item = new VisTable();
        item.left();
        item.add(
                Form.check(
                        "предмет в стартовом наборе",
                        hasItem,
                        b -> {
                            if (b) {
                                reward.put("startItem", StackEditor.newStack());
                            } else {
                                reward.remove("startItem");
                                reward.remove("modes");
                            }
                            store.run();
                            show(current);
                        }));
        row("", item);
        if (hasItem) {
            row(
                    "Предмет",
                    new StackEditor(items, (Map<String, Object>) reward.get("startItem"), store));
            modes(reward, "modes", "Для режимов", "ничего не отмечено — для всех режимов");
        }
    }

    // ------------------------------------------------------------------ поля

    private List<Form.Option> levels(String none) {
        List<Form.Option> options = new ArrayList<>();
        options.add(new Form.Option(null, none));
        for (String id : draft.levels()) {
            options.add(new Form.Option(id, id));
        }
        return options;
    }

    private com.badlogic.gdx.scenes.scene2d.Actor levelSelect(
            Map<String, Object> map, String key, List<Form.Option> options) {
        return levelSelect(map, key, options, changed);
    }

    private com.badlogic.gdx.scenes.scene2d.Actor levelSelect(
            Map<String, Object> map, String key, List<Form.Option> options, Runnable after) {
        return Form.select(
                options,
                (String) map.get(key),
                v -> {
                    put(map, key, v);
                    after.run();
                });
    }

    @SuppressWarnings("unchecked")
    private void restrictions(Map<String, Object> when) {
        VisTable box = new VisTable();
        box.left();
        List<Object> current =
                when.get("without") instanceof List<?> l ? (List<Object>) l : new ArrayList<>();
        for (int i = 0; i < PackDefinition.RESTRICTIONS.size(); i++) {
            String value = PackDefinition.RESTRICTIONS.get(i);
            box.add(
                            Form.check(
                                    RESTRICTION_LABELS.get(i),
                                    current.contains(value),
                                    b -> {
                                        toggle(when, "without", value, b);
                                        changed.run();
                                    }))
                    .left()
                    .padRight(8);
            if (i % 2 == 1) {
                box.row();
            }
        }
        row("Ограничения", box);
    }

    private void modes(Map<String, Object> map, String key, String label, String hintText) {
        VisTable box = new VisTable();
        box.left();
        List<?> current = map.get(key) instanceof List<?> l ? l : List.of();
        for (int i = 0; i < PackDefinition.MODES.size(); i++) {
            String mode = PackDefinition.MODES.get(i);
            box.add(
                            Form.check(
                                    MODE_LABELS.get(i),
                                    current.contains(mode),
                                    b -> {
                                        toggle(map, key, mode, b);
                                        changed.run();
                                    }))
                    .padRight(8);
        }
        row(label, box);
        hint(hintText);
    }

    /** Добавить или убрать значение в списке {@code key}; пустой список удаляется. */
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

    private void row(String label, com.badlogic.gdx.scenes.scene2d.Actor field) {
        form.add(new VisLabel(label)).width(150).top().padTop(4);
        form.add(field).width(FORM - 160).left().row();
    }

    private void hint(String text) {
        form.add();
        form.add(Form.hint(text, FORM - 160)).width(FORM - 160).row();
    }

    private static VisTextButton button(String text, Runnable action) {
        VisTextButton b = new VisTextButton(text);
        b.addListener(Form.changed(action));
        return b;
    }
}
