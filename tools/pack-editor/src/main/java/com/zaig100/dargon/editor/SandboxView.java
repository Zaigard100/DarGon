package com.zaig100.dargon.editor;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.zaig100.dargon.ecs.EntitySet;
import com.zaig100.dargon.ecs.Family;
import com.zaig100.dargon.game.LevelRuntime;
import com.zaig100.dargon.game.component.GridPos;
import com.zaig100.dargon.game.component.Marker;
import com.zaig100.dargon.game.component.ScriptHandlers;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.level.ItemCatalog;
import com.zaig100.dargon.render.GameAssets;
import com.zaig100.dargon.render.LevelRenderer;
import java.util.function.Supplier;

/**
 * Песочница отладки: мир запущенного уровня целиком. Щелчок по объекту со скриптом вызывает его
 * обработчик; когда время идёт, героем можно ходить (стрелки / WASD, E — использовать).
 */
final class SandboxView extends Widget {

    private static final int TILE = LevelRenderer.TILE;
    private static final Color HANDLER = new Color(0.75f, 0.45f, 1f, 0.9f);

    private final LevelRenderer renderer;
    private final Supplier<DebugSession> session;
    private final Matrix4 transform = new Matrix4();
    private final Matrix4 saved = new Matrix4();
    private LevelRuntime bound;
    private float zoom = 1;
    private int left;
    private int top;

    SandboxView(GameAssets assets, ItemCatalog items, Supplier<DebugSession> session) {
        this.renderer = new LevelRenderer(assets, items, false);
        this.session = session;
        addListener(new Input());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(0.07f, 0.07f, 0.08f, 1);
        batch.draw(CodeEditor.white(), getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
        DebugSession s = session.get();
        if (s == null || !s.canRead() || !(batch instanceof SpriteBatch sprites)) {
            return;
        }
        LevelRuntime runtime = s.runtime();
        if (runtime != bound) {
            renderer.bind(runtime.world());
            bound = runtime;
        }
        int mapW = runtime.state().tiles().width() * TILE;
        int mapH = runtime.state().tiles().height() * TILE;
        zoom = Math.max(0.25f, Math.min(getWidth() / mapW, getHeight() / mapH));
        int viewW = (int) Math.ceil(getWidth() / zoom);
        int viewH = (int) Math.ceil(getHeight() / zoom);
        left = -(viewW - mapW) / 2;
        top = -(viewH - mapH) / 2;
        if (!clipBegin(getX(), getY(), getWidth(), getHeight())) {
            return;
        }
        saved.set(batch.getTransformMatrix());
        transform
                .set(saved)
                .translate(getX(), getY() + getHeight() - viewH * zoom, 0)
                .scale(zoom, zoom, 1);
        batch.setTransformMatrix(transform);
        renderer.renderArea(sprites, runtime, left, top, viewW, viewH);
        // Объекты со скриптами — метка в углу клетки: по ним можно щёлкнуть.
        EntitySet set = runtime.world().entities(Family.all(ScriptHandlers.class, GridPos.class));
        batch.setColor(HANDLER);
        for (int i = 0; i < set.size(); i++) {
            int e = set.get(i);
            if (runtime.world().has(e, Marker.Disabled.class)) {
                continue;
            }
            GridPos p = runtime.world().get(e, GridPos.class);
            float x = p.x * TILE - left;
            float y = viewH - (p.y + 1) * TILE + top;
            batch.draw(CodeEditor.white(), x + TILE - 4, y + TILE - 4, 3, 3);
        }
        batch.setColor(Color.WHITE);
        batch.setTransformMatrix(saved);
        batch.flush();
        clipEnd();
    }

    @Override
    public float getPrefWidth() {
        return 240;
    }

    @Override
    public float getPrefHeight() {
        return 200;
    }

    @Override
    public float getMinWidth() {
        return 0;
    }

    @Override
    public float getMinHeight() {
        return 0;
    }

    private final class Input extends InputListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            getStage().setKeyboardFocus(SandboxView.this);
            DebugSession s = session.get();
            if (s == null || !s.isIdle()) {
                return true;
            }
            int viewH = (int) Math.ceil(getHeight() / zoom);
            int cx = Math.floorDiv((int) (left + x / zoom), TILE);
            int cy = Math.floorDiv((int) (top + viewH - y / zoom), TILE);
            int entity = s.handlerAt(cx, cy, "use");
            if (entity >= 0) {
                s.fire(entity, "use");
            }
            return true;
        }

        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            return key(keycode, true);
        }

        @Override
        public boolean keyUp(InputEvent event, int keycode) {
            return key(keycode, false);
        }

        private boolean key(int keycode, boolean down) {
            Action action =
                    switch (keycode) {
                        case Keys.UP, Keys.W -> Action.MOVE_UP;
                        case Keys.DOWN, Keys.S -> Action.MOVE_DOWN;
                        case Keys.LEFT, Keys.A -> Action.MOVE_LEFT;
                        case Keys.RIGHT, Keys.D -> Action.MOVE_RIGHT;
                        case Keys.E, Keys.SPACE, Keys.ENTER -> Action.USE;
                        default -> null;
                    };
            DebugSession s = session.get();
            if (action == null || s == null) {
                return false;
            }
            s.action(action, down);
            return true;
        }
    }
}
