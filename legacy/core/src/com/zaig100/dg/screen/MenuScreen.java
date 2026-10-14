package com.zaig100.dg.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zaig100.dg.Main;
import com.zaig100.dg.world.Player;
import com.zaig100.dg.world.elements.items.EasterEgg;
import com.zaig100.dg.world.elements.items.Money;
import com.zaig100.dg.world.elements.items.Poition;
import com.zaig100.dg.world.elements.items.Sheld;
import com.zaig100.dg.world.elements.items.SlowdownPoition;
import com.zaig100.dg.world.elements.items.SpeedPoition;
import com.zaig100.dg.world.elements.items.Torch;
import com.zaig100.dg.screen.extensions.Extension;
import com.zaig100.dg.screen.extensions.Render3D;
import com.zaig100.dg.screen.extensions.Setting;
import com.zaig100.dg.screen.game.LevelModScreen;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.LevelRead;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.ShaderManager;
import com.zaig100.dg.world.World;
import com.zaig100.dg.world.elements.items.TrapShowPotion;

import java.util.Random;

public class MenuScreen implements Screen {
    FrameBuffer fbo, fbo2;
    SpriteBatch batch;
    OrthographicCamera cam;
    Main m;
    Sprite frame;
    LevelRead lR;
    int height;
    int width;
    int scrW, scrH;
    Viewport viewport;
    int[] map;
    int wW, hW;
    int ry, rx;
    Random random = new Random();
    int tick = 0;
    int mX, mY;
    boolean b_Play = false;
    boolean b_Settings = false;
    boolean b_Tutorial = false;
    boolean isExit = false;
    int button = 0;
    private int stage;
    private float timer;
    Player player;

    public MenuScreen(Main m) {
        this.m = m;
        batch = new SpriteBatch();
        lR = new LevelRead("levels/01.json");
        map = lR.getMap();
        wW = lR.getWight();
        hW = lR.getHeight();
        player = new Player(0,0);
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        ShaderManager.load(m.isAndroid(), m);
    }

    public void show() {

        scrH = height;
        scrW = width;
        cam = new OrthographicCamera();
        cam.translate(width/2, height/2, 0);
        viewport = new ScreenViewport(cam);

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyPressed(Input.Keys.F11)) {
            if (!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                Gdx.graphics.setWindowedMode((int) (16 * 7 * Configuration.getScale()), (int) (16 * 5 * Configuration.getScale()));
            }
        }

        fbo.begin();
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        backgraund();
        button_draw();
        logo_draw();
        sensor((int) (scrW - 16 * 7 * Configuration.getScale()), (int) (scrH - 16 * 5 * Configuration.getScale()));
        batch.end();
        fbo.end();

        frame = new Sprite(fbo.getColorBufferTexture());


        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (isExit) {
            switch (button) {
                case 1:
                    dispose();
                    m.setScreen(new Extension(m));
                    break;
                case 2:
                    dispose();
                    player.setHp(4);
                    player.inventory.clear();
                    player.inventory.set(new Poition(3));
                    player.inventory.set(new Sheld(2));
                    player.inventory.set(new Torch(1));
                    player.coin_count = 0;
                    m.setScreen(new LevelModScreen(m, "levels/01.json",player));
                    break;
                case 3:
                    dispose();
                    m.setScreen(new Setting(m));
                    break;
            }
        }

        fbo2.begin();
        batch.begin();
        batch.draw(Res.boards, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        frame.setPosition((scrW - 16 * 7 * Configuration.getScale()) / 2, -(scrH - 16 * 5 * Configuration.getScale()) / 2);
        frame.draw(batch);

        batch.end();
        fbo2.end();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        frame = new Sprite(fbo2.getColorBufferTexture());
        frame.setPosition(0, 0);


        batch.begin();

        frame.draw(batch);

        batch.end();
    }

