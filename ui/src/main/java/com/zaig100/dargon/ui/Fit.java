package com.zaig100.dargon.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

/**
 * Содержимое экрана во всю доступную область; если оно больше области (маленькое окно, длинный
 * перевод), уменьшается целиком, а не обрезается краями.
 */
final class Fit extends WidgetGroup {

    private final Actor content;

    Fit(Actor content) {
        this.content = content;
        setTouchable(Touchable.childrenOnly);
        addActor(content);
    }

    @Override
    public void layout() {
        float prefW = pref(true);
        float prefH = pref(false);
        float scale = Math.min(1f, Math.min(getWidth() / prefW, getHeight() / prefH));
        // Масштаб учитывается, только если трансформация включена у самой группы содержимого.
        if (content instanceof Group group) {
            group.setTransform(scale < 1f);
        }
        if (scale >= 1f) {
            content.setScale(1f);
            content.setBounds(0, 0, getWidth(), getHeight());
        } else {
            // Уменьшаем: содержимое получает свой естественный размер и масштаб.
            float w = getWidth() / scale;
            float h = getHeight() / scale;
            content.setBounds(0, 0, w, h);
            content.setScale(scale);
        }
        if (content instanceof Layout layout) {
            layout.validate();
        }
    }

    private float pref(boolean width) {
        if (content instanceof Layout layout) {
            return Math.max(1f, width ? layout.getPrefWidth() : layout.getPrefHeight());
        }
        return Math.max(1f, width ? content.getWidth() : content.getHeight());
    }

    @Override
    public float getPrefWidth() {
        return 0;
    }

    @Override
    public float getPrefHeight() {
        return 0;
    }
}
