package com.zaig100.dargon.game.system;

import com.zaig100.dargon.ecs.ComponentMapper;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.ecs.IteratingSystem;
import com.zaig100.dargon.game.component.Health;
import com.zaig100.dargon.game.component.Motion;
import com.zaig100.dargon.game.component.PlayerAnim;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.ActionState;

/**
 * Двигает картинки сущностей к их клеткам со скоростью {@link Motion#BASE_SPEED} пикселей в секунду
 * по каждой оси и анимирует игрока.
 */
public final class MotionSystem extends IteratingSystem {

    private ComponentMapper<Motion> motion;
    private ComponentMapper<PlayerAnim> anim;
    private ComponentMapper<Health> health;
    private ActionState input;

    public MotionSystem() {
        super(Family.all(Motion.class));
    }

    @Override
    protected void process(int entity, float delta) {
        Motion m = motion.get(entity);
        m.prevOffsetX = m.offsetX;
        m.prevOffsetY = m.offsetY;
        float step = Motion.BASE_SPEED * m.speed * delta;
        PlayerAnim a = anim.getOrNull(entity);
        // Отражение спрайта — как в DarGon3: влево — отражён, вправо и вниз — нет.
        if (m.offsetX != 0) {
            if (a != null) {
                a.flipX = m.offsetX > 0;
            }
            m.offsetX = approachZero(m.offsetX, step);
        }
        if (m.offsetY != 0) {
            if (a != null && m.offsetY < 0) {
                a.flipX = false;
            }
            m.offsetY = approachZero(m.offsetY, step);
        }
        if (a != null) {
            animate(entity, m, a, delta);
        }
    }

    private void animate(int entity, Motion m, PlayerAnim a, float delta) {
        Health h = health.getOrNull(entity);
        boolean alive = h == null || !h.isDead();
        a.running =
                alive
                        && (m.isMoving()
                                || input.isHeld(Action.MOVE_UP)
                                || input.isHeld(Action.MOVE_DOWN)
                                || input.isHeld(Action.MOVE_LEFT)
                                || input.isHeld(Action.MOVE_RIGHT));
        // Кадры 1..5 по кругу (кадр 0 — только в самом начале), как в DarGon3.
        a.timer += delta;
        if (a.timer >= PlayerAnim.FRAME_TIME / m.speed) {
            if (a.frame > 4) {
                a.frame = 0;
            }
            a.frame++;
            a.timer = 0;
        }
    }

    private static float approachZero(float value, float step) {
        if (value > 0) {
            return Math.max(0, value - step);
        }
        return Math.min(0, value + step);
    }
}