    private void backgraund() {
        batch.draw(Res.boards, 7 * 16 * Configuration.getScale(), 0, width, height);
        batch.draw(Res.boards, 0, 5 * 16 * Configuration.getScale(), width * 2, height);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (map[j + wW * i] == 11) {
                    batch.draw(Res.tile(0, 0), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 12) {
                    batch.draw(Res.tile(1, 1), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 13) {
                    batch.draw(Res.tile(2, 0), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 14) {
                    batch.draw(Res.tile(1, 0), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 15) {
                    batch.draw(Res.tile(2, 1), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 16) {
                    batch.draw(Res.tile(3, 0), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
                if (map[j + wW * i] == 18) {
                    batch.draw(Res.tile(4, 1), (j - 2) * 16 * 8 / 8 * Configuration.getScale(), (i - 4) * 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale(), 16 * 8 / 8 * Configuration.getScale());//backgraund
                }
            }
        }
        tick(0.2f);
        batch.draw(Res.hero(true, false, stage), 6 * 16 * Configuration.getScale(), 3 * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
        batch.draw(Res.nextlv, 6 * 16 * Configuration.getScale(), 0, 16 * Configuration.getScale(), 16 * Configuration.getScale());

    }

    public void tick(float second) {
        timer += Gdx.graphics.getDeltaTime();
        if (timer >= second) {
            if (stage > 4) {
                stage = 0;
            }
            stage++;
            timer = 0;
        }
    }

    private void button_draw() {
        if (b_Play) {
            batch.draw(Res.play_button_toched, 7 * 16 * 4 / 8 * Configuration.getScale() - 32 * 4 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 32 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 32 * 8 / 8 * Configuration.getScale(), 32 * 8 / 8 * Configuration.getScale());


            if (tick < 120) {
                b_Tutorial = false;
                b_Play = false;
                b_Settings = false;
                isExit = true;
                button = 2;
                player.setHp(4);
                player.inventory.clear();
                player.inventory.set(new Poition(3));
                player.inventory.set(new Sheld(2));
                player.inventory.set(new Torch(1));
                player.coin_count = 0;
                player.wCordNormalize();
                m.setScreen(new LevelModScreen(m, "levels/01.json",player));
            } else {
                tick++;
            }

        } else {
            batch.draw(Res.play_button, 7 * 16 * 4 / 8 * Configuration.getScale() - 32 * 4 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 32 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 32 * 8 / 8 * Configuration.getScale(), 32 * 8 / 8 * Configuration.getScale());//play button
        }
        /*
        if (b_Tutorial) {
            batch.draw(Res.tutorial_button_toched, 14 * 8 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 21 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale());
            if (tick < 120) {
                b_Tutorial = false;
                b_Play = false;
                b_Settings = false;
                isExit = true;
                button = 1;

            } else {
                tick++;
            }
        } else
            batch.draw(Res.tutorial_button, 14 * 8 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 21 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale());//tutorial
        */
        if (b_Settings) {
            batch.draw(Res.settings_button_toched, 7 * 16 * 8 / 8 * Configuration.getScale() - 21 * 8 / 8 * Configuration.getScale() - 14 * 8 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 21 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale());
            if (tick < 120) {
                b_Tutorial = false;
                b_Play = false;
                b_Settings = false;
                isExit = true;
                button = 3;
            } else {
                tick++;
            }
        } else {
            batch.draw(Res.settings_button, 7 * 16 * 8 / 8 * Configuration.getScale() - 21 * 8 / 8 * Configuration.getScale() - 14 * 8 / 8 * Configuration.getScale(), 5 * 16 * 4 / 8 * Configuration.getScale() - 21 * 4 / 8 * Configuration.getScale() - 32 * 3 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale(), 21 * 8 / 8 * Configuration.getScale());//settings
        }
    }

    private void logo_draw() {
        batch.draw(Res.logo, 12 * 16 / 8 * Configuration.getScale(), 3.85f * 16 * 8 / 8 * Configuration.getScale() - 16 * 8 / 2 / 8 * Configuration.getScale(), 408 * 1.25f / 8 * Configuration.getScale(), 125 * 1f / 8 * Configuration.getScale());
        batch.draw(Res.vk, 4 * Configuration.getScale(), 4 * Configuration.getScale(), 8 * Configuration.getScale(), 8 * Configuration.getScale());
    }

    public void sensor(int sx, int sy) {
        if (Gdx.input.justTouched()) {
            //System.out.println(Gdx.input.getX());
            //System.out.println(Gdx.graphics.getHeight()-Gdx.input.getY());
            //System.out.println(space);
            mX = Gdx.input.getX() - sx / 2;
            mY = Gdx.graphics.getHeight() - Gdx.input.getY() - sy / 2;

            if (mX > 4 * Configuration.getScale() && mX < 12 * Configuration.getScale()) {
                if (mY > 4 * Configuration.getScale() && mY < 12 * Configuration.getScale()) {
                    Gdx.net.openURI("https://vk.com/dargonzaig100");
                }
            }

            if (mX > 6 * 16 * Configuration.getScale() && mX < 7 * 16 * Configuration.getScale()) {
                if (mY > 0 && mY < 16 * Configuration.getScale()) {
                    Gdx.app.exit();
                }
            }
            if (mX > 0 && mX < 16 * Configuration.getScale()) {
                if (mY > 4 * 16 * Configuration.getScale() && mY < 5 * 16 * Configuration.getScale()) {
                    if ((Configuration.getScale() == 6.0f) && (Configuration.getMusic() == 10) && (Configuration.getSound() == 80)) {
                        m.setScreen(new Render3D(m));
                    }
                    if ((Configuration.getMusic() == 20) && (Configuration.getSound() == 50)) {
                        player.setHp(4);
                        player.inventory.clear();
                        player.inventory.set(new Poition(99));
                        player.inventory.set(new Sheld(99));
                        player.inventory.set(new Torch(99));
                        player.coin_count = 0;
                        player.setPosition(2,2);
                        player.wCordNormalize();
                        m.setScreen(new LevelModScreen(m, "levels/Test.json",player));
                    }
                }
            }
            if (mX > 6 * 16 * Configuration.getScale() && mX < 7 * 16 * Configuration.getScale()) {
                if (mY > 3 * 16 * Configuration.getScale() && mY < 4 * 16 * Configuration.getScale()) {

                    if ((Configuration.getMusic() == 0) && (Configuration.getSound() == 0)) {
                        player.setHp(4);
                        player.inventory.clear();
                        player.inventory.set(new Poition(99));
                        player.inventory.set(new Sheld(99));
                        player.inventory.set(new Torch(99));
                        player.inventory.set(new SlowdownPoition(99,2));
                        player.inventory.set(new SpeedPoition(99,2));
                        player.inventory.set(new Money(9999));
                        player.inventory.set(new TrapShowPotion(99));
                        player.inventory.set(new EasterEgg("4601025111298"));
                        player.inventory.set(new EasterEgg("jokkeer_lovushcera"));
                        player.inventory.set(new EasterEgg("inf"));
                        player.coin_count = 0;
                        player.setPosition(0,6);
                        player.wCordNormalize();
                        m.setScreen(new LevelModScreen(m, "levels/AllLevels.json",player));
                    }
                }
            }
            /*
            if ((mY > 109 / 6 * Configuration.getScale()) && (mY < 239 / 6 * Configuration.getScale())) {
                if ((mX > 116 / 8 * Configuration.getScale()) && (mX < 276 / 8 * Configuration.getScale())) {
                    tick = 0;
                    b_Tutorial = true;
                    Res.click[random.nextInt(2)].play(Configuration.getSound());
                    //isPres =true;
                }
            }
            */

            if ((mY > 75 / 6 * Configuration.getScale()) && (mY < 256 / 6 * Configuration.getScale())) {
                if ((mX < 576 / 8 * Configuration.getScale() * 1) && (mX > 320 / 8 * Configuration.getScale() * 1)) {
                    tick = 0;
                    b_Play = true;
                    Res.click[random.nextInt(2)].play(Configuration.getSound());
                    // isPres =true;
                }
            }
            if ((mY > 109 / 6 * Configuration.getScale() * 1) && (mY < 239 / 6 * Configuration.getScale() * 1)) {
                if ((mX > 616 / 8 * Configuration.getScale() * 1) && (mX < 789 / 8 * Configuration.getScale() * 1)) {
                    tick = 0;
                    b_Settings = true;
                    Res.click[random.nextInt(2)].play(Configuration.getSound());
                    // isPres =true;
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        fbo.dispose();
        fbo2.dispose();
        ry = height - scrH;
        rx = width - scrW;
        scrW = width;
        scrH = height;
        viewport.update(width, height);
        cam.translate(rx / 2, ry / 2, 0);
        cam.update();
        fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
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
        fbo.dispose();
        fbo2.dispose();
    }
}
