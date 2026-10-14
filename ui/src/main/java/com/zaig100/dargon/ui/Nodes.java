package com.zaig100.dargon.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.zaig100.dargon.ui.signal.ReadSignal;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Виды узлов. Создаются только через {@link Ui}. */
final class Nodes {

    private Nodes() {}

    /** Колонка или строка. */
    static final class Box extends UiNode {
        final boolean vertical;
        final List<UiNode> children;
        float gap;

        Box(boolean vertical, List<UiNode> children) {
            this.vertical = vertical;
            this.children = children;
        }
    }

    /** Слои друг над другом. */
    static final class Layers extends UiNode {
        final List<UiNode> children;

        Layers(List<UiNode> children) {
            this.children = children;
        }
    }

    static final class Grid extends UiNode {
        final int columns;
        final List<UiNode> children;
        float gap;

        Grid(int columns, List<UiNode> children) {
            this.columns = columns;
            this.children = children;
        }
    }

    static final class Text extends UiNode {
        final ReadSignal<String> text;
        boolean wrap;

        Text(ReadSignal<String> text) {
            this.text = text;
        }
    }

    static final class Picture extends UiNode {
        final ReadSignal<String> region;
        ReadSignal<String> tint;

        Picture(ReadSignal<String> region) {
            this.region = region;
        }
    }

    static final class Spacer extends UiNode {}

    /** Свой виджет Scene2D (крестовина, мини-карта). */
    static final class Custom extends UiNode {
        final Supplier<Actor> factory;

        Custom(Supplier<Actor> factory) {
            this.factory = factory;
        }
    }

    /** Список узлов по сигналу; виджеты переиспользуются по ключу. */
    static final class Each<T> extends UiNode {
        final ReadSignal<? extends List<T>> items;
        final Function<T, Object> key;
        final Function<T, UiNode> view;
        final int columns;
        float gap;

        Each(
                ReadSignal<? extends List<T>> items,
                Function<T, Object> key,
                Function<T, UiNode> view,
                int columns) {
            this.items = items;
            this.key = key;
            this.view = view;
            this.columns = columns;
        }
    }
}
