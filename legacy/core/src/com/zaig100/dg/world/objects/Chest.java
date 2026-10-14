package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.contain.ChestC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.elements.items.Item;
import com.zaig100.dg.world.elements.items.Key;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

import java.util.ArrayList;

public class Chest extends Obj {
    ArrayList<Item> items;
    boolean open;
    boolean isLocked;
    String keyTag;


    public Chest(ChestC contain){
        super(contain.getX(), contain.getY(), contain.getTag());
        type = ObjType.CHEST;
        items = contain.getItems();
        open = contain.isOpen();
        isLocked = contain.isLoked();
        keyTag = contain.getKeyTag();
        this.contain = contain;
    }

    public void load(ChestC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        type = ObjType.CHEST;
        items = contain.getItems();
        open = contain.isOpen();
        isLocked = contain.isLoked();
        keyTag = contain.getKeyTag();
        this.contain = contain;
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
            case "open": case "Open":
                open = (val.asInt() == 1);
                break;
            case "isLocked": case "IsLocked": case "islocked":
                isLocked = (val.asInt() == 1);
                break;
            case "keyTag": case "KeyTag": case "keytag":
                keyTag = val.asString();
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
            case "open": case "Open":
                return NumberVal.fromBoolean(open);
            case "isLocked": case "IsLocked": case "islocked":
                return NumberVal.fromBoolean(isLocked);
            case "keyTag": case "KeyTag": case "keytag":
                return  new StringVal(keyTag);
        }
        return null;
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(
                Res.chest(open),
                wX - World.player.wX,
                wY - World.player.wY,
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );

        if ((World.player.getX() - 1 == x || World.player.getX() == x || World.player.getX() + 1 == x) && (World.player.getY() - 1 == y || World.player.getY() == y || World.player.getY() + 1 == y)) {
            if (isLocked) {
                Res.getFont(3).draw(
                        batch,
                        keyTag,
                        x * 16 * Configuration.getScale() - World.player.get_wX() + 3 * 16 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() - World.player.get_wY() + 7 * 16 * Configuration.getScale()
                );
            } else {
                Res.getFont(3).draw(
                        batch,
                        "open",
                        x * 16 * Configuration.getScale() - World.player.get_wX() + 3 * 16 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() - World.player.get_wY() + 7 * 16 * Configuration.getScale()
                );
            }
        }

    }

    @Override
    public void frame() {
        if ((Gdx.input.justTouched() && Joystick.isUse()) || (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            if ((World.player.getX() - 1 == x || World.player.getX() == x || World.player.getX() + 1 == x) && (World.player.getY() - 1 == y || World.player.getY() == y || World.player.getY() + 1 == y)) {
                if (!((World.player.getX() == x && World.player.getY() == y))) {
                    if (isLocked) {
                        for (Item item : World.player.inventory.items) {
                            if (item.getType().equals(Item.ItemType.KEY)) {
                                if (((Key) item).getKeyTag().equals(keyTag)) {
                                    isLocked = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        if (items != null) {
                            open = true;
                            for (Item item : items) {
                                World.player.inventory.set(item);
                            }
                            items = null;
                        }
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
            case "KeyTag":
                keyTag = func.split(">")[1];
                break;
            case "Open":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    open = !open;
                } else {
                    open = Boolean.getBoolean(func.split(">")[1]);
                }
                break;
            case "IsDoorOpen":
                if (func.split(">")[1] == "++" || func.split(">")[1] == "--") {
                    isLocked = !isLocked;
                } else {
                    isLocked = Boolean.getBoolean(func.split(">")[1]);
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
}
