package com.zaig100.dargon.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.render.GameTheme;
import com.zaig100.dargon.render.LevelRenderer;
import com.zaig100.dargon.ui.UiView;
import java.util.ArrayList;
import java.util.List;

/**
 * Стек экранов интерфейса на одной сцене Scene2D. Единица сцены — логический пиксель: при
 * целочисленном масштабе окна картинки остаются пиксельными, а текст — чётким.
 */
public final class Screens implements Disposable {

    private final App app;
    private final ScreenViewport viewport = new ScreenViewport();
    private final Stage stage;
    private final Group full = new Group();
    private final Group game = new Group();
    private final List<UiScreen> stack = new ArrayList<>();
    private final List<UiView> views = new ArrayList<>();
    private GameTheme theme;

    Screens(App app, SpriteBatch batch) {
        this.app = app;
        this.stage = new Stage(viewport, batch);
        full.setTouchable(Touchable.childrenOnly);
        game.setTouchable(Touchable.childrenOnly);
        stage.addActor(game);
        stage.addActor(full);
    }

    public Stage stage() {
        return stage;
    }

    /** Новый размер окна и масштаб: экраны пересобираются с новыми шрифтами. */
    void resize(int width, int height, int scale, int originX, int originY, GameTheme theme) {
        viewport.setUnitsPerPixel(1f / scale);
        viewport.update(width, height, true);
        full.setBounds(0, 0, width / (float) scale, height / (float) scale);
        game.setBounds(
                originX / (float) scale,
                originY / (float) scale,
                LevelRenderer.VIEW_WIDTH,
                LevelRenderer.VIEW_HEIGHT);
        this.theme = theme;
        rebuild();
    }

    /** Пересобрать все экраны (масштаб, тема). Язык пересборки не требует — тексты привязаны. */
    public void rebuild() {
        for (UiView view : views) {
            view.dispose();
        }
        views.clear();
        for (UiScreen screen : stack) {
            views.add(show(screen));
        }
        hideCovered();
    }

    private UiView show(UiScreen screen) {
        UiView view = new UiView(theme, screen.build());
        view.attach(screen.area() == UiScreen.Area.GAME ? game : full);
        return view;
    }

    public void push(UiScreen screen) {
        screen.attach(app);
        stack.add(screen);
        views.add(show(screen));
        hideCovered();
    }

    /**
     * Немодальные экраны (HUD, сенсорные кнопки) под открытым модальным не принимают касаний —
     * иначе кнопки сумки и паузы под инвентарём открывали бы его ещё раз. Слой всего окна рисуется
     * поверх игровой области, поэтому сенсорные кнопки под модальным экраном игровой области
     * (магазин, инвентарь) ещё и прячутся, чтобы не закрывать его.
     */
    private void hideCovered() {
        boolean covered = false;
        boolean coveredInGame = false;
        for (int i = stack.size() - 1; i >= 0; i--) {
            UiScreen screen = stack.get(i);
            if (!screen.modal()) {
                Table root = views.get(i).root();
                root.setTouchable(covered ? Touchable.disabled : Touchable.childrenOnly);
                if (screen.area() == UiScreen.Area.FULL) {
                    root.setVisible(!coveredInGame);
                }
            }
            covered |= screen.modal();
            coveredInGame |= screen.modal() && screen.area() == UiScreen.Area.GAME;
        }
    }

    /** Закрывает экран (и все над ним). */
    public void pop(UiScreen screen) {
        int index = stack.indexOf(screen);
        if (index < 0) {
            return;
        }
        while (stack.size() > index) {
            UiScreen removed = stack.remove(stack.size() - 1);
            views.remove(views.size() - 1).dispose();
            removed.closed();
        }
        hideCovered();
        if (!views.isEmpty()) {
            views.get(views.size() - 1).focus().ensureFocus();
        }
    }

    /** Закрыть всё и открыть {@code screen}. */
    public void replaceAll(UiScreen screen) {
        clear();
        push(screen);
    }

    public void clear() {
        if (!stack.isEmpty()) {
            pop(stack.get(0));
        }
    }

    public UiScreen top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /** Верхний модальный экран (он получает клавиши), или {@code null}. */
    UiScreen modalTop() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).modal()) {
                return stack.get(i);
            }
        }
        return null;
    }

    /** Стоит ли мир игры. */
    boolean gamePaused() {
        for (UiScreen screen : stack) {
            if (screen.pausesGame()) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Class<? extends UiScreen> type) {
        for (UiScreen screen : stack) {
            if (type.isInstance(screen)) {
                return true;
            }
        }
        return false;
    }

    /** Нажатие действия: модальному экрану сверху. */
    boolean onAction(Action action) {
        UiScreen top = modalTop();
        if (top == null) {
            return false;
        }
        UiView view = views.get(stack.indexOf(top));
        top.onAction(action, view.focus());
        return true;
    }

    void update(float delta) {
        for (UiScreen screen : new ArrayList<>(stack)) {
            screen.update(delta);
        }
        stage.act(delta);
    }

    void draw() {
        viewport.apply();
        stage.draw();
    }

    @Override
    public void dispose() {
        for (UiView view : views) {
            view.dispose();
        }
        views.clear();
        stack.clear();
        stage.dispose();
    }
}
