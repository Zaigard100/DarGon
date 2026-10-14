package com.zaig100.dargon.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Кнопка, которая получает фокус: мышь — наведением, клавиатура и геймпад — через {@link
 * FocusNavigator}. Нажатие: клик, тап или действие «использовать».
 */
public final class FocusButton extends Table {

    private static final float DISABLED_ALPHA = 0.4f;

    private final Theme theme;
    private final Runnable action;
    private final Runnable onFocus;
    private final FocusNavigator navigator;
    private boolean focused;
    private boolean enabled = true;

    FocusButton(Theme theme, FocusNavigator navigator, Runnable action, Runnable onFocus) {
        this.theme = theme;
        this.navigator = navigator;
        this.action = action;
        this.onFocus = onFocus;
        setTouchable(Touchable.enabled);
        addListener(
                new ClickListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                        if (pointer == -1 && FocusButton.this.enabled) {
                            navigator.hover(FocusButton.this);
                        }
                    }

                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        navigator.focus(FocusButton.this);
                        activate();
                    }
                });
    }

    /** Нажать (если доступна). */
    public void activate() {
        if (enabled && action != null) {
            theme.pressed();
            action.run();
        }
    }

    public boolean isFocused() {
        return focused;
    }

    void setFocused(boolean focused) {
        if (this.focused == focused) {
            return;
        }
        this.focused = focused;
        setBackground(focused ? theme.focusBackground() : null);
        recolor(this, focused ? theme.focusColor() : null);
        if (focused && onFocus != null) {
            onFocus.run();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getColor().a = enabled ? 1f : DISABLED_ALPHA;
        if (!enabled && focused) {
            navigator.focusNext(this);
        }
    }

    /** Текст в кнопке с фокусом — цветом фокуса; без фокуса — цветом своего стиля. */
    private void recolor(Actor actor, Color color) {
        if (actor instanceof Label label) {
            Object style = label.getUserObject();
            label.setColor(color != null ? color : theme.color((String) style));
        } else if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                recolor(child, color);
            }
        }
    }
}
