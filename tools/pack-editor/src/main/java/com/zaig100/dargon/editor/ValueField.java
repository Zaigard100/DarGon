package com.zaig100.dargon.editor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.zaig100.dargon.editor.model.Json;
import com.zaig100.dargon.editor.model.LevelDoc;
import com.zaig100.dargon.level.Direction;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.level.ParamType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Поле значения по типу параметра (как в уровне): числа, да/нет, варианты, направления, клетка,
 * урон, предметы. Неверный ввод не применяется (поле краснеет).
 */
final class ValueField {

    /** Подписи типов для людей. */
    static final List<String> TYPE_LABELS =
            List.of(
                    "целое",
                    "число",
                    "да/нет",
                    "строка",
                    "текст для игрока",
                    "направление (4)",
                    "направление (8)",
                    "клетка",
                    "цвет",
                    "уровень",
                    "сущность уровня",
                    "предмет",
                    "предметы",
                    "товары лавки",
                    "урон");

    private ValueField() {}

    static List<String> typeIds() {
        List<String> ids = new ArrayList<>();
        for (ParamType t : ParamType.values()) {
            ids.add(t.id());
        }
        return ids;
    }

    /**
     * @param values допустимые строки (для string) или {@code null}
     * @param set получает новое значение (Long, Double, Boolean, String, List, Map)
     */
    @SuppressWarnings("unchecked")
    static Actor of(
            ParamType type,
            List<String> values,
            Object current,
            ItemCatalog items,
            Consumer<Object> set) {
        switch (type) {
            case INT:
                return Form.integer(current, Integer.MIN_VALUE, n -> set.accept(n));
            case NUMBER:
                return Form.number(current, n -> set.accept(n));
            case BOOL:
                return Form.check("", Boolean.TRUE.equals(current), b -> set.accept(b));
            case DIRECTION:
            case DIRECTION8:
                {
                    List<Form.Option> options = new ArrayList<>();
                    for (Direction d : Direction.values()) {
                        if (type == ParamType.DIRECTION8 || d.isOrthogonal()) {
                            options.add(new Form.Option(d.id(), d.id()));
                        }
                    }
                    return Form.select(
                            options,
                            current == null ? "right" : String.valueOf(current),
                            set::accept);
                }
            case POINT:
                return Form.validated(
                        current instanceof List<?> l && l.size() == 2
                                ? l.get(0) + ", " + l.get(1)
                                : "",
                        v -> {
                            String[] parts = v.split("[,\\s]+");
                            try {
                                set.accept(
                                        LevelDoc.point(
                                                Integer.parseInt(parts[0].strip()),
                                                Integer.parseInt(parts[1].strip())));
                                return parts.length == 2;
                            } catch (RuntimeException e) {
                                return false;
                            }
                        });
            case DAMAGE:
                return Form.validated(
                        damageText(current),
                        v -> {
                            Object d = parseDamage(v);
                            if (d == null) {
                                return false;
                            }
                            set.accept(d);
                            return true;
                        });
            case ITEM:
                {
                    Map<String, Object> stack =
                            current instanceof Map<?, ?> m
                                    ? Json.copy((Map<String, Object>) m)
                                    : StackEditor.newStack();
                    if (!(current instanceof Map)) {
                        set.accept(Json.copy(stack));
                    }
                    return new StackEditor(items, stack, () -> set.accept(Json.copy(stack)));
                }
            case ITEMS:
            case OFFERS:
                {
                    List<Object> list =
                            current instanceof List<?> l
                                    ? Json.copy((List<Object>) l)
                                    : new ArrayList<>();
                    return new StackEditor.ListEditor(
                            items,
                            list,
                            type == ParamType.OFFERS,
                            () -> set.accept(Json.copy(list)));
                }
            default:
                if (values != null && !values.isEmpty()) {
                    List<Form.Option> options = new ArrayList<>();
                    for (String v : values) {
                        options.add(new Form.Option(v, v));
                    }
                    return Form.select(
                            options,
                            current == null ? values.get(0) : String.valueOf(current),
                            set::accept);
                }
                return Form.text(
                        current == null ? "" : String.valueOf(current), v -> set.accept(v));
        }
    }

    /** Значение по умолчанию для нового поля этого типа. */
    static Object initial(ParamType type, List<String> values) {
        return switch (type) {
            case INT -> 1L;
            case NUMBER -> 1.0;
            case BOOL -> false;
            case DIRECTION, DIRECTION8 -> "right";
            case POINT -> LevelDoc.point(0, 0);
            case DAMAGE -> 5L;
            case ITEM -> StackEditor.newStack();
            case ITEMS, OFFERS -> new ArrayList<>();
            case COLOR -> "#FFFFFF";
            default -> values != null && !values.isEmpty() ? values.get(0) : "";
        };
    }

    static String damageText(Object value) {
        if (value instanceof List<?> l && l.size() == 2) {
            return l.get(0) + "-" + l.get(1);
        }
        return value == null ? "" : String.valueOf(value);
    }

    /** «kill», «5» или «3-7» → значение урона; неверное — {@code null}. */
    static Object parseDamage(String text) {
        String v = text.strip();
        if (v.equals("kill")) {
            return v;
        }
        try {
            if (v.contains("-")) {
                String[] parts = v.split("-");
                long min = Long.parseLong(parts[0].strip());
                long max = Long.parseLong(parts[1].strip());
                return min >= 0 && min <= max ? new ArrayList<>(List.of(min, max)) : null;
            }
            long n = Long.parseLong(v);
            return n >= 0 ? n : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
