package com.zaig100.dargon.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Собранный экран интерфейса: корневой виджет на весь экран, подписки и фокус. */
public final class UiView {

    private final Table root = new Table();
    private final Bindings bindings = new Bindings();
    private final FocusNavigator focus = new FocusNavigator();

    public UiView(Theme theme, UiNode node) {
        root.setFillParent(true);
        Actor content = new UiBuilder(theme, focus).build(node, bindings);
        root.add(new Fit(content)).expand().fill();
        focus.onMoved(theme::focusMoved);
        root.setTouchable(Touchable.childrenOnly);
    }

    public Table root() {
        return root;
    }

    public FocusNavigator focus() {
        return focus;
    }

    /** Добавить на сцену (поверх уже добавленных). */
    public void attach(Group parent) {
        parent.addActor(root);
        root.validate();
        focus.markPointer();
        focus.ensureFocus();
    }

    /** Убрать со сцены и отписаться от всех сигналов. */
    public void dispose() {
        root.remove();
        bindings.dispose();
    }

    public Bindings bindings() {
        return bindings;
    }
}
