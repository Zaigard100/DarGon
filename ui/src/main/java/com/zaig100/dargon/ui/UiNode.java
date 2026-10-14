package com.zaig100.dargon.ui;

import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.ui.signal.ReadSignal;

/**
 * Описание элемента интерфейса. Узлы создаются фабрикой {@link Ui} и превращаются в виджеты Scene2D
 * сборщиком {@link UiBuilder}; сами узлы ничего не рисуют.
 *
 * <p>Размеры и отступы — в логических пикселях (пикселях тайлов).
 */
public abstract class UiNode {

    String style;
    float padTop;
    float padLeft;
    float padBottom;
    float padRight;
    boolean padSet;
    float width = -1;
    float height = -1;
    int align = Align.center;

    /** Выравнивание задано явно (иначе наследуется от контейнера). */
    boolean alignSet;

    boolean expandX;
    boolean expandY;
    String background;
    ReadSignal<Boolean> visible;
    String id;

    /** Стиль по имени из темы ({@code "title"}, {@code "small"}…). */
    public UiNode style(String style) {
        this.style = style;
        return this;
    }

    public UiNode pad(float all) {
        return pad(all, all, all, all);
    }

    public UiNode pad(float top, float left, float bottom, float right) {
        padSet = true;
        padTop = top;
        padLeft = left;
        padBottom = bottom;
        padRight = right;
        return this;
    }

    /** Точный размер; {@code -1} — по содержимому. */
    public UiNode size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** Выравнивание в выделенной ячейке ({@link Align}). */
    public UiNode align(int align) {
        this.align = align;
        this.alignSet = true;
        return this;
    }

    /** Занять всё свободное место по горизонтали и вертикали. */
    public UiNode expand() {
        expandX = true;
        expandY = true;
        return this;
    }

    public UiNode expandX() {
        expandX = true;
        return this;
    }

    /** Фон: имя рисунка темы ({@code "panel"}, {@code "dim"}…). */
    public UiNode background(String background) {
        this.background = background;
        return this;
    }

    /** Показывать, пока сигнал истинен. */
    public UiNode visible(ReadSignal<Boolean> visible) {
        this.visible = visible;
        return this;
    }

    /** Имя для поиска в тестах и первоначального фокуса. */
    public UiNode id(String id) {
        this.id = id;
        return this;
    }
}
