package com.zaig100.dg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.world.elements.Inventory;
import com.zaig100.dg.world.elements.items.Poition;
import com.zaig100.dg.world.elements.items.Sheld;
import com.zaig100.dg.world.elements.items.Torch;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.ai.way.Coordinate;
import com.zaig100.dg.utils.ai.way.MainWay;
import com.zaig100.dg.utils.ai.way.StartWay;
import com.zaig100.dg.world.objects.Obj;

import java.util.Random;

public class Player {

    int x;
    int y;
    int oldX;
    int oldY;
    double frameStepDist = 0f;
    public int wX;
    public int wY;
    public int hp = 4;
    public Inventory inventory = new Inventory();
    int stage = 0;
    float timer = 0;
    boolean walked = false, walked_anim = false, flip = false;
    int sx, sy;
    int wasted_id = 0;
    public float speed = 1;

    boolean[] slots = new boolean[2];
    public boolean isSheld = false;
    public boolean isShowObj = false, isShop = false, inf = false;

    Random random = new Random();

    float damgeScr = 100;
    private int getYP;
    public int coin_count;

    public boolean isPause = false, isStop = false, inventarIsOpen = false, menu_opened = false;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.oldX = x;
        this.oldY = y;
        this.wX = (int) (x * 16 * Configuration.getScale());
        this.wY = (int) (y * 16 * Configuration.getScale());

