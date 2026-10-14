package com.zaig100.dargon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Полоса загрузки, пока грузятся ассеты (атласа ещё нет — рисуем простыми прямоугольниками). */
final class LoadingView implements Disposable {

    private static final float BAR_WIDTH = 0.5f;
    private static final float BAR_HEIGHT = 0.02f;

    private final SpriteBatch batch = new SpriteBatch();
    private final Texture white;
    private final Matrix4 projection = new Matrix4();

    LoadingView() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        white = new Texture(pixmap);
        pixmap.dispose();
    }

    void render(float progress) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        ScreenUtils.clear(Color.BLACK);
        projection.setToOrtho2D(0, 0, w, h);
        batch.setProjectionMatrix(projection);
        float barW = w * BAR_WIDTH;
        float barH = Math.max(2, h * BAR_HEIGHT);
        float x = (w - barW) / 2;
        float y = (h - barH) / 2;
        batch.begin();
        batch.setColor(0.2f, 0.2f, 0.22f, 1f);
        batch.draw(white, x, y, barW, barH);
        batch.setColor(1f, 0.85f, 0.3f, 1f);
        batch.draw(white, x, y, barW * progress, barH);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        white.dispose();
    }
}
