package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/**
 * Плавный переход между клетками. Логическая клетка ({@link GridPos}) меняется сразу в начале шага,
 * а картинка догоняет её: {@code offsetX/offsetY} — сколько пикселей осталось до клетки (в пикселях
 * тайла, Y вниз).
 */
public final class Motion implements Component {

    /** Скорость, пикселей тайла в секунду (в DarGon3 — 1 пиксель за кадр при 60 FPS). */
    public static final float BASE_SPEED = 60f;

    public float offsetX;
    public float offsetY;

    /** Смещение на прошлом тике — для интерполяции при отрисовке. */
    public float prevOffsetX;

    public float prevOffsetY;

    /** Множитель скорости (зелья скорости и замедления). */
    public float speed = 1f;

    public boolean isMoving() {
        return offsetX != 0 || offsetY != 0;
    }

    /** Мгновенно встать в клетку (телепорт). */
    public void snap() {
        offsetX = 0;
        offsetY = 0;
        prevOffsetX = 0;
        prevOffsetY = 0;
    }

    @Override
    public void reset() {
        snap();
        speed = 1f;
    }
}
