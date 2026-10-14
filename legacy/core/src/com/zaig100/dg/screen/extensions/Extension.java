package com.zaig100.dg.screen.extensions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.Main;
import com.zaig100.dg.screen.MenuScreen;
import com.zaig100.dg.screen.game.LevelModScreen;
import com.zaig100.dg.utils.Font;
import com.zaig100.dg.utils.PackManager;
import com.zaig100.dg.world.World;

import org.json.simple.JSONArray;

public class Extension implements Screen {

    Main m;
    PackManager pM;

    Font font;
    BitmapFont f1;

    SpriteBatch batch;

    int num = 0;

    public Extension(Main m){
        this.m = m;
        batch = new SpriteBatch();
    }

    @Override
    public void show() {

        font = new Font();

        pM = new PackManager(m.isAndroid(),m);

        f1 = font.gFont(8 * Main.getConfiguration().getScale(), "fonts/GFont.ttf");

        Gdx.input.setCatchBackKey(true);

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||Gdx.input.isKeyJustPressed(Input.Keys.BACK)){
            dispose();
            m.setScreen(new MenuScreen(m));
            Gdx.input.setCatchBackKey(false);
        }

        if(pM.getList().size()>0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                num++;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                num--;
            }
            if (Main.getConfiguration().isSensor()) {
                sensor();
            }
            if (num < 0) {
                num = pM.getList().size() - 1;
            }
            if (num > (pM.getList().size()) - 1) {
                num = 0;
            }
        }
        batch.begin();
            if(pM.getList().size()>0) {
                f1.draw(batch, "Pack: " + pM.getList().get(num).get("PackName"), 10, Gdx.graphics.getHeight() - 5 * Main.getConfiguration().getScale());
                f1.draw(batch, "Author: " + pM.getList().get(num).get("Author"), 10, Gdx.graphics.getHeight() - 15 * Main.getConfiguration().getScale());
            }else{
                f1.draw(batch,"Packs No Found", Gdx.graphics.getWidth()/2-42*Main.getConfiguration().getScale(),Gdx.graphics.getHeight()/2);
            }
        batch.end();
        if(pM.getList().size()>0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                //m.setScreen(new LevelModScreen(m, (String) pM.getList().get(num).get("FirstLevel"), true, (String) pM.getList().get(num).get("PackName"), pM.getDestory().getPath()));
                World.player.inventory.jsonToInventory((JSONArray) pM.getList().get(num).get("Inventory"));
            }
        }
    }

    void sensor(){
        if(Gdx.input.justTouched()){
            if(Gdx.input.getY()<3*16*Main.getConfiguration().getScale()){
                if(Gdx.input.getX()<3.5*16*Main.getConfiguration().getScale()){
                    num--;
                }
                if(Gdx.input.getX()>3.5*16*Main.getConfiguration().getScale()){
                    num++;
                }
            }else {
                //m.setScreen(new LevelModScreen(m, (String) pM.getList().get(num).get("FirstLevel"), true, (String) pM.getList().get(num).get("PackName"), pM.getDestory().getPath()));
                World.player.inventory.jsonToInventory((JSONArray) pM.getList().get(num).get("Inventory"));
            }
        }
    }

    @Override
    public void resize(int width, int height) {

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
        f1.dispose();
        font.dispose();
    }
}
