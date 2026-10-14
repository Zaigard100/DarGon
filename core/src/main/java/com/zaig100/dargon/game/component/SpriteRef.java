package com.zaig100.dargon.game.component;

import com.zaig100.dargon.ecs.Component;

/**
 * Картинка сущности: кадр атласа, номер кадра в полосе 16×16, отражение, сдвиг и поворот.
 *
 * <p>Если у сущности есть {@link Facing}, картинка поворачивается по направлению: {@code angle} —
 * поворот для направления «вправо» (у арбалета и стрелы картинка смотрит влево, поэтому 180).
 */
public final class SpriteRef implements Component {

    /** Кадр атласа; {@code null} — ничего не рисовать (целая плитка). */
    public String region;

    public int frame;
    public boolean flipX;

    /** Сдвиг картинки в пикселях (Y вниз). */
    public int offsetX;

    public int offsetY;

    /** Поворот (градусы против часовой) для направления «вправо». */
    public float angle;

    @Override
    public void reset() {
        region = null;
        frame = 0;
        flipX = false;
        offsetX = 0;
        offsetY = 0;
        angle = 0;
    }
}
