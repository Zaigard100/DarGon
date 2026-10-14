package com.zaig100.dargon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Внешний вид: шрифты и цвета по имени стиля, рисунки по имени. */
public interface Theme {

    /** Шрифт стиля; {@code null} — стиль по умолчанию. Размеры шрифта — в логических пикселях. */
    BitmapFont font(String style);

    /** Цвет текста стиля. */
    Color color(String style);

    /** Цвет текста в кнопке с фокусом. */
    Color focusColor();

    /**
     * Рисунок по имени: кадр атласа или фон ({@code "panel"}, {@code "dim"}); {@code null} — нет.
     */
    Drawable drawable(String name);

    /** Фон кнопки с фокусом. */
    Drawable focusBackground();

    /** Отклик на нажатие кнопки (звук) — любым способом: клавиша, мышь, тап, геймпад. */
    default void pressed() {}

    /** Отклик на смену фокуса. */
    default void focusMoved() {}
}
