package com.zaig100.dg.world.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.CrossbowC;
import com.zaig100.dg.utils.contain.DoorC;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.elements.items.Item;
import com.zaig100.dg.world.elements.items.Key;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Joystick;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.world.World;

public class Door extends Obj {

    boolean isDoorOpen;
    String keyTag;
    int facing;
    int doorTile;


    public Door(DoorC contain){
        super(contain.getX(), contain.getY(), contain.getTag());
        isDoorOpen = contain.isOpen();
        keyTag = contain.getKeyTag();
        facing = contain.getFaicing();
        type = ObjType.DOOR;
        doorTile = World.map.map[x + y * World.map.getMapWidht()];
        if (!isDoorOpen) {
            World.map.map[x + y * World.map.getMapWidht()] = 10;
        }
    }

    public void load(DoorC contain){
        x = contain.getX();
        y = contain.getY();
        tag = contain.getTag();
        doorTile = World.map.map[x + y * World.map.getMapWidht()];
        if (!isDoorOpen) {
            World.map.map[x + y * World.map.getMapWidht()] = 10;
        }
        this.contain = contain;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!isDoorOpen) {
            batch.draw(
                    Res.door(facing),
                    wX - World.player.get_wX(),
                    wY - World.player.get_wY(),
                    16 * Configuration.getScale(),
                    32 * Configuration.getScale()
            );
            if ((World.player.getX() - 1 == x || World.player.getX() == x || World.player.getX() + 1 == x) && (World.player.getY() - 1 == y || World.player.getY() == y || World.player.getY() + 1 == y)) {
                Res.getFont(3).draw(batch,
                        keyTag,
                        x * 16 * Configuration.getScale() - World.player.get_wX() + 3 * 16 * Configuration.getScale(),
                        y * 16 * Configuration.getScale() - World.player.get_wY() + 2 * 16 * Configuration.getScale()
                );
            }
        }
    }

    @Override
    public void frame() {
        if ((Gdx.input.justTouched() && Joystick.isUse()) || (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            if ((World.player.getX() - 1 == x || World.player.getX() == x || World.player.getX() + 1 == x) && (World.player.getY() - 1 == y || World.player.getY() == y || World.player.getY() + 1 == y)) {
                if (!((World.player.getX() == x && World.player.getY() == y))) {
                    for (Item item : World.player.inventory.items) {
                        if (item.getType().equals(Item.ItemType.KEY)) {
                            if (((Key) item).getKeyTag().equals(keyTag)) {
                                isDoorOpen = !isDoorOpen;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (isDoorOpen) {
            World.map.map[x + y * World.map.getMapWidht()] = doorTile;
        } else {
            World.map.map[x + y * World.map.getMapWidht()] = 10;
        }

    }

    @Override
    public void show_obj(SpriteBatch batch) {
        batch.draw(
                Res.blue_obj,
                wX - World.player.get_wX(),
                wY - World.player.get_wY(),
                16 * Configuration.getScale(),
                16 * Configuration.getScale()
        );
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
            case "Open": case "open":
                isDoorOpen = (val.asInt() == 1);
                break;
            case "keyTag": case "KeyTag": case "keytag":
                keyTag = val.asString();
                break;
            case "facing": case "Facing":
                facing = val.asInt();
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
            case "Open": case "open":
                return  NumberVal.fromBoolean(isDoorOpen);
            case "keyTag": case "KeyTag": case "keytag":
                return new StringVal(keyTag);
            case "facing": case "Facing":
                return new NumberVal(facing);
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
            case "KeyTag":
                keyTag = func.split(">")[1];
                break;
            case "Facing":
                if (func.split(">")[1] == "++") {
                    facing++;
                } else if (func.split(">")[1] == "--") {
                    facing--;
                } else {
                    facing = Integer.parseInt((func.split(">")[1]));
                }
                break;
            case "IsDoorOpen":
                if (func.split(">")[1] == "++") {
                    isDoorOpen = !isDoorOpen;
                } else if (func.split(">")[1] == "--") {
                    isDoorOpen = !isDoorOpen;
                } else {
                    isDoorOpen = Boolean.getBoolean(func.split(">")[1]);
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
