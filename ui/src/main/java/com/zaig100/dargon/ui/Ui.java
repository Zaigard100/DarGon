package com.zaig100.dargon.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.zaig100.dargon.ui.signal.ReadSignal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Фабрика узлов интерфейса:
 *
 * <pre>{@code
 * column(4,
 *     text(texts.ui("pause.title")).style("title"),
 *     button(text("Продолжить"), screens::pop),
 *     row(2, image("money"), text(coins.map(String::valueOf)).style("small")))
 * }</pre>
 */
public final class Ui {

    private Ui() {}

    /** Колонка с промежутком {@code gap} между элементами. */
    public static UiNode column(float gap, UiNode... children) {
        Nodes.Box box = new Nodes.Box(true, List.of(children));
        box.gap = gap;
        return box;
    }

    /** Строка с промежутком {@code gap}. */
    public static UiNode row(float gap, UiNode... children) {
        Nodes.Box box = new Nodes.Box(false, List.of(children));
        box.gap = gap;
        return box;
    }

    /** Слои: первый — снизу. */
    public static UiNode layers(UiNode... children) {
        return new Nodes.Layers(List.of(children));
    }

    /** Сетка по строкам из {@code columns} столбцов. */
    public static UiNode grid(int columns, float gap, UiNode... children) {
        Nodes.Grid grid = new Nodes.Grid(columns, List.of(children));
        grid.gap = gap;
        return grid;
    }

    public static UiNode text(String text) {
        return new Nodes.Text(ReadSignal.constant(text));
    }

    /** Текст, привязанный к сигналу: меняется только этот виджет. */
    public static UiNode text(ReadSignal<String> text) {
        return new Nodes.Text(text);
    }

    /** Текст с переносом по ширине. */
    public static UiNode paragraph(ReadSignal<String> text) {
        Nodes.Text node = new Nodes.Text(text);
        node.wrap = true;
        return node;
    }

    /** Картинка из атласа. */
    public static UiNode image(String region) {
        return new Nodes.Picture(ReadSignal.constant(region));
    }

    /** Картинка по сигналу (имя кадра; {@code null} — пусто). */
    public static UiNode image(ReadSignal<String> region) {
        return new Nodes.Picture(region);
    }

    /** Картинка с окраской ({@code "#RRGGBB"}; {@code null} — без окраски). */
    public static UiNode image(ReadSignal<String> region, ReadSignal<String> tint) {
        Nodes.Picture picture = new Nodes.Picture(region);
        picture.tint = tint;
        return picture;
    }

    /** Кнопка: получает фокус, нажимается кликом, тапом или действием «использовать». */
    public static ButtonNode button(UiNode content, Runnable action) {
        return new ButtonNode(content, action);
    }

    /** Кнопка с текстом. */
    public static ButtonNode button(String label, Runnable action) {
        return button(text(label).style("button"), action);
    }

    /** Кнопка с текстом по сигналу (переключается язык). */
    public static ButtonNode button(ReadSignal<String> label, Runnable action) {
        return button(text(label).style("button"), action);
    }

    /** Свой виджет Scene2D: создаётся при каждой сборке экрана. */
    public static UiNode custom(Supplier<Actor> factory) {
        return new Nodes.Custom(factory);
    }

    /** Растягивающийся промежуток. */
    public static UiNode spacer() {
        Nodes.Spacer spacer = new Nodes.Spacer();
        spacer.expand();
        return spacer;
    }

    /** Список: при изменении сигнала виджеты с тем же ключом сохраняются. */
    public static <T> UiNode each(
            ReadSignal<? extends List<T>> items,
            Function<T, Object> key,
            Function<T, UiNode> view) {
        return new Nodes.Each<>(items, key, view, 0);
    }

    /** Список сеткой из {@code columns} столбцов. */
    public static <T> UiNode eachGrid(
            ReadSignal<? extends List<T>> items,
            int columns,
            float gap,
            Function<T, Object> key,
            Function<T, UiNode> view) {
        Nodes.Each<T> each = new Nodes.Each<>(items, key, view, columns);
        each.gap = gap;
        return each;
    }

    /** Узел кнопки с дополнительными настройками. */
    public static final class ButtonNode extends UiNode {
        final UiNode content;
        final Runnable action;
        ReadSignal<Boolean> enabled;
        Runnable onFocus;
        boolean initialFocus;

        ButtonNode(UiNode content, Runnable action) {
            this.content = content;
            this.action = action;
        }

        /** Доступна ли кнопка (недоступная не получает фокус и выглядит бледно). */
        public ButtonNode enabled(ReadSignal<Boolean> enabled) {
            this.enabled = enabled;
            return this;
        }

        /** Вызывается, когда кнопка получает фокус (подсказка, описание предмета). */
        public ButtonNode onFocus(Runnable onFocus) {
            this.onFocus = onFocus;
            return this;
        }

        /** Фокус при открытии экрана. */
        public ButtonNode focused() {
            this.initialFocus = true;
            return this;
        }

        @Override
        public ButtonNode pad(float all) {
            super.pad(all);
            return this;
        }

        @Override
        public ButtonNode pad(float top, float left, float bottom, float right) {
            super.pad(top, left, bottom, right);
            return this;
        }

        @Override
        public ButtonNode size(float width, float height) {
            super.size(width, height);
            return this;
        }

        @Override
        public ButtonNode align(int align) {
            super.align(align);
            return this;
        }

        @Override
        public ButtonNode visible(ReadSignal<Boolean> visible) {
            super.visible(visible);
            return this;
        }

        @Override
        public ButtonNode id(String id) {
            super.id(id);
            return this;
        }
    }
}
