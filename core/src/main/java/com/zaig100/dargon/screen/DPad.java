package com.zaig100.dargon.screen;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.zaig100.dargon.input.Action;
import com.zaig100.dargon.input.InputMapper;
import com.zaig100.dargon.render.GameAssets;

/**
 * Крестовина: палец держит направление (шаг повторяется, пока держишь). Касание в углу между
 * стрелками держит сразу две — шаг по диагонали. Зона касания больше рисунка.
 */
final class DPad extends Actor {

    /** Касание ближе к центру не нажимает ничего. */
    private static final float DEAD_ZONE = 0.15f;

    /**
     * Угол касания считается диагональю, если меньшая составляющая не меньше этой доли большей
     * (≈27° от оси): прямые направления чуть шире диагоналей, чтобы не промахиваться.
     */
    private static final float DIAGONAL = 0.5f;

    private final GameAssets assets;
    private final InputMapper input;
    private Action horizontal;
    private Action vertical;

    DPad(GameAssets assets, InputMapper input, float size) {
        this.assets = assets;
        this.input = input;
        setSize(size, size);
        setTouchable(Touchable.enabled);
        addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event, float x, float y, int pointer, int button) {
                        update(x, y);
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        update(x, y);
                    }

                    @Override
                    public void touchUp(
                            InputEvent event, float x, float y, int pointer, int button) {
                        hold(null, null);
                    }
                });
    }

    private void update(float x, float y) {
        float dx = x / getWidth() - 0.5f;
        float dy = y / getHeight() - 0.5f;
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        if (ax < DEAD_ZONE && ay < DEAD_ZONE) {
            hold(null, null);
            return;
        }
        Action h = dx > 0 ? Action.MOVE_RIGHT : Action.MOVE_LEFT;
        Action v = dy > 0 ? Action.MOVE_UP : Action.MOVE_DOWN;
        if (Math.min(ax, ay) >= DIAGONAL * Math.max(ax, ay)) {
            hold(h, v);
        } else if (ax > ay) {
            hold(h, null);
        } else {
            hold(null, v);
        }
    }

    private void hold(Action h, Action v) {
        horizontal = swap(horizontal, h);
        vertical = swap(vertical, v);
    }

    private Action swap(Action old, Action now) {
        if (old != now) {
            if (old != null) {
                input.actionUp(old);
            }
            if (now != null) {
                input.actionDown(now);
            }
        }
        return now;
    }

    @Override
    protected void setStage(Stage stage) {
        if (stage == null) {
            // Экран закрыт, пока палец на крестовине — отпустить направление.
            hold(null, null);
        }
        super.setStage(stage);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        String region = "dpad";
        if (vertical != null) {
            region += vertical == Action.MOVE_UP ? "_up" : "_down";
        }
        if (horizontal != null) {
            region += horizontal == Action.MOVE_RIGHT ? "_right" : "_left";
        }
        TextureRegion r = assets.region(region);
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);
        batch.draw(r, getX(), getY(), getWidth(), getHeight());
        batch.setColor(1, 1, 1, 1);
    }
}
