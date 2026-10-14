package com.zaig100.dg.screen.extensions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.zaig100.dg.Main;
import com.zaig100.dg.screen.MenuScreen;
import com.zaig100.dg.utils.Res;

public class ResLoadScreen implements Screen {

    Main m;
    int wight, height;
    SpriteBatch batch;
    BitmapFont font;
    int num = 0;
    Texture zaigard[] = new Texture[8];
    float[][] colors = new float[][]{
            {0.8705882f, 0.890196f, 0.8745098f},
            {0.6392157f, 0.592116f, 0.6f},
            {0.7803922f, 0.807843f, 0.635294f},
            {0.3529412f, 0.317647f, 0.298039f},
            {0.4823529f, 0.592157f, 0.603922f},
            {0.4862745f, 0.447059f, 0.439215f},
            {0.7294118f, 0.541176f, 0.392156f},
            {0.6980392f, 0.686275f, 0.666667f}
    };
    Texture progresbar;
    GlyphLayout load_layout, press_layout;
    float tick_timer = 0;
    float color_a = 1;
    byte color_dr = -1;

    final int load_while = 18;

    public ResLoadScreen(Main main) {
        m = main;
        num = (int) Math.round(7 * Math.random());
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        font = new BitmapFont();
        font.setColor(0f, 0f, 0f, 0.5f);
        load_layout = new GlyphLayout();
        press_layout = new GlyphLayout();
        this.zaigard[0] = new Texture("zaigard/0.jpg");
        this.zaigard[1] = new Texture("zaigard/1.jpg");
        this.zaigard[3] = new Texture("zaigard/2.jpg");
        this.zaigard[4] = new Texture("zaigard/3.jpg");
        this.zaigard[5] = new Texture("zaigard/4.jpg");
        this.zaigard[6] = new Texture("zaigard/5.jpg");
        this.zaigard[7] = new Texture("zaigard/6.jpg");
        this.zaigard[2] = new Texture("zaigard/7.jpg");
        progresbar = new Texture("texture/resume.png");
        load_layout.setText(font, "Load resource: " + String.valueOf(0) + "%", Color.BLACK, Gdx.graphics.getWidth(), Align.center, true);
        press_layout.setText(font, "Press to screen", Color.BLACK, Gdx.graphics.getWidth(), Align.center, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(colors[num][0], colors[num][1], colors[num][2], 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (tick_timer > 0.15f) {
            Res.sprL();
            load_layout.setText(font, "Load resource: " + String.valueOf((int) ((float) Res.i / load_while * 100)) + "%", Color.BLACK, Gdx.graphics.getWidth(), Align.center, true);
            tick_timer = 0;
        }
        batch.begin();
        batch.draw(zaigard[num], 3 * wight / 8, height / 2 - height / 8, wight / 4, wight / 4);
        batch.draw(progresbar, 3 * wight / 8, height / 2 - height / 6, wight / 4 * (float) ((float) Res.i / load_while), wight / 128);
        font.draw(batch, load_layout, 0, height / 2 - height / 4);
        if (Res.end) {
            loadEnd(0.03125f);
        }
        batch.end();

        tick_timer += Gdx.graphics.getDeltaTime();
    }

    private void loadEnd(float speed) {

        if (color_a <= 0) {
            color_dr = 1;
        } else if (color_a >= 1) {
            color_dr = -1;
        }

        color_a += color_dr * speed;

        press_layout.setText(font, "Press to screen", new Color(0, 0, 0, color_a), Gdx.graphics.getWidth(), Align.center, true);
        font.draw(batch, press_layout, 0, height / 2 - height / 3);

        if (Gdx.input.justTouched()) {
            m.setScreen(new MenuScreen(m));
        }

    }

    @Override
    public void resize(int width, int height) {
        this.wight = width;
        this.height = height;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        this.zaigard[0].dispose();
        this.zaigard[1].dispose();
        this.zaigard[2].dispose();
        this.zaigard[3].dispose();
        this.zaigard[4].dispose();
        this.zaigard[5].dispose();
        this.zaigard[6].dispose();
        this.zaigard[7].dispose();
        progresbar.dispose();
    }
}
