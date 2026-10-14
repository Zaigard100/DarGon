package com.zaig100.dargon.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextField;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Поля форм редактора: выбор с подписями, числа с проверкой, флажки, подсказки. */
final class Form {

    private Form() {}

    /** Пункт выбора: значение и подпись для человека. */
    record Option(String value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    static List<Option> options(List<String> values, List<String> labels) {
        java.util.List<Option> result = new java.util.ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            result.add(
                    new Option(values.get(i), i < labels.size() ? labels.get(i) : values.get(i)));
        }
        return result;
    }

    /** Выпадающий список; {@code current} не из списка — добавляется как есть. */
    static VisSelectBox<Option> select(List<Option> options, String current, Consumer<String> set) {
        VisSelectBox<Option> box = new VisSelectBox<>();
        Array<Option> items = new Array<>();
        Option selected = null;
        for (Option o : options) {
            items.add(o);
            if (o.value() == null ? current == null : o.value().equals(current)) {
                selected = o;
            }
        }
        if (selected == null && current != null) {
            selected = new Option(current, current + " (?)");
            items.add(selected);
        }
        box.setItems(items);
        if (selected != null) {
            box.setSelected(selected);
        }
        box.addListener(changed(() -> set.accept(box.getSelected().value())));
        return box;
    }

    /** Текстовое поле: каждое изменение — сразу в модель. */
    static VisTextField text(String value, Consumer<String> set) {
        VisTextField f = new VisTextField(value == null ? "" : value);
        f.addListener(changed(() -> set.accept(f.getText())));
        return f;
    }

    /** Поле с проверкой: неверное значение не применяется, поле краснеет. */
    static VisTextField validated(String value, Predicate<String> set) {
        VisTextField f = new VisTextField(value == null ? "" : value);
        f.addListener(changed(() -> f.setInputValid(set.test(f.getText()))));
        return f;
    }

    /** Целое ≥ {@code min}; пустое — {@code null} (поле не задано). */
    static VisTextField integer(Object value, int min, Consumer<Long> set) {
        return validated(
                value == null ? "" : String.valueOf(value),
                v -> {
                    if (v.isBlank()) {
                        set.accept(null);
                        return true;
                    }
                    try {
                        long n = Long.parseLong(v.strip());
                        if (n < min) {
                            return false;
                        }
                        set.accept(n);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
    }

    /** Число; пустое — {@code null}. */
    static VisTextField number(Object value, Consumer<Double> set) {
        return validated(
                value == null ? "" : String.valueOf(value),
                v -> {
                    if (v.isBlank()) {
                        set.accept(null);
                        return true;
                    }
                    try {
                        set.accept(Double.parseDouble(v.strip().replace(',', '.')));
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
    }

    static VisCheckBox check(String label, boolean value, Consumer<Boolean> set) {
        VisCheckBox box = new VisCheckBox(label, value);
        box.addListener(changed(() -> set.accept(box.isChecked())));
        return box;
    }

    static VisLabel hint(String text, float width) {
        VisLabel label = new VisLabel(text, "small");
        label.setWrap(true);
        label.setColor(Color.LIGHT_GRAY);
        label.setWidth(width);
        return label;
    }

    static VisLabel heading(String text) {
        VisLabel label = new VisLabel(text);
        label.setColor(new Color(1, 0.85f, 0.4f, 1));
        return label;
    }

    /** Прокрутка колесом там, где мышь (в модальных окнах фокус колеса сам не переходит). */
    static <T extends com.badlogic.gdx.scenes.scene2d.ui.ScrollPane> T wheel(T pane) {
        pane.addListener(
                new com.badlogic.gdx.scenes.scene2d.InputListener() {
                    @Override
                    public void enter(
                            com.badlogic.gdx.scenes.scene2d.InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            Actor from) {
                        if (pane.getStage() != null) {
                            pane.getStage().setScrollFocus(pane);
                        }
                    }
                });
        return pane;
    }

    static ChangeListener changed(Runnable r) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        };
    }

    /**
     * Высота поля в строках. С запасом: при масштабе интерфейса не 100% высота строки дробная, и
     * поле ровно в N строк считает, что в нём помещается N−1 (одна строка — ни одной, текст не
     * виден).
     */
    static void rows(com.kotcrab.vis.ui.widget.VisTextArea area, float rows) {
        area.setPrefRows(rows + 0.1f);
    }
}
