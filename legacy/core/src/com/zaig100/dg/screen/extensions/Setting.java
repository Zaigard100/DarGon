package com.zaig100.dg.screen.extensions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zaig100.dg.Main;
import com.zaig100.dg.screen.MenuScreen;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Font;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.ShaderManager;

import java.io.IOException;


public class Setting implements Screen {

    Main m;
    
    double Scale;
    long Music;
    long Sound;
    boolean Sensor;
    boolean Debug;
    int Shader;

    SpriteBatch batch;

    String[] line = new String[6];
    Font font;
    BitmapFont f1;

    int num = 0;

    int getY;
    private int scrH;
    private int scrW;
    private Viewport viewport;
    private OrthographicCamera cam;

    public Setting(Main m){
        this.m = m;
        batch = new SpriteBatch();
    }

    @Override
    public void show() {


        scrH = Gdx.graphics.getHeight();
        scrW = Gdx.graphics.getWidth();

        cam = new OrthographicCamera();
        cam.translate(scrW / 2, scrW / 2, 0);
        viewport = new ScreenViewport(cam);


        Scale = Configuration.getScale();
        Music = Configuration.getMusic();
        Sound = Configuration.getSound();
        Sensor = Configuration.isSensor();
        Debug = Configuration.isDebug();
        Shader = Configuration.getShader();
        //Fullscreen = Configuration.isFulscreen();
        //Shader = Configuration.getShader();


        font = new Font();
        f1 = font.gFont(7 * Configuration.getScale(), "fonts/GFont.ttf");

    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyPressed(Input.Keys.F11)) {
            if(!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }else{
                Gdx.graphics.setWindowedMode((int) (16 * 7 * Configuration.getScale()), (int) (16 * 5 * Configuration.getScale()));
            }
        }

        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1f);
        Gdx.graphics.getGL20().glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Sensor) sensor();


        line[0] = "  Scale: x" + Scale + " (" + Configuration.getScale() + ")";
        line[1] = "  Music:" + Music + " (" + Configuration.getMusic() + ")";
        line[2] = "  Sound:" + Sound + " (" + Configuration.getSound() + ")";
        line[3] = "  Sensor:" + boolStr(Sensor) + " (" + boolStr(Configuration.isSensor()) + ")";
        line[4] = "  Debug:" + boolStr(Debug) + " (" + boolStr(Configuration.isDebug()) + ")";
        if (Configuration.getShader() != -1) {
            if (Shader != -1) {
                line[5] = "  Shader:" + ShaderManager.getShaders().get(Shader).getName() + " (" + ShaderManager.getShaders().get(Configuration.getShader()).getName() + ")";
            } else {
                line[5] = "  Shader:" + "Default" + " (" + ShaderManager.getShaders().get(Configuration.getShader()).getName() + ")";
            }
        } else {
            if (Shader == -1) {
                line[5] = "  Shader:" + "Default" + " (" + "Defaulf" + ")";
            } else {
                line[5] = "  Shader:" + ShaderManager.getShaders().get(Shader).getName() + " (" + "Default" + ")";
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) num--;
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) num++;
        if (Gdx.input.justTouched()) {
            if (getY > 228 / 8 * Configuration.getScale() && getY < 249 / 8 * Configuration.getScale()) {
                Sensor = !Sensor;
                num = 3;
            }
        }
        if (num > 5) num = 0;
        if (num < 0) num = 5;

        if (num == 0) {
            line[0] = ">Scale: x" + Scale + " (" + Configuration.getScale() + ")";

            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) Scale -= 1f;
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)) Scale += 1f;

            if (Scale < 0) Scale = 0;

        }
        if(num == 1){
            line[1] = ">Music:" + Music + " (" + Configuration.getMusic() + ")";

            if(Gdx.input.isKeyJustPressed(Input.Keys.A)) Music -= 10;
            if(Gdx.input.isKeyJustPressed(Input.Keys.D)) Music += 10;
            if(Music>100) Music = 100;
            if(Music<0) Music = 0;

        }
        if(num == 2){
            line[2] = ">Sound:" + Sound + " (" + Configuration.getSound() + ")";

            if(Gdx.input.isKeyJustPressed(Input.Keys.A)) Sound -= 10;
            if(Gdx.input.isKeyJustPressed(Input.Keys.D)) Sound += 10;
            if(Sound>100) Sound = 100;
            if(Sound<0) Sound = 0;

        }

        if(num == 3){
            line[3] = ">Sensor:" + boolStr(Sensor) + " (" + boolStr(Configuration.isSensor()) + ")";

            if(Gdx.input.isKeyJustPressed(Input.Keys.A)) Sensor =!Sensor;
            if(Gdx.input.isKeyJustPressed(Input.Keys.D)) Sensor =!Sensor;

        }
        if (num == 4) {
            line[4] = ">Debug:" + boolStr(Debug) + " (" + boolStr(Configuration.isDebug()) + ")";

            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) Debug = !Debug;
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)) Debug = !Debug;

        }

        if (num == 5) {
            if (Configuration.getShader() != -1) {
                if (Shader != -1) {
                    line[5] = ">Shader:" + ShaderManager.getShaders().get(Shader).getName() + " (" + ShaderManager.getShaders().get(Configuration.getShader()).getName() + ")";
                } else {
                    line[5] = ">Shader:" + "Default" + " (" + ShaderManager.getShaders().get(Configuration.getShader()).getName() + ")";
                }
            } else {
                if (Shader == -1) {
                    line[5] = ">Shader:" + "Default" + " (" + "Defaulf" + ")";
                } else {
                    line[5] = ">Shader:" + ShaderManager.getShaders().get(Shader).getName() + " (" + "Default" + ")";
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) Shader--;
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)) Shader++;

            if (Shader > ShaderManager.getShaders().size() - 1)
                Shader = ShaderManager.getShaders().size() - 1;
            if (Shader < -1) Shader = -1;
        }

        batch.begin();
        f1.draw(batch, "Settings :", 10, 5 * 16 * Configuration.getScale() - 10);
        f1.draw(batch, line[0], 10, 5 * 16 * Configuration.getScale() - (10 * Configuration.getScale()) - 10);
        f1.draw(batch, line[1], 10, 5 * 16 * Configuration.getScale() - (20 * Configuration.getScale()) - 10);
        f1.draw(batch, line[2], 10, 5 * 16 * Configuration.getScale() - (30 * Configuration.getScale()) - 10);
        f1.draw(batch, line[3], 10, 5 * 16 * Configuration.getScale() - (40 * Configuration.getScale()) - 10);
        f1.draw(batch, line[4], 10, 5 * 16 * Configuration.getScale() - (50 * Configuration.getScale()) - 10);
        f1.draw(batch, line[5], 10, 5 * 16 * Configuration.getScale() - (60 * Configuration.getScale()) - 10);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) exit();

    }

    void sensor(){
        if(Gdx.input.justTouched()) {
            System.out.println(Gdx.graphics.getHeight() - Gdx.input.getY());
            getY = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (getY > 564 / 8 * Configuration.getScale()) {
                exit();
            }
            if (Gdx.input.getX() < 596 / 8 * Configuration.getScale()) {
                if (getY > 479 / 8 * Configuration.getScale() && getY < 564 / 8 * Configuration.getScale()) {
                    num = 0;
                    Scale -= 1f;
                }
                if (getY > 398 / 8 * Configuration.getScale() && getY < 479 / 8 * Configuration.getScale()) {
                    num = 1;
                    Music -= 10;
                }
                if (getY > 319 / 8 * Configuration.getScale() && getY < 398 / 8 * Configuration.getScale()) {
                    num = 2;
                    Sound -= 10;
                }
                if (getY > 77 / 8 * Configuration.getScale() && getY < 149 / 8 * Configuration.getScale()) {
                    Shader -= 1;
                    num = 5;
                }
            }
            if (Gdx.input.getX() > 596 / 8 * Configuration.getScale()) {
                if (getY > 479 / 8 * Configuration.getScale() && getY < 564 / 8 * Configuration.getScale()) {
                    num = 0;
                    Scale += 1f;
                }
                if (getY > 398 / 8 * Configuration.getScale() && getY < 479 / 8 * Configuration.getScale()) {
                    num = 1;
                    Music += 10;
                }
                if (getY > 319 / 8 * Configuration.getScale() && getY < 398 / 8 * Configuration.getScale()) {
                    num = 2;
                    Sound += 10;
                }
                if (getY > 77 / 8 * Configuration.getScale() && getY < 149 / 8 * Configuration.getScale()) {
                    Shader += 1;
                    num = 5;
                }
            }
            if (getY > 149 / 8 * Configuration.getScale() && getY < 228 / 8 * Configuration.getScale()) {
                Debug = !Debug;
                num = 4;
            }

        }
        if (Shader > ShaderManager.getShaders().size() - 1)
            Shader = ShaderManager.getShaders().size() - 1;
        if (Shader < -1) Shader = -1;
    }

    void exit() {
        if(Configuration.jsonObject == null){
            try {
                Configuration.load(m);
                System.out.println("JO is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (Scale == Configuration.getScale()) {
            Configuration.setMusic((int) Music);
            Configuration.setSound((int) Sound);
            Configuration.setSensor(Sensor);
            Configuration.setDebug(Debug);
            Configuration.setShader(Shader);
            Configuration.save(Configuration.getConf());
            m.setScreen(new MenuScreen(m));
        } else {
            Configuration.setMusic((int) Music);
            Configuration.setSound((int) Sound);
            Configuration.setSensor(Sensor);
            Configuration.setDebug(Debug);
            Configuration.setScale((float) Scale);
            Configuration.setShader(Shader);
            Configuration.save(Configuration.getConf());
            f1 = new Font().gFont((float) (8 * Scale), "fonts/GFont.ttf");
            if (!Gdx.graphics.isFullscreen() && !m.isAndroid()) {
                Gdx.graphics.setWindowedMode((int) (16 * 7 * Scale), (int) (16 * 5 * Scale));
            }
            Res.cleanFont();
            dispose();
            m.setScreen(new MenuScreen(m));

        }

    }

    String boolStr(boolean bool){
        if(bool){
            return "T";
        }else{
            return "F";
        }
    }

    @Override
    public void resize(int width, int height) {

        int ry = height - this.scrH;
        int rx = width - this.scrW;
        scrW = width;
        scrH = height;
        viewport.update(width, height);
        cam.translate(rx/2, ry/2,0);
        cam.update();
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
        f1.dispose();
        font.dispose();
        batch.dispose();
    }
}
