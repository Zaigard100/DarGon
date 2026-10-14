package com.zaig100.dargon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Превращает дерево {@link UiNode} в виджеты Scene2D и связывает их с сигналами. */
public final class UiBuilder {

    private final Theme theme;
    private final FocusNavigator focus;

    public UiBuilder(Theme theme, FocusNavigator focus) {
        this.theme = theme;
        this.focus = focus;
    }

    /** Собирает узел; подписки складываются в {@code bindings}. */
    public Actor build(UiNode node, Bindings bindings) {
        Actor actor = create(node, bindings);
        if (node.id != null) {
            actor.setName(node.id);
        }
        if (node.visible != null) {
            // Скрытый элемент не занимает места (в Scene2D невидимый виджет держит размер ячейки).
            Collapsible wrapper = new Collapsible(actor);
            bindings.observe(
                    node.visible,
                    visible -> {
                        wrapper.setVisible(visible);
                        wrapper.invalidateHierarchy();
                    });
            return wrapper;
        }
        return actor;
    }

    /** Обёртка, которая при скрытии сжимается до нулевого размера. */
    private static final class Collapsible extends Container<Actor> {
        Collapsible(Actor actor) {
            super(actor);
            fill();
            setTouchable(Touchable.childrenOnly);
        }

        @Override
        public float getPrefWidth() {
            return isVisible() ? super.getPrefWidth() : 0;
        }

        @Override
        public float getPrefHeight() {
            return isVisible() ? super.getPrefHeight() : 0;
        }

        @Override
        public float getMinWidth() {
            return isVisible() ? super.getMinWidth() : 0;
        }

        @Override
        public float getMinHeight() {
            return isVisible() ? super.getMinHeight() : 0;
        }
    }

    private Actor create(UiNode node, Bindings bindings) {
        if (node instanceof Nodes.Box box) {
            return box(box, bindings);
        }
        if (node instanceof Nodes.Layers layers) {
            Stack stack = new Stack();
            for (UiNode child : layers.children) {
                stack.add(wrap(child, build(child, bindings)));
            }
            return background(stack, node);
        }
        if (node instanceof Nodes.Grid grid) {
            Table table = table(node);
            for (int i = 0; i < grid.children.size(); i++) {
                UiNode child = grid.children.get(i);
                place(table.add(build(child, bindings)), child, grid.align).space(grid.gap);
                if ((i + 1) % grid.columns == 0) {
                    table.row();
                }
            }
            return table;
        }
        if (node instanceof Nodes.Text text) {
            return text(text, bindings);
        }
        if (node instanceof Nodes.Picture picture) {
            return picture(picture, bindings);
        }
        if (node instanceof Ui.ButtonNode button) {
            return button(button, bindings);
        }
        if (node instanceof Nodes.Spacer) {
            Actor spacer = new Actor();
            spacer.setTouchable(Touchable.disabled);
            return spacer;
        }
        if (node instanceof Nodes.Custom custom) {
            return custom.factory.get();
        }
        if (node instanceof Nodes.Each<?> each) {
            return each(each, bindings);
        }
        throw new IllegalArgumentException("Неизвестный узел: " + node.getClass());
    }

    private Table box(Nodes.Box box, Bindings bindings) {
        Table table = table(box);
        boolean first = true;
        for (UiNode child : box.children) {
            Cell<Actor> cell = place(table.add(build(child, bindings)), child, box.align);
            if (child instanceof Nodes.Spacer) {
                // Распорка тянется только вдоль своей оси.
                if (box.vertical) {
                    cell.expand(0, 1).fill(0f, 1f);
                } else {
                    cell.expand(1, 0).fill(1f, 0f);
                }
            }
            if (!first) {
                if (box.vertical) {
                    cell.spaceTop(box.gap);
                } else {
                    cell.spaceLeft(box.gap);
                }
            }
            if (box.vertical) {
                table.row();
            }
            first = false;
        }
        return table;
    }

    private Table table(UiNode node) {
        Table table = new Table();
        // Содержимое прижимается так же, как сам узел (важно, когда таблица растянута).
        table.align(node.align);
        table.pad(node.padTop, node.padLeft, node.padBottom, node.padRight);
        Drawable bg = node.background == null ? null : theme.drawable(node.background);
        if (bg != null) {
            table.setBackground(bg);
        }
        return table;
    }

    private Actor background(Actor actor, UiNode node) {
        if (node.background == null
                && node.padTop == 0
                && node.padLeft == 0
                && node.padBottom == 0
                && node.padRight == 0) {
            return actor;
        }
        Container<Actor> container = new Container<>(actor).fill();
        container.pad(node.padTop, node.padLeft, node.padBottom, node.padRight);
        if (node.background != null) {
            container.setBackground(theme.drawable(node.background));
        }
        return container;
    }

