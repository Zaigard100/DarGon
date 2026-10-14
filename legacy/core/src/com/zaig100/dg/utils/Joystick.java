package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.world.World;


public class Joystick {

    static int getY;
    static boolean up, right, down, left, use, Sensor, bag, menu;
    static int pointX, pointY, deltaX, deltaY;


    public static void render(SpriteBatch batch) {
        if (Configuration.isSensor()) {
            batch.draw(Res.use, pointX - Configuration.getScale() - (int) ((Gdx.graphics.getWidth() - 16 * 7 * Configuration.getScale()) / 2), pointY - Configuration.getScale() - (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2), 2f * Configuration.getScale(), 2 * Configuration.getScale());
            //batch.draw(res.use, 6 * 16 * conf.getScale() - 0.8f * 16 * conf.getScale() / 2, 1.25f * 16 * conf.getScale() - 0.8f * 16 * conf.getScale() / 2, 0.8f * 16 * conf.getScale(), 0.8f * 16 * conf.getScale());
        }
    }

    public static void frame(int sx, int sy) {
        if (Configuration.isSensor()) {
            left = false;
            right = false;
            down = false;
            up = false;
            use = false;
            bag = false;
            menu = false;
            if (Gdx.input.justTouched()) {
                pointX = Gdx.input.getX();
                pointY = Gdx.graphics.getHeight() - Gdx.input.getY();
            }
            if (Gdx.input.isTouched()) {
                getY = Gdx.graphics.getHeight() - Gdx.input.getY();

                deltaX = Gdx.input.getX()-pointX;
                deltaY = getY-pointY;
                if(pointX<Gdx.graphics.getWidth()/2) {
                    if (deltaX > 8 * Configuration.getScale()) {
                        right = true;
                    }
                    if (deltaX < -8 * Configuration.getScale()) {
                        left = true;
                    }
                    if (deltaY > 8 * Configuration.getScale()) {
                        up = true;
                    }
                    if (deltaY < -8 * Configuration.getScale()) {
                        down = true;
                    }
                }
                if (Gdx.input.getX() > Gdx.graphics.getWidth() / 2) {
                    use = true;
                }
                if (Gdx.input.justTouched()) {
                    if ((Gdx.input.getX() - sx > 6 * 16 * Configuration.getScale()) && (Gdx.input.getX() - sx < 7 * 16 * Configuration.getScale())) {
                        if ((getY - sy > 4 * 16 * Configuration.getScale()) && (getY - sy < 5 * 16 * Configuration.getScale())) {
                            bag = true;
                        }
                    }
                }
                if (World.player.menu_opened && !World.player.isPause) {
                    use = false;
                }
            }
        }
    }

    public static boolean isJoystick() {
        return up || down || left || right || use;
    }

    public static boolean isBag() {
        return bag;
    }

    public static boolean isUp() {
        return up;
    }

    public static boolean isRight() {
        return right;
    }

    public static boolean isDown() {
        return down;
    }

    public static boolean isLeft() {
        return left;
    }

    public static boolean isUse() {
        return use;
    }

    public static void setUse(boolean is) {
        use = is;
    }
}
