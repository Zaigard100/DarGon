package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.custom;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.text;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dargon.render.Shaders;
import com.zaig100.dargon.ui.UiNode;

/**
 * Пасхалка «кубик» (код 4601025111298): 3D-сцена из DarGon3 — шейдер трассировки лучей. WASD —
 * движение, Shift/пробел — вниз/вверх, мышь или палец с зажатием — обзор, Esc — назад.
 *
 * <p>Это отдельная игрушка внутри игры, поэтому клавиши читаются напрямую, а не через действия.
 */
final class CubeScreen extends UiScreen {

    private static final float SPEED = 0.2f;
    private static final float LOOK = 0.005f;

    private ShaderProgram shader;

    @Override
    protected UiNode build() {
        if (shader == null) {
            shader = Shaders.load("shaders/rayCastShader.glsl");
        }
        UiNode hint =
                text(app.texts().tr("cube.hint"))
                        .style("tiny")
                        .align(Align.bottomLeft)
                        .pad(0, 2, 2, 0);
        if (shader == null) {
            return layers(text(app.texts().tr("cube.unavailable")).style("small"), hint)
                    .background("dim")
                    .expand();
        }
        return layers(custom(() -> new Scene(shader, app.assets().region("black"))).expand(), hint);
    }

    @Override
    protected void closed() {
        if (shader != null) {
            shader.dispose();
            shader = null;
        }
    }

    /** Полноэкранный прямоугольник, раскрашенный шейдером. */
    private static final class Scene extends Actor {
        private final ShaderProgram shader;
        private final TextureRegion canvas;
        private float time;
        private float lookX;
        private float lookY;
        private float x = -5;
        private float y;
        private float z;

        Scene(ShaderProgram shader, TextureRegion canvas) {
            this.shader = shader;
            this.canvas = canvas;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            time += delta;
            if (Gdx.input.isTouched()) {
                lookX += Gdx.input.getDeltaX() * LOOK;
                lookY += Gdx.input.getDeltaY() * LOOK;
            }
            float forward = axis(Input.Keys.W, Input.Keys.UP) - axis(Input.Keys.S, Input.Keys.DOWN);
            float side = axis(Input.Keys.D, Input.Keys.RIGHT) - axis(Input.Keys.A, Input.Keys.LEFT);
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                z += SPEED / 2;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                z -= SPEED / 2;
            }
            // Направление взгляда поворачивает шаг (как в DarGon3).
            float tz = (float) (-forward * Math.sin(-lookY));
            float tx = (float) (forward * Math.cos(-lookY));
            x += (float) (tx * Math.cos(lookX) - side * Math.sin(lookX)) * SPEED;
            y += (float) (tx * Math.sin(lookX) + side * Math.cos(lookX)) * SPEED;
            z += tz * SPEED;
        }

        private static float axis(int key, int alternative) {
            return Gdx.input.isKeyPressed(key) || Gdx.input.isKeyPressed(alternative) ? 1 : 0;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setShader(shader);
            shader.setUniformf("time", time);
            shader.setUniformf(
                    "resolution",
                    Gdx.graphics.getBackBufferWidth(),
                    Gdx.graphics.getBackBufferHeight());
            shader.setUniformf("mouse", lookX, lookY);
            shader.setUniformf("campos", x, y, z);
            batch.draw(canvas, getX(), getY(), getWidth(), getHeight());
            batch.setShader(null);
        }
    }
}
