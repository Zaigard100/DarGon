package com.zaig100.dg.world.elements.items;

import com.badlogic.gdx.graphics.g2d.Batch;

import org.json.simple.JSONObject;

public abstract class Item {

    public enum ItemType {
        ITEM("item"),
        EMPTY("empty"),
        POITION("potion"),
        SHELD("sheld"),
        TORCH("torch"),
        KEY("key"),
        EGG("egg"),
        SHOW_TRAP_POITION("show_trap"),
        MONEY("money"),
        SPEED_POITION("speed"),
        SLOWDOWN_POITION("slowdown");

        String name;

        ItemType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    ItemType type = ItemType.ITEM;
    int count;

    public Item() {
        this.count = 1;
    }

    public Item(int count) {
        this.count = count;
    }


    public static Item jsonToItem(JSONObject item) {
        String type;
        try {
            type = (String) item.get("Type");
        } catch (NullPointerException npe) {
            System.out.println("Error in jsonToItem(): ");
            System.out.println(item.toJSONString());
            type = "empty";
        }
        switch (type) {
            case "potion":
                return new Poition((((Number) item.get("Count"))).intValue());
            case "sheld":
                return new Sheld((((Number) item.get("Count"))).intValue());
            case "torch":
                return new Torch((((Number) item.get("Count"))).intValue());
            case "key":
                return new Key(
                        ((String) item.get("KeyTag")),
                        (((Number) item.get("R"))).floatValue(),
                        (((Number) item.get("G"))).floatValue(),
                        (((Number) item.get("B"))).floatValue()
                );
            case "show_trap":
                return new TrapShowPotion((((Number) item.get("Count"))).intValue());
            case "money":
                return new Money((((Number) item.get("Count"))).intValue());
            case "speed":
                return new SpeedPoition((((Number) item.get("Count"))).intValue(),(((Number) item.get("Power"))).floatValue());
            case "slowdown":
                return new SlowdownPoition((((Number) item.get("Count"))).intValue(),(((Number) item.get("Power"))).floatValue());
            case "egg":
                return new EasterEgg((String) item.get("Code"));
            default:
                return new Empty();
        }
    }

    public abstract void render(Batch batch, int x, int y);

    public abstract void renderInMap(Batch batch, int wX, int wY);

    public abstract boolean use();

    public abstract JSONObject toJson();

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ItemType getType() {
        return type;
    }
}
