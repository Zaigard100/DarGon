package com.zaig100.dg.screen.game;

import static jdk.nashorn.internal.objects.Global.print;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zaig100.dg.Main;
import com.zaig100.dg.screen.MenuScreen;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Font;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.LevelRead;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.Save;
import com.zaig100.dg.utils.ShaderManager;
import com.zaig100.dg.utils.dgscript.ScriptStarter;
import com.zaig100.dg.world.Map;
import com.zaig100.dg.world.Player;
import com.zaig100.dg.world.World;

import java.io.IOException;
import java.util.Random;

public class LevelModScreen implements Screen {

    private FrameBuffer fbo, fbo2;
    private SpriteBatch batch;
    private OrthographicCamera cam;
    public static Main m;
    private Sprite frame;
    static LevelRead lR;
    static Font font;
    static Random random;
    private BitmapFont f1;
    private int height;
    private int width;

    private int scrW, scrH;
    private String path, line;

    private int menu = 0;
    private Save save;
    private boolean start = true;


    private boolean debag = false;
    private float exit_timer = 0, sensor_timer = 0f;

    private boolean fir1 = true;

    public Player player;
    boolean u_player = false;

    //TODO Script test;
    //ScriptStarter script = new ScriptStarter();

    private Viewport viewport;

    private boolean black_frame;

    public LevelModScreen(Main m, String path) {
        LevelModScreen.m = m;
        this.path = path;
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        batch = new SpriteBatch();
        fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
    }

    public LevelModScreen(Main m, String path, Player player) {
        LevelModScreen.m = m;
        this.path = path;
        this.player = player;
        u_player =true;
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        batch = new SpriteBatch();
        fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
    }