        coin_count = 0;
    }

    public Player(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.oldX = x;
        this.oldY = y;
        this.wX = x * 16 * (int) Configuration.getScale();
        this.wY = y * 16 * (int) Configuration.getScale();
        coin_count = 0;
        this.hp = hp;

    }

    public void render(SpriteBatch batch) {
        if (getHp() > 0) {
            batch.draw(
                    Res.hero(flip, walked_anim, stage),
                    16 * Configuration.getScale() * 3,
                    16 * Configuration.getScale() * 2,
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
            if (isSheld) {
                if (flip) {
                    batch.draw(
                            Res.sheld,
                            16 * Configuration.getScale() * 3.25f,
                            16 * Configuration.getScale() * 2,
                            16 * Configuration.getScale() * 0.75f,
                            16 * Configuration.getScale() * 0.75f
                    );
                } else {
                    batch.draw(
                            Res.sheld,
                            16 * Configuration.getScale() * 3,
                            16 * Configuration.getScale() * 2,
                            16 * Configuration.getScale() * 0.75f,
                            16 * Configuration.getScale() * 0.75f
                    );
                }
            }
        } else {

            //wasted_id == -1: Texture = null!!

            if (wasted_id == 0) {
                batch.draw(
                        Res.hero(flip, walked_anim, stage),
                        16 * Configuration.getScale() * 3,
                        16 * Configuration.getScale() * 2,
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale()
                );
            }
            if (wasted_id == 1) {
                batch.draw(
                        Res.amonghero,
                        16 * Configuration.getScale() * 3,
                        16 * Configuration.getScale() * 2,
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale());
            }
            if (wasted_id == 2) {
                batch.draw(
                        Res.firedhero,
                        16 * Configuration.getScale() * 3,
                        16 * Configuration.getScale() * 2,
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale());
            }
            if (wasted_id == 3) {
                batch.draw(
                        Res.shotedhero,
                        16 * Configuration.getScale() * 3,
                        16 * Configuration.getScale() * 2,
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale());
            }
            if (wasted_id == 4) {
                batch.draw(
                        Res.spikedhero,
                        16 * Configuration.getScale() * 3,
                        16 * Configuration.getScale() * 2,
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale());
            }

        }
        if (damgeScr < 0.5f) {
            batch.draw(
                    Res.damage,
                    0,
                    0,
                    112 * Configuration.getScale(),
                    80 * Configuration.getScale()
            );
            damgeScr = damgeScr + Gdx.graphics.getDeltaTime();
        }

    }


    public void render_menu(SpriteBatch batch, BitmapFont font) {
        batch.draw(
                Res.menu,
                6 * 16 * Configuration.getScale() + 2 * Configuration.getScale(),
                4 * 16 * Configuration.getScale() + 2 * Configuration.getScale(),
                12 * Configuration.getScale(),
                12 * Configuration.getScale()
        );
        if (menu_opened) {
            menu_use();
            if (hp > 0) {
                batch.draw(
                        Res.HP(hp),
                        6 * 16 * Configuration.getScale(),
                        3 * 16 * Configuration.getScale(),
                        16 * Configuration.getScale(),
                        16 * Configuration.getScale());
            }

            batch.draw(
                    Res.money,
                    6 * 16 * Configuration.getScale(),
                    2 * 16 * Configuration.getScale() + 9 * Configuration.getScale(),
                    6 * Configuration.getScale(),
                    6 * Configuration.getScale()
            );

            Res.getFont(3).draw(
                    batch,
                    String.valueOf(coin_count),
                    6 * 16 * Configuration.getScale() + 7 * Configuration.getScale(),
                    2 * 16 * Configuration.getScale() + 14 * Configuration.getScale()
            );

            batch.draw(
                    Res.bag,
                    6 * 16 * Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );

            batch.draw(
                    Res.pause,
                    6 * 16 * Configuration.getScale() + 2 * Configuration.getScale(),
                    2 * Configuration.getScale(),
                    12 * Configuration.getScale(),
                    12 * Configuration.getScale()
            );

        }

        if (inventarIsOpen) {
            inventory.frame();
            inventory.render(batch);
        }
        if (isShop) {
            World.map.shop.render_shop(batch);
            World.map.shop.frame_shop();
        }
    }


     public void frame() {
        if (hp > 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) || Joystick.isBag()) {
                Joystick.setUse(false);
                menu_opened = !menu_opened;
            }
            if ((Gdx.input.isKeyPressed(Input.Keys.W)) || (Joystick.isUp())) {
                if (!walked) {
                    oldY = y + 1;
                    // System.out.println("W");
                }
            }
            if ((Gdx.input.isKeyPressed(Input.Keys.S)) || (Joystick.isDown())) {
                if (!walked) {
                    oldY = y - 1;
                    //System.out.println("S");
                }
            }
            if ((Gdx.input.isKeyPressed(Input.Keys.A)) || (Joystick.isLeft())) {
                if (!walked) {
                    oldX = x - 1;
                    // System.out.println("A");
                }

            }
            if ((Gdx.input.isKeyPressed(Input.Keys.D)) || (Joystick.isRight())) {
                if (!walked) {
                    oldX = x + 1;
                    // System.out.println("D");
                }
            }
        }

         if ((Gdx.input.isKeyJustPressed(Input.Keys.ALT_RIGHT))) {
             if (!walked) {
                 for(Obj stair:World.map.stair){
                     System.out.println("Stair " + (World.map.stair.indexOf(stair)+1)+":");
                     MainWay way = new MainWay(x,y,stair.x,stair.y,15);
                     way.init();
                     Coordinate endwey = way.shortWay();
                     if(endwey instanceof StartWay) {
                         System.out.println(((StartWay) endwey).getWayStory());
                     }else{
                         System.out.println("No Way");
                     }
                     System.out.println();
                 }

             }
         }

        if (World.map.isGround(oldX, oldY)) {

            x = oldX;
            y = oldY;

        } else {

            oldX = x;
            oldY = y;

        }
         frameStepDist += Configuration.getScale() * Gdx.graphics.getDeltaTime() * 60 * speed;
        if(frameStepDist>1){
            if (wX != x * 16 * Configuration.getScale()) {
                if (wX > x * 16 * Configuration.getScale()) {
                    if ((wX - frameStepDist) > x * 16 * Configuration.getScale()) {
                        wX = wX - (int) frameStepDist;
                    } else {
                        wX = (int) (x * 16 * Configuration.getScale());
                    }
                    flip = true;
                }
                if (wX < x * 16 * Configuration.getScale()) {
                    if ((wX + frameStepDist) < x * 16 * Configuration.getScale()) {
                        wX = wX + (int) frameStepDist;
                    } else {
                        wX = (int) (x * 16 * Configuration.getScale());
                    }
                    flip = false;
                }

            }

            if (wY != y * 16 * Configuration.getScale()) {
                if (wY > y * 16 * Configuration.getScale()) {
                    if ((wY - frameStepDist) > y * 16 * Configuration.getScale()) {
                        wY = wY - (int) frameStepDist;
                    } else {
                        wY = (int) (y * 16 * Configuration.getScale());
                    }
                    flip = false;
                }
                if (wY < y * 16 * Configuration.getScale()) {
                    if ((wY + frameStepDist) < y * 16 * Configuration.getScale()) {
                        wY = wY + (int) frameStepDist;
                    } else {
                        wY = (int) (y * 16 * Configuration.getScale());
                    }
                }
            }
            frameStepDist = 0;
        }
        if ((wX != x * 16 * Configuration.getScale()) || (wY != y * 16 * Configuration.getScale())) {

            walked = true;
            walked_anim = true;
        } else {

            walked = false;
            walked_anim = (Gdx.input.isKeyPressed(Input.Keys.W)) || (Gdx.input.isKeyPressed(Input.Keys.S)) || (Gdx.input.isKeyPressed(Input.Keys.A)) || (Gdx.input.isKeyPressed(Input.Keys.D)) || (Joystick.isJoystick() && !Joystick.isUse());
        }
        if ((hp <= 0)) {
            walked_anim = false;
        }


    }

    public void tick(float second) {
        timer += Gdx.graphics.getDeltaTime();
        if (timer >= second/speed) {
            if (stage > 4) {
                stage = 0;
            }
            stage++;
            timer = 0;
        }
    }

    public void teleport(int tx, int ty) {
        x = tx;
        y = ty;
        oldX = x;
        oldY = y;
        wX = (int) (x * 16 * Configuration.getScale());
        wY = (int) (y * 16 * Configuration.getScale());
    }


    void menu_use() {
        sx = (int) ((Gdx.graphics.getWidth() - 16 * 7 * Configuration.getScale()) / 2);
        sy = (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2);
        slots[0] = false;
        slots[1] = false;
        if (Gdx.input.justTouched()) {
            getYP = Gdx.graphics.getHeight() - Gdx.input.getY();
            if ((Gdx.input.getX() - sx > 6 * 16 * Configuration.getScale()) && (Gdx.input.getX() - sx < 7 * 16 * Configuration.getScale())) {
                if ((getYP - sy > 1 * 16 * Configuration.getScale()) && (getYP - sy < 2 * 16 * Configuration.getScale())) {
                    Joystick.setUse(false);
                    slots[0] = true;
                }
                if ((getYP - sy > 0) && (getYP - sy < 1 * 16 * Configuration.getScale())) {
                    Joystick.setUse(false);
                    slots[1] = true;
                }
            }
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)||slots[0]) {
            inventarIsOpen = !inventarIsOpen;
            isStop = inventarIsOpen;
            isShop = false;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)||slots[1]) {
            isPause = !isPause;
            isStop = isPause;
            inventarIsOpen = false;
            isShop = false;
        }
    }


    public void setDamgeScr(float damgeScr, int id) {
        if (!inf) {
            this.damgeScr = damgeScr;
            wasted_id = id;
        }
    }



    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setPosition(int x,int y){
        this.x = x;
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public void wCordNormalize() {
        oldX = x;
        oldY = y;
        wX = (int) (x * 16 * Configuration.getScale());
        wY = (int) (y * 16 * Configuration.getScale());

    }

    public void setY(int y) {
        this.y = y;
    }

    public int get_wX() {
        return wX;
    }

    public int get_wY() {
        return wY;
    }

    public void setHp(int hp) {
        if (!inf) {
            this.hp = hp;
        } else {
            hp = 4;
        }
    }

    public int getHp() {
        return hp;
    }

    public boolean isSheld() {
        return isSheld;
    }

    public void setIsSheld(boolean sheld) {
        isSheld = sheld;
    }

}
