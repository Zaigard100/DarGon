package com.zaig100.dargon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Логический экран {@code width×height} пикселей: игра рисуется в маленький буфер, который затем
 * увеличивается в целое число раз и ставится по центру окна. Интерфейс рисуется поверх уже в
 * разрешении окна (сцена Scene2D в логических пикселях с тем же масштабом).
 */
public final class PixelScreen implements Disposable {

    private final int width;
    private final int height;
    private final FrameBuffer buffer;
    private final TextureRegion bufferRegion;

    /** Верхний пиксельный слой (поверх темноты): прозрачный фон. */
    private final FrameBuffer overlay;

    private final TextureRegion overlayRegion;
    private final Matrix4 logical = new Matrix4();
    private final Matrix4 window = new Matrix4();
    private int scale = 1;
    private int originX;
    private int originY;
    private int windowHeight;
    private ShaderProgram effect;
    private float time;

    public PixelScreen(int width, int height) {
        this.width = width;
        this.height = height;
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        buffer.getColorBufferTexture()
                .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        bufferRegion = new TextureRegion(buffer.getColorBufferTexture());
        bufferRegion.flip(false, true);
        overlay = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        overlay.getColorBufferTexture()
                .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        overlayRegion = new TextureRegion(overlay.getColorBufferTexture());
        overlayRegion.flip(false, true);
        logical.setToOrtho2D(0, 0, width, height);
    }

    /**
     * Новый размер окна.
     *
     * @return {@code true}, если изменился масштаб (шрифты нужно перегенерировать)
     */
    public boolean resize(int windowWidth, int windowHeight) {
        int newScale = Math.max(1, Math.min(windowWidth / width, windowHeight / height));
        boolean changed = newScale != scale;
        scale = newScale;
        originX = (windowWidth - width * scale) / 2;
        originY = (windowHeight - height * scale) / 2;
        this.windowHeight = windowHeight;
        window.setToOrtho2D(0, 0, windowWidth, windowHeight);
        return changed;
    }

    /** Начинает рисование в логический буфер. */
    public void beginWorld(SpriteBatch batch) {
        buffer.begin();
        ScreenUtils.clear(Color.BLACK);
        batch.setProjectionMatrix(logical);
        // Батч общий с интерфейсом: Scene2D оставляет в нём цвет последнего виджета.
        batch.setColor(Color.WHITE);
        batch.begin();
    }

    /** Заканчивает рисование в буфер и переносит его в окно (через эффект, если он задан). */
    public void endWorld(SpriteBatch batch) {
        batch.end();
        buffer.end();
        ScreenUtils.clear(Color.BLACK);
        batch.setProjectionMatrix(window);
        batch.setColor(Color.WHITE);
        if (effect != null) {
            time += Gdx.graphics.getDeltaTime();
            batch.setShader(effect);
        }
        batch.begin();
        if (effect != null) {
            effect.setUniformf("u_time", time);
            effect.setUniformf("u_size", width * scale, height * scale);
        }
        batch.draw(bufferRegion, originX, originY, width * scale, height * scale);
        batch.end();
        batch.setShader(null);
    }

    /**
     * Гладкий слой поверх мира в разрешении окна (темнота): шейдер получает прямоугольник игрового
     * окна с текстурными координатами 0..1. Вызывать после {@link #endWorld}.
     */
    public void drawSmooth(
            SpriteBatch batch, ShaderProgram shader, Texture white, Runnable uniforms) {
        batch.setProjectionMatrix(window);
        batch.setColor(Color.WHITE);
        batch.setShader(shader);
        batch.begin();
        uniforms.run();
        batch.draw(white, originX, originY, width * scale, height * scale);
        batch.end();
        batch.setShader(null);
    }

    /** Начинает верхний пиксельный слой (поверх темноты). */
    public void beginOverlay(SpriteBatch batch) {
        overlay.begin();
        ScreenUtils.clear(0, 0, 0, 0);
        batch.setProjectionMatrix(logical);
        batch.setColor(Color.WHITE);
        batch.begin();
    }

    /** Заканчивает верхний слой и накладывает его на окно. */
    public void endOverlay(SpriteBatch batch) {
        batch.end();
        overlay.end();
        batch.setProjectionMatrix(window);
        batch.setColor(Color.WHITE);
        batch.begin();
        batch.draw(overlayRegion, originX, originY, width * scale, height * scale);
        batch.end();
    }

    /** Пост-эффект при переносе картинки в окно ({@code null} — без эффекта). */
    public void setEffect(ShaderProgram effect) {
        this.effect = effect;
    }

    /** Масштаб: сколько пикселей окна в одном логическом пикселе. */
    public int scale() {
        return scale;
    }

    /** Левый нижний угол картинки в окне (пиксели окна). */
    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    /** Логический X точки окна. */
    public float toLogicalX(int screenX) {
        return (screenX - originX) / (float) scale;
    }

    /** Логический Y точки окна, отсчитанный сверху (как в уровне). */
    public float toLogicalYDown(int screenY) {
        int fromBottom = windowHeight - screenY;
        return height - (fromBottom - originY) / (float) scale;
    }

    public boolean contains(float logicalX, float logicalYDown) {
        return logicalX >= 0 && logicalYDown >= 0 && logicalX < width && logicalYDown < height;
    }

    @Override
    public void dispose() {
        buffer.dispose();
        overlay.dispose();
    }
}