    @Override
    public void show() {
        random = new Random();
        cam = new OrthographicCamera(width, height);
        viewport = new ScreenViewport(cam);
        font = new Font();
        debag = Configuration.isDebug();
        f1 = new BitmapFont();
        f1.setColor(Color.WHITE);
        lR = new LevelRead(path);
        World.setMap(new Map(lR.getWight(), lR.getHeight(), lR.getMap(), true));
        World.map.object_load(lR);
        if(u_player) World.setPlayer(player);
        cam.position.set(new Vector3(width / 2, height / 2, 0));

        /*TODO Script test;

        script.load(
                "use dargon\n" +
                        "",
                this
        );
        */

        try {

            save = new Save(m, "", path);

            if (lR.isSave()) {
                Save.setHp(World.player.getHp());
                Save.setMoney(World.player.coin_count);
                save.setPath(path);
                Save.setArr(World.player.inventory.inventoryToJSON());
                save.save(save.getConf());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void render(float delta) {
        Joystick.frame((int) ((Gdx.graphics.getWidth() - 16 * 7 * Configuration.getScale()) / 2), (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2));
        if (!Joystick.isJoystick() && Gdx.input.isTouched()) {
            sensor_timer += delta;
            if (sensor_timer > 1f) {
                World.player.isPause = !World.player.isPause;
                World.player.isStop = World.player.isPause;
                World.player.inventarIsOpen = false;
                sensor_timer = 0;
            }
        } else {
            sensor_timer = 0;
        }

        fbo.begin();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        first_render();
        batch.end();
        fbo.end();

        if (World.player.getX() != lR.getSpawnX() || World.player.getY() != lR.getSpawnY()) start = false;

        frame = new Sprite(fbo.getColorBufferTexture());
        frame.setPosition((scrW - 16 * 7 * Configuration.getScale()) / 2, (-scrH + 16 * 5 * Configuration.getScale()) / 2);

        fbo2.begin();
        batch.begin();
        second_render();
        batch.end();
        fbo2.end();


        frame = new Sprite(fbo2.getColorBufferTexture());
        cam.update();

        fbo.begin();
        batch.begin();
        third_render();
        batch.end();
        fbo.end();

        frame = new Sprite(fbo.getColorBufferTexture());

        if (Configuration.getShader() != -1) {
            frame.setFlip(ShaderManager.getShaders().get(Configuration.getShader()).isFlipX(), ShaderManager.getShaders().get(Configuration.getShader()).isFlipY());
        } else {
            frame.setFlip(false, true);
        }

        batch.begin();
        frame.draw(batch);

        batch.end();

    }


    private void first_render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        World.map.render(batch);

        stair_update();

        World.map.object_update(batch);

        World.map.dark_render(batch);

        if (World.player.isShowObj) {
            World.map.show_obj(batch);
        }
        render_ui();

    }

    private void second_render() {
        batch.draw(Res.boards, 0, 0, 16 * 7 * Configuration.getScale() * 10, 16 * 5 * Configuration.getScale() * 10);
        if (World.player.isPause) {
            batch.draw(Res.pause_dark, 0, 0, 16 * 7 * Configuration.getScale() * 10, 16 * 5 * Configuration.getScale() * 10);
        }
        frame.draw(batch);
    }

    private void third_render() {
        if (Configuration.getShader() != -1) {
            ShaderManager.getShaders().get(Configuration.getShader()).shader_update();
            batch.setShader(ShaderManager.getShaders().get(Configuration.getShader()).getShader());
        }
        frame.draw(batch);
        if (Configuration.getShader() != -1) {
            batch.setShader(null);
        }
    }
    private void stair_update() {
        if (World.map.stair.size() > 0) {
            int i;
            for (i = 0; i < World.map.stair.size(); i++) {
                World.map.stair.get(i).render(batch);
                World.map.stair.get(i).frame();
                if (World.map.stair.get(i).isExit()) {
                    World.player.isShowObj = false;
                    World.player.isSheld = false;
                    World.player.isShop = false;
                    World.player.inventarIsOpen = false;
                    World.player.menu_opened = false;
                    World.player.speed = 1;
                    dispose();
                    if (World.map.stair.get(i).isEnd()) {
                        World.del();
                        m.setScreen(new MenuScreen(m));

                    } else {
                        World.del();
                        m.setScreen(new LevelModScreen(m, World.map.stair.get(i).getNext_path()));

                    }
                }
            }
        }
    }

    void debugShow() {
        line = "Debug:";
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 10);
        line = "FPS:" + Gdx.graphics.getFramesPerSecond();
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 25);
        line = "X:" + World.player.getX() + "  Y:" + World.player.getY();
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 40);
        line = "WX:" + World.player.get_wX() + "  WY:" + World.player.get_wY();
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 55);
        line = "JavaHeap:" + (((int) Gdx.app.getJavaHeap()) / (8 * 1024)) + " KB";
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 70);
        line = "NativeHeap:" + (((int) Gdx.app.getNativeHeap()) / (8 * 1024)) + " KB";
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 85);
        line = "Object Count:" + World.map.getObjCount();
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 100);
        line = "Level:" + lR.getLevelname() +"("+path+")";
        f1.draw(batch, line, 10, 5f * 16 * Configuration.getScale() - 115);
    }
    void pauseMenu() {
        batch.draw(Res.pause_dark, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Res.getFont(11).draw(batch, "Pause", 2.2f * 16 * Configuration.getScale(), 4.5f * 16 * Configuration.getScale());
        if (menu == 0) {
            Res.getFont(6).draw(batch, ">Resume<", 2.2f * 16 * Configuration.getScale(), 3.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Main menu", 2.2f * 16 * Configuration.getScale(), 2.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Load save", 2.2f * 16 * Configuration.getScale(), 1.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Exit(Hold)", 2.2f * 16 * Configuration.getScale(), 0.5f * 16 * Configuration.getScale());
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                World.player.isPause = false;
                World.player.isStop = false;
                World.player.inventarIsOpen = false;
            }
        }
        if (menu == 1) {
            Res.getFont(6).draw(batch, " Resume", 2.2f * 16 * Configuration.getScale(), 3.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, ">Main menu<", 2.2f * 16 * Configuration.getScale(), 2.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Load save", 2.2f * 16 * Configuration.getScale(), 1.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Exit(Hold)", 2.2f * 16 * Configuration.getScale(), 0.5f * 16 * Configuration.getScale());
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || (Joystick.isUse() && Gdx.input.justTouched())) {
                World.player.isPause = false;
                World.player.isStop = false;
                World.player.inventarIsOpen = false;
                World.player.isShowObj = false;
                World.player.isSheld = false;
                World.player.isShop = false;
                World.player.menu_opened = false;
                World.player.inf = false;
                World.player.speed = 1;
                World.del();
                dispose();
                m.setScreen(new MenuScreen(m));

            }
        }
        if(menu==2) {
            Res.getFont(6).draw(batch, " Resume", 2.2f * 16 * Configuration.getScale(), 3.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Main menu", 2.2f * 16 * Configuration.getScale(), 2.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, ">Load save<", 2.2f * 16 * Configuration.getScale(), 1.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Exit(Hold)", 2.2f * 16 * Configuration.getScale(), 0.5f * 16 * Configuration.getScale());
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                World.player.setHp(save.getHp());
                World.player.coin_count = Save.getMoney();
                World.player.inventory.jsonToInventory(save.getArr());
                World.player.isPause = false;
                World.player.isStop = false;
                World.player.inventarIsOpen = false;
                World.player.isShowObj = false;
                World.player.isSheld = false;
                World.player.isShop = false;
                World.player.menu_opened = false;
                World.player.inf = false;
                World.player.speed = 1;
                World.del();
                dispose();
                m.setScreen(new LevelModScreen(m, save.getsPath()));
            }

        }

        if(menu==3) {
            Res.getFont(6).draw(batch, " Resume", 2.2f * 16 * Configuration.getScale(), 3.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Main menu", 2.2f * 16 * Configuration.getScale(), 2.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, " Load save", 2.2f * 16 * Configuration.getScale(), 1.5f * 16 * Configuration.getScale());
            Res.getFont(6).draw(batch, ">Exit(Hold)<", 2.2f * 16 * Configuration.getScale(), 0.5f * 16 * Configuration.getScale());
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                exit_timer += Gdx.graphics.getDeltaTime();

                World.player.isPause = false;
                World.player.isStop = false;
                World.player.inventarIsOpen = false;
                World.player.isShowObj = false;
                World.player.isSheld = false;
                World.player.isShop = false;
                World.player.menu_opened = false;
                World.player.inf = false;
                World.player.speed = 1;
                World.del();
                Gdx.app.exit();

            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            menu--;
            Res.click[random.nextInt(2)].play(Configuration.getSound());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            menu++;
            Res.click[random.nextInt(2)].play(Configuration.getSound());
        }

        sensor_pause();

        if (menu >= 4) {
            menu = 0;
        }

        if (menu < 0) {
            menu = 3;
        }

    }
    int sy;
    private void sensor_pause() {
        if (Gdx.input.justTouched()) {
            sy = (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2);
            if ((Gdx.input.getY()-sy > 170/8 * Configuration.getScale()) && (Gdx.input.getY()-sy <= 285/8 * Configuration.getScale())) {
                if (menu != 0) {
                    menu = 0;
                } else {
                    World.player.isPause = false;
                    World.player.isStop = false;
                    World.player.inventarIsOpen = false;
                }
            }
            if ((Gdx.input.getY()-sy > 285/8 * Configuration.getScale()) && (Gdx.input.getY()-sy <= 410/8 * Configuration.getScale())) {
                if (menu != 1) {
                    menu = 1;
                } else {
                    World.player.isPause = false;
                    World.player.isStop = false;
                    World.player.inventarIsOpen = false;
                    World.player.isShowObj = false;
                    World.player.isSheld = false;
                    World.player.isShop = false;
                    World.player.menu_opened = false;
                    World.player.inf = false;
                    World.player.speed = 1;
                    World.del();
                    dispose();
                    m.setScreen(new MenuScreen(m));
                }
            }
            if ((Gdx.input.getY()-sy > 410/8 * Configuration.getScale()) && (Gdx.input.getY()-sy <= 540/8 * Configuration.getScale())) {
                if (menu != 2) {
                    menu = 2;
                } else {
                    World.player.setHp(save.getHp());
                    World.player.coin_count = Save.getMoney();
                    World.player.inventory.jsonToInventory(save.getArr());
                    World.player.isPause = false;
                    World.player.isStop = false;
                    World.player.inventarIsOpen = false;
                    World.player.isShowObj = false;
                    World.player.isSheld = false;
                    World.player.isShop = false;
                    World.player.menu_opened = false;
                    World.player.inf = false;
                    World.player.speed = 1;
                    World.del();
                    dispose();
                    m.setScreen(new LevelModScreen(m, save.getsPath()));
                }
            }
        }

        if (Gdx.input.isTouched()) {
            if ((Gdx.input.getY()-sy > 540/8 * Configuration.getScale())) {
                if (menu != 3) {
                    menu = 3;
                } else {
                    exit_timer += Gdx.graphics.getDeltaTime();
                        if (exit_timer > 1.0f) {
                            World.player.isPause = false;
                            World.player.isStop = false;
                            World.player.inventarIsOpen = false;
                            World.player.isShowObj = false;
                            World.player.isSheld = false;
                            World.player.isShop = false;
                            World.player.menu_opened = false;
                            World.player.inf = false;
                            World.player.speed = 1;
                            dispose();
                            Gdx.app.exit();
                        }
                    }
                }else {
                    exit_timer = 0;
                }
            } else {
            exit_timer = 0;
        }
    }

    private void render_ui() {

        if (!World.player.isPause) {
            World.player.render_menu(batch, Res.getFont(3));
            if (start)
                Res.getFont(6).draw(batch, lR.getLevelname(), 8 * Configuration.getScale(), 4.5f * 16 * Configuration.getScale());
            World.map.show_tablet_text(batch);
        }
        if (World.player.getHp() <= 0 && !World.player.isPause) {
            wasted_render();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (World.player.inventarIsOpen) {
                World.player.inventarIsOpen = false;
                World.player.isStop = false;
            } else if (World.player.isShop) {
                if (World.player.isPause) {
                    World.player.isPause = false;
                    World.player.isStop = false;
                } else {
                    World.player.isPause = true;
                    World.player.isStop = true;
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debag = !debag;
        }
        if (World.player.isPause) {
            pauseMenu();
        }
        Joystick.render(batch);
        if (debag) debugShow();

    }

    private void wasted_render() {
        if (fir1) Res.wasted.play(2.0f);
        fir1 = false;
        Res.getFont(11).draw(batch, "Wasted", 2 * 16 * Configuration.getScale(), 4.5f * 16 * Configuration.getScale());
        Res.getFont(6).draw(batch, "Press Space", 2 * 16 * Configuration.getScale(), 0.5f * 16 * Configuration.getScale());
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Joystick.isUse()) {
            World.player.isPause = false;
            World.player.isStop = false;
            World.player.inventarIsOpen = false;
            World.player.isShowObj = false;
            World.player.isSheld = false;
            World.player.isShop = false;
            World.player.menu_opened = false;
            World.player.inf = false;
            World.player.speed = 1;
            World.del();
            dispose();
            m.setScreen(new MenuScreen(m));

        }
    }

    @Override
    public void resize(int width, int height) {
        fbo.dispose();
        fbo2.dispose();
        int ry = height - this.height;
        int rx = width - this.width;
        this.width = width;
        this.height = height;
        scrW = width;
        scrH = height;
        viewport.update(width, height);
        cam.translate(rx / 2, ry / 2, cam.position.z);
        cam.update();
        fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
    }

    @Override
    public void pause() {
        World.player.isPause = true;
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
