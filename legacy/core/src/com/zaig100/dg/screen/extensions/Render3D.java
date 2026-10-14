package com.zaig100.dg.screen.extensions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zaig100.dg.Main;
import com.zaig100.dg.screen.MenuScreen;

public class Render3D implements Screen {
    private final Main m;
    private Viewport viewport;
    private Camera camera;

    BitmapFont f1;
    long timestart ;
    SpriteBatch batch;
    ShaderProgram shader;
    Texture wind;
    FrameBuffer fram;
    Sprite WinSpr;
    float mx,my,mx1,my1;
    float posx =-5.0f,posy=0.0f,posz=0.0f,posx1,posy1,posz1;
    boolean updata = true;

    float dirx=0.0f,diry=0.0f,dirz=0.0f;
    float dirTempx=0.0f,dirTempy=0.0f,dirTempz=0.0f;
    int w = 1300,h = 650;

    int atx=0,aty=0;

    public Render3D(Main m) {
        this.m = m;
        batch = new SpriteBatch();
    }

    @Override
    public void show() {
        f1 = new BitmapFont();
        f1.setColor(Color.BLACK);
        camera = new OrthographicCamera();
        w = Gdx.graphics.getWidth();
        h = Gdx.graphics.getHeight();
        camera.translate(w/2, h/2, camera.position.z);
        viewport = new ScreenViewport(camera);

        timestart = TimeUtils.millis();
        WinSpr = new Sprite();
        fram = new FrameBuffer(Format.RGB888,w,h,false);
        wind = new Texture(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),Format.RGBA8888);
        //wind.bind(0);
        ShaderProgram.pedantic=false;
        shader = new ShaderProgram(Gdx.files.internal("shaders/default.vert"),
                (Gdx.files.internal("shaders/rayCastShader.glsl")));
        if (!shader.isCompiled()) {
            System.err.println(shader.getLog());
            System.exit(0);
        }else {
            System.out.println( "Sucsessfull");
        }
        Gdx.input.setCatchBackKey(true);

    }

    @Override
    public void dispose() {
        batch.dispose();
        f1.dispose();
        wind.dispose();
        fram.dispose();
    }

    @Override
    public void pause() {

    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyPressed(Input.Keys.F11)) {
            if(!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }else{
                Gdx.graphics.setWindowedMode(1300, 650);
            }
        }
        dirx=0.0f;
        diry=0.0f;
        dirz=0.0f;
        if(!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            if (!m.isAndroid()) {
                mx += (((float) Gdx.input.getX() - w / 2) / w);
                my += (((float) Gdx.input.getY() - h / 2) / h);

                if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    Gdx.input.setCursorCatched(false);
                }else {
                    Gdx.input.setCursorCatched(true);
                    Gdx.input.setCursorPosition(w/2, h/2);
                }

            }else{
                if(Gdx.input.isTouched()){
                    mx += (Gdx.input.getDeltaX())*delta*0.5;
                    my += (Gdx.input.getDeltaY())*delta*0.5;
                }
            }
        }

        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            dirx=1.0f;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            dirx=-1.0f;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            diry=1.0f;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            diry=-1.0f;
        }



        if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) posz += 1.0f*0.1f;
        if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) posz -= 1.0f*0.1f;

        dirTempz = (float) (dirz * Math.cos(-my) -dirx * Math.sin(-my));
        dirTempx = (float) (dirz * Math.sin(-my) +dirx * Math.cos(-my));
        dirTempy =  diry ;
        dirx = (float) (dirTempx * Math.cos(mx) - dirTempy * Math.sin(mx))*0.2f;
        diry = (float) (dirTempx * Math.sin(mx) + dirTempy * Math.cos(mx))*0.2f;
        dirz = dirTempz*0.2f;
        posx = posx + dirx;
        posy = posy + diry;
        posz = posz + dirz;



        if((posx == posx1)&&(posy == posy1)&&(posz == posz1)&&(mx == mx1)&&(my == my1)) {
            updata = false;
        }else {
            updata = true;
            posx1 = posx;
            posy1 = posy;
            posz1 = posz;
            mx1 = mx;
            my1 = my;
        }

        if(updata) {
            fr();
        }
        WinSpr.flip(false, false);

        batch.begin();
        batch.draw(WinSpr, 0, 0,w,h);
        f1.draw(batch, String.valueOf(Gdx.graphics.getFramesPerSecond()), 10,h-20);
        batch.end();

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||Gdx.input.isKeyPressed(Input.Keys.BACK)){
            dispose();
            m.setScreen(new MenuScreen(m));
            Gdx.input.setCursorCatched(false);
            Gdx.input.setCatchBackKey(false);
        }
    }

    @Override
    public void resize(int w, int h) {
        int ry = h - this.h;
        int rx = w - this.w;
        this.w = w;
        this.h = h;
        viewport.update(w, h);
        camera.translate(rx/2, ry/2, camera.position.z);
        camera.update();
        fram = new FrameBuffer(Format.RGB888,w,h,false);
        fr();
    }

    void fr() {
        shader.begin();
        shader.setUniformf("time",(TimeUtils.millis()-timestart)/1000.f);
        shader.setUniformf("resolution", new Vector2(w,h));
        shader.setUniformf("mouse", new Vector2(mx,my));
        shader.setUniformf("campos", new Vector3(posx,posy,posz));
        shader.end();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        fram.begin();
        batch.setProjectionMatrix(camera.combined);
        batch.setShader(shader);
        batch.begin();
        batch.draw(wind, 0, 0,w,h);
        batch.end();
        batch.setShader(null);
        fram.end();
        //System.out.println("Update");

        WinSpr = new Sprite(fram.getColorBufferTexture());
    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }
}