    private Label text(Nodes.Text node, Bindings bindings) {
        Label.LabelStyle style = new Label.LabelStyle(theme.font(node.style), Color.WHITE);
        Label label = new Label("", style);
        label.setUserObject(node.style);
        label.setColor(theme.color(node.style));
        label.setWrap(node.wrap);
        label.setAlignment(node.align);
        bindings.observe(node.text, value -> label.setText(value == null ? "" : value));
        return label;
    }

    private Image picture(Nodes.Picture node, Bindings bindings) {
        Image image = new Image();
        image.setScaling(Scaling.stretch);
        image.setTouchable(Touchable.disabled);
        bindings.observe(
                node.region,
                region -> {
                    image.setDrawable(region == null ? null : theme.drawable(region));
                    if (node.width < 0) {
                        image.invalidateHierarchy();
                    }
                });
        if (node.tint != null) {
            bindings.observe(
                    node.tint,
                    hex -> image.setColor(hex == null ? Color.WHITE : Color.valueOf(hex)));
        }
        return image;
    }

    private FocusButton button(Ui.ButtonNode node, Bindings bindings) {
        FocusButton button = new FocusButton(theme, focus, node.action, node.onFocus);
        if (node.padSet) {
            button.pad(node.padTop, node.padLeft, node.padBottom, node.padRight);
        } else {
            button.pad(1, 2, 1, 2);
        }
        Actor content = build(node.content, bindings);
        content.setTouchable(Touchable.disabled);
        place(button.add(content), node.content, node.align);
        focus.register(button, node.initialFocus);
        bindings.add(() -> focus.unregister(button));
        if (node.enabled != null) {
            bindings.observe(node.enabled, button::setEnabled);
        }
        return button;
    }

    /**
     * Список с переиспользованием виджетов: элемент с тем же ключом и значением не пересоздаётся.
     */
    private <T> Table each(Nodes.Each<T> node, Bindings bindings) {
        Table table = table(node);
        Map<Object, Entry> built = new HashMap<>();
        bindings.observe(
                node.items,
                items -> {
                    Map<Object, Entry> next = new HashMap<>();
                    List<Entry> order = new ArrayList<>();
                    for (T item : items) {
                        Object key = node.key.apply(item);
                        Entry entry = built.remove(key);
                        if (entry == null || !Objects.equals(entry.item, item)) {
                            if (entry != null) {
                                entry.bindings.dispose();
                            }
                            Bindings own = new Bindings();
                            UiNode view = node.view.apply(item);
                            entry = new Entry(item, build(view, own), own, view);
                        }
                        next.put(key, entry);
                        order.add(entry);
                    }
                    built.values().forEach(e -> e.bindings.dispose());
                    built.clear();
                    built.putAll(next);
                    table.clearChildren();
                    for (int i = 0; i < order.size(); i++) {
                        Entry entry = order.get(i);
                        place(table.add(entry.actor), entry.view, node.align).space(node.gap / 2);
                        if (node.columns <= 0 || (i + 1) % node.columns == 0) {
                            table.row();
                        }
                    }
                    // Кнопка с фокусом могла быть пересоздана — вернуть фокус.
                    focus.ensureFocus();
                });
        bindings.add(
                () -> {
                    built.values().forEach(e -> e.bindings.dispose());
                    built.clear();
                });
        return table;
    }

    /** Элемент списка: значение, виджет, его подписки и узел (для размещения). */
    private record Entry(Object item, Actor actor, Bindings bindings, UiNode view) {}

    /** Размер, отступы, выравнивание узла — в ячейке таблицы. */
    private static Cell<Actor> place(Cell<Actor> cell, UiNode node, int parentAlign) {
        if (node.width >= 0) {
            cell.width(node.width);
        }
        if (node.height >= 0) {
            cell.height(node.height);
        }
        cell.align(node.alignSet ? node.align : parentAlign);
        if (node.expandX) {
            cell.expandX().fillX();
        }
        if (node.expandY) {
            cell.expandY().fillY();
        }
        if (node instanceof Nodes.Text text && text.wrap) {
            cell.fillX();
        }
        return cell;
    }

    /** Слой стека с выравниванием и отступами узла. */
    private static Actor wrap(UiNode node, Actor actor) {
        // Слой растягивается на весь стек, даже если у содержимого точный размер (иначе
        // максимум контейнера равен размеру содержимого и выравнивание не работает).
        Container<Actor> container =
                new Container<>(actor) {
                    @Override
                    public float getMaxWidth() {
                        return 0;
                    }

                    @Override
                    public float getMaxHeight() {
                        return 0;
                    }
                };
        container.align(node.align);
        container.pad(node.padTop, node.padLeft, node.padBottom, node.padRight);
        if (node.expandX || node.expandY) {
            container.fill(node.expandX, node.expandY);
        }
        if (node.width >= 0) {
            container.width(node.width);
        }
        if (node.height >= 0) {
            container.height(node.height);
        }
        container.setTouchable(Touchable.childrenOnly);
        return container;
    }
}
