package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.contain.ShopC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.elements.items.Item;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

public class Shop extends Obj {

    Item[] item;
    int[] cost;

    int pos = 0, lastPos = 0;
    int scrX, scrY;


    public Shop(ShopC contain){
        super(contain.getX(),contain.getY(), contain.getTag());
        type = ObjType.SHOP;
        item = contain.getItems();
        cost = contain.getCost();
        this.contain = contain;
    }

    public void load(ShopC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.SHOP;
        item = contain.getItems();
        cost = contain.getCost();
        this.contain = contain;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(
                Res.shop,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );

    }

    @Override
    public void frame() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || (Joystick.isUse() && Gdx.input.justTouched())) {
            if ((World.player.getX() - 1 == x || World.player.getX() == x || World.player.getX() + 1 == x) && (World.player.getY() - 1 == y || World.player.getY() == y || World.player.getY() + 1 == y)) {
                if (!((World.player.getX() == x && World.player.getY() == y))) {
                    if (!World.player.isShop) {
                        World.map.setShop(this);
                        World.player.isStop = true;
                        World.player.menu_opened = true;
                        pos = 99;
                    }
                }
            }
        }
    }

    @Override
    public void show_obj(SpriteBatch batch) {
        batch.draw(
                Res.green_obj,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
    }

    public void render_shop(SpriteBatch batch) {
        batch.draw(
                Res.shop_ui,
                8 * Configuration.getScale(),
                36 * Configuration.getScale(),
                64 * Configuration.getScale(),
                36 * Configuration.getScale()
        );

        for (int i = 0; i < 3; i++) {
            item[i].render(batch, i, 2);
            batch.draw(
                    Res.item_frame,
                    (pos + 1) * 16 * Configuration.getScale(),
                    3 * 16 * Configuration.getScale(),
                    16 * Configuration.getScale(),
                    16 * Configuration.getScale()
            );
            batch.draw(
                    Res.x,
                    3.5f * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    2 * 16 * Configuration.getScale() + 19 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale(),
                    16 * Configuration.getScale() - 6 * Configuration.getScale()
            );
            batch.draw(
                    Res.money,
                    i * 16 * Configuration.getScale() + 16 * Configuration.getScale(),
                    40 * Configuration.getScale(),
                    5 * Configuration.getScale(),
                    5 * Configuration.getScale()
            );
            Res.getFont(3).draw(
                    batch,
                    String.valueOf(cost[i]),
                    i * 16 * Configuration.getScale() + 22 * Configuration.getScale(),
                    44 * Configuration.getScale()
            );
        }

    }

    public void frame_shop() {
        sensor((int) ((Gdx.graphics.getWidth() - 16 * 7 * Configuration.getScale()) / 2), (int) ((Gdx.graphics.getHeight() - 16 * 5 * Configuration.getScale()) / 2));

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            pos--;
            if (pos < 0) {
                pos = 2;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            pos++;
            if ((pos > 2)) {
                pos = 0;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (World.player.isShop) {
                World.player.isShop = false;
                World.player.isStop = false;
            }
            pos = 99;//ЧЗХ
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (pos >= 0 && pos <= 2) {
                if (cost[pos] <= World.player.coin_count) {
                    World.player.coin_count -= cost[pos];
                    World.player.inventory.set(Item.jsonToItem(item[pos].toJson()));
                }
            }
            item =  ((ShopC)contain).getItems();
        }

    }

    void sensor(int sx, int sy) {
        if (Gdx.input.justTouched()) {

            scrX = (int) (Gdx.input.getX() - (sx + 16 * Configuration.getScale()));
            scrY = (int) ((Gdx.graphics.getHeight() - Gdx.input.getY()) - (sy + 16 * Configuration.getScale()));

            if ((2 * 16 * Configuration.getScale() <= scrY) && (3 * 16 * Configuration.getScale() >= scrY)) {
                for (int i = 0; i < 3; i++) {
                    if (i * 16 * Configuration.getScale() <= scrX && (i + 1) * 16 * Configuration.getScale() >= scrX) {
                        lastPos = i;
                    }
                } /// Pos
                if ((3.5f * 16 * Configuration.getScale() <= scrX) && (4.5f * 16 * Configuration.getScale() >= scrX)) {
                    if (World.player.isShop) {
                        World.player.isShop = false;
                        World.player.isStop = false;
                    }
                    pos = 99;
                }

                if (lastPos == pos) {
                    if (cost[pos] <= World.player.coin_count) {
                        World.player.coin_count -= cost[pos];
                        World.player.inventory.set(Item.jsonToItem(item[pos].toJson()));
                    }
                    item =  ((ShopC)contain).getItems();
                } else {
                    pos = lastPos;
                }
            }
        }
    }

    @Override
    public void setVal(String name, Value val) {
        switch (name) {
            case "X":
            case "x":
                x = val.asInt();
                break;
            case "Y":
            case "y":
                y = val.asInt();
                break;
        }
    }

    @Override
    public Value getVal(String name) {
        switch (name) {
            case "X":
            case "x":
                return new NumberVal(x);
            case "Y":
            case "y":
                return new NumberVal(y);
        }
        return null;
    }

    @Override
    public void tag_activate(String func) {
        switch (func.split(">")[0]) {
            case "X":
                if (func.split(">")[1] == "++") {
                    x++;
                } else if (func.split(">")[1] == "--") {
                    x--;
                } else {
                    x = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "Y":
                if (func.split(">")[1] == "++") {
                    y++;
                } else if (func.split(">")[1] == "--") {
                    y--;
                } else {
                    y = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "cordN":
                cordinateNormalize();
                break;
            case "del":
                del();
                break;
        }
    }

    public Item[] getItem() {
        return item;
    }

    public int[] getCost() {
        return cost;
    }
}
