package com.zaig100.dargon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.util.ArrayList;
import java.util.List;

/**
 * Фокус кнопок одного экрана. Стрелки, WASD, геймпад двигают фокус к ближайшей кнопке в нужном
 * направлении (по положению на экране — экран не описывает порядок вручную). Если в направлении
 * кнопок нет, фокус переходит на самую дальнюю в обратном (меню «по кругу»).
 */
public final class FocusNavigator {

    private final List<FocusButton> buttons = new ArrayList<>();
    private FocusButton focused;
    private FocusButton initial;
    private final Vector2 corner = new Vector2();
    private final Rectangle from = new Rectangle();
    private final Rectangle to = new Rectangle();

    void register(FocusButton button, boolean initialFocus) {
        buttons.add(button);
        if (initialFocus) {
            initial = button;
        }
    }

    void unregister(FocusButton button) {
        buttons.remove(button);
        if (focused == button) {
            focused = null;
        }
        if (initial == button) {
            initial = null;
        }
    }

    private int pointerX = Integer.MIN_VALUE;
    private int pointerY = Integer.MIN_VALUE;

    /** Запомнить, где курсор при открытии экрана. */
    void markPointer() {
        if (Gdx.input != null) {
            pointerX = Gdx.input.getX();
            pointerY = Gdx.input.getY();
        }
    }

    /**
     * Наведение мышью: фокус меняется, только если курсор сдвинулся после открытия экрана, — иначе
     * экран, открывшийся под неподвижным курсором, отнимал бы фокус у клавиатуры.
     */
    void hover(FocusButton button) {
        if (Gdx.input != null && Gdx.input.getX() == pointerX && Gdx.input.getY() == pointerY) {
            return;
        }
        pointerX = Integer.MIN_VALUE;
        focus(button);
    }

    public FocusButton focused() {
        return focused;
    }

    public List<FocusButton> buttons() {
        return buttons;
    }

    public void focus(FocusButton button) {
        if (button == focused || (button != null && !button.isEnabled())) {
            return;
        }
        if (focused != null) {
            focused.setFocused(false);
        }
        focused = button;
        if (button != null) {
            button.setFocused(true);
        }
    }

    /** Фокус на первоначальную кнопку или первую доступную, если фокуса ещё нет. */
    public void ensureFocus() {
        if (focused != null && usable(focused)) {
            return;
        }
        if (initial != null && usable(initial)) {
            focus(initial);
            return;
        }
        for (FocusButton button : buttons) {
            if (usable(button)) {
                focus(button);
                return;
            }
        }
        focus(null);
    }

    /** Убрать фокус с недоступной кнопки. */
    void focusNext(FocusButton from) {
        if (focused == from) {
            focused.setFocused(false);
            focused = null;
            ensureFocus();
        }
    }

    /**
     * Сдвиг фокуса: {@code dx, dy} — направление (Y вверх, как на экране Scene2D). Сначала —
     * ближайшая кнопка по ходу, перекрывающаяся с текущей поперёк (та же строка или колонка), затем
     * — ближайшая в полуплоскости по направлению, иначе — по кругу.
     */
    public void move(int dx, int dy) {
        if (focused == null || !usable(focused)) {
            ensureFocus();
            return;
        }
        bounds(focused, from);
        FocusButton aligned = null;
        float alignedScore = Float.MAX_VALUE;
        FocusButton ahead = null;
        float aheadScore = Float.MAX_VALUE;
        FocusButton wrap = null;
        float wrapScore = -Float.MAX_VALUE;
        for (FocusButton button : buttons) {
            if (button == focused || !usable(button)) {
                continue;
            }
            bounds(button, to);
            float along =
                    (to.x + to.width / 2 - from.x - from.width / 2) * dx
                            + (to.y + to.height / 2 - from.y - from.height / 2) * dy;
            boolean overlap =
                    dx != 0
                            ? to.y < from.y + from.height && from.y < to.y + to.height
                            : to.x < from.x + from.width && from.x < to.x + to.width;
            float across =
                    Math.abs((to.x + to.width / 2 - from.x - from.width / 2) * dy)
                            + Math.abs((to.y + to.height / 2 - from.y - from.height / 2) * dx);
            if (along > 0.5f) {
                if (overlap && along + across / 4 < alignedScore) {
                    alignedScore = along + across / 4;
                    aligned = button;
                }
                if (along + across < aheadScore) {
                    aheadScore = along + across;
                    ahead = button;
                }
            } else if (overlap && -along > wrapScore) {
                wrapScore = -along;
                wrap = button;
            }
        }
        FocusButton next = aligned != null ? aligned : ahead != null ? ahead : wrap;
        if (next != null && next != focused) {
            moved.run();
        }
        focus(next != null ? next : focused);
    }

    /** Отклик на смену фокуса стрелками (звук). */
    private Runnable moved = () -> {};

    public void onMoved(Runnable moved) {
        this.moved = moved;
    }

    /** Нажать кнопку с фокусом. */
    public void activate() {
        ensureFocus();
        if (focused != null) {
            focused.activate();
        }
    }

    /** Прямоугольник кнопки в координатах сцены. */
    private void bounds(Actor actor, Rectangle out) {
        corner.set(0, 0);
        actor.localToStageCoordinates(corner);
        out.set(corner.x, corner.y, actor.getWidth(), actor.getHeight());
    }

    /** Кнопка видна на сцене и доступна. */
    private static boolean usable(FocusButton button) {
        if (!button.isEnabled() || button.getStage() == null) {
            return false;
        }
        for (Actor actor = button; actor != null; actor = actor.getParent()) {
            if (!actor.isVisible()) {
                return false;
            }
        }
        return true;
    }
}
