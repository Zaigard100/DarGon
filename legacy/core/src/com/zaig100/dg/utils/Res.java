package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Res {

    static public int num, i = -1;
    public static boolean end = false;

    static final Calendar c = Calendar.getInstance();
    static int month = c.get(Calendar.MONTH);
    static int day = c.get(Calendar.DAY_OF_MONTH);

    static public Texture black = new Texture(Gdx.files.internal("texture/black.png"));

    static public Texture ui = new Texture(Gdx.files.internal("texture/UI.png"));
    static public Texture tileset = new Texture(Gdx.files.internal("texture/TileSet.png"));
    static public Texture logo;

    static public Texture doors = new Texture(Gdx.files.internal("texture/doors.png"));

    static public TextureRegion play_button;
    static public TextureRegion play_button_toched;

    static public TextureRegion tutorial_button;
    static public TextureRegion tutorial_button_toched;

    static public TextureRegion settings_button;
    static public TextureRegion settings_button_toched;

    static public TextureRegion nextlv;
    static public Texture dark;

    static public Texture bag;
    static public Texture menu;
    static public Texture money, shop, shop_ui;

    static public Texture hp_potion;
    static public Texture sheld;
    static public Texture torch;
    static public Texture show_trap_potion, sinf;
    static public Texture speed_potion;
    static public Texture slowdown_potion;

    static public Texture bundle;

    static public Texture teleport;
    static public Texture damage;

    static public Texture blue_obj, green_obj, red_sm_obj, blue_sm_obj, green_sm_obj;

    static public TextureRegion flamethrowfer;


    static public TextureRegion arrow;

    static public Texture vk;

    static public Texture spike_0;
    static public Texture spike_1;

    static public Texture button;
    static public Texture chest;

    static Sprite hero;

    static public Texture amonghero;
    static public Texture firedhero;
    static public Texture shotedhero;
    static public Texture spikedhero;

    static public Texture pause_dark, turn_off;


    static public Texture joystick;
    static public Texture use;
    /*
        public Music mainmenu;
        public Music ingame;
        public Music settingmisc;
    */

    static public Texture cube;
    static public Texture item_frame;
    static public Texture drop_item;

    static public Sound[] click = new Sound[3];
    static public Sound wasted;
    static public Sound lov;

    static public Texture boards;

    static public Texture clvl2;
    static public Texture clvl1;
    static public Texture clvl0;

    static public Texture run;
    static public Texture idle;
    static public Texture NGSprite;
    static public Texture HP1, HP2, HP3, HP4;
    static public Texture spinney;
    static public Texture tablet, x;

    static public Sprite key;

    static Sprite spr = null;

    static Sprite spr1;

    static Texture crossbowT;
    static Texture fireT;
    static Map<Integer, BitmapFont> fonts = new HashMap<>();
    static public Texture inventar;
    static public Texture pause;

    /**
     * Resource Loading
     */
    static public void sprL() {
        if (!end) i++;
        switch (i) {
            case 0:
                Res.play_button = new Sprite(ui, 0, 0, 32, 32);
                Res.play_button_toched = new Sprite(ui, 32, 0, 32, 32);
                break;
            case 1:
                Res.tutorial_button = new Sprite(ui, 0, 32, 21, 21);
                Res.tutorial_button_toched = new Sprite(ui, 21, 32, 21, 21);
                break;
            case 2:
                Res.settings_button = new Sprite(ui, 42, 32, 21, 21);
                Res.settings_button_toched = new Sprite(ui, 63, 32, 21, 21);
                speed_potion = new Texture(Gdx.files.internal("texture/speed_potion.png"));
                slowdown_potion = new Texture(Gdx.files.internal("texture/slowdown_potion.png"));
                break;
            case 3:
                Res.nextlv = new TextureRegion(new Texture(Gdx.files.internal("texture/nextlevel.png")));
                Res.logo = new Texture(Gdx.files.internal("texture/logo.png"));
                Res.dark = new Texture(Gdx.files.internal("texture/Dark.png"));
                drop_item = new Texture(Gdx.files.internal("texture/drop_item.png"));
                sinf = new Texture(Gdx.files.internal("texture/sinf.png"));
                break;
            case 4:
                Res.bag = new Texture(Gdx.files.internal("texture/bag.png"));
                Res.hp_potion = new Texture(Gdx.files.internal("texture/hp_potion.png"));
                Res.sheld = new Texture(Gdx.files.internal("texture/sheld.png"));
                Res.torch = new Texture(Gdx.files.internal("texture/torch_spritesheet.png"));
                break;
            case 5:
                Res.teleport = new Texture(Gdx.files.internal("texture/teleporter.png"));
                Res.damage = new Texture(Gdx.files.internal("texture/Damage.png"));
                Res.flamethrowfer = new TextureRegion(new Texture(Gdx.files.internal("texture/flamethrower.png")));
                Res.arrow = new TextureRegion(new Texture(Gdx.files.internal("texture/arrow.png")));
                break;
            case 6:
                Res.amonghero = new Texture(Gdx.files.internal("texture/amonghero.png"));
                Res.firedhero = new Texture(Gdx.files.internal("texture/firedhero.png"));
                Res.shotedhero = new Texture(Gdx.files.internal("texture/shotedhero.png"));
                Res.spikedhero = new Texture(Gdx.files.internal("texture/spikedhero.png"));
                break;
            case 7:
                Res.pause_dark = new Texture(Gdx.files.internal("texture/resume.png"));
                Res.button = new Texture(Gdx.files.internal("texture/button.png"));
                Res.inventar = new Texture(Gdx.files.internal("texture/inventar.png"));
                show_trap_potion = new Texture(Gdx.files.internal("texture/show_trap_potion.png"));
                break;
            case 8:
                Res.joystick = new Texture(Gdx.files.internal("texture/Joystick.png"));
                Res.use = new Texture(Gdx.files.internal("texture/use.png"));
                Res.key = new Sprite(new Texture(Gdx.files.internal("texture/key.png")));
                turn_off = new Texture(Gdx.files.internal("texture/turn_off.png"));
                x = new Texture(Gdx.files.internal("texture/x.png"));
                break;
            case 9:
                Res.vk = new Texture(Gdx.files.internal("texture/vk.png"));
                Res.spinney = new Texture(Gdx.files.internal("texture/spinney.png"));
                tablet = new Texture(Gdx.files.internal("texture/tablet.png"));
                chest = new Texture(Gdx.files.internal("texture/chest.png"));
                break;
/*
            mainmenu = Gdx.audio.newMusic(Gdx.files.internal("sounds/mainmenu.mp3"));
            ingame = Gdx.audio.newMusic(Gdx.files.internal("sounds/ingame.mp3"));
            settingmisc = Gdx.audio.newMusic(Gdx.files.internal("sounds/settings.mp3"));
*/
            case 10:
                Res.click[0] = Gdx.audio.newSound(Gdx.files.internal("sounds/click1.mp3"));
                Res.click[1] = Gdx.audio.newSound(Gdx.files.internal("sounds/click1.mp3"));
                Res.click[2] = Gdx.audio.newSound(Gdx.files.internal("sounds/click2.mp3"));
                break;
            case 11:
                Res.wasted = Gdx.audio.newSound(Gdx.files.internal("sounds/wasted.mp3"));
                Res.lov = Gdx.audio.newSound(Gdx.files.internal("sounds/lov.mp3"));
                Res.boards = new Texture(Gdx.files.internal("texture/Dark.png"));
                break;
            case 12:
                Res.clvl2 = new Texture(Gdx.files.internal("texture/2_clvl.png"));
                Res.clvl1 = new Texture(Gdx.files.internal("texture/1_clvl.png"));
                Res.clvl0 = new Texture(Gdx.files.internal("texture/0_clvl.png"));
                cube = new Texture(Gdx.files.internal("texture/cube.png"));
                item_frame = new Texture(Gdx.files.internal("texture/item_frame.png"));
                break;
            case 13:
                Res.run = new Texture(Gdx.files.internal("texture/run.png"));
                Res.idle = new Texture(Gdx.files.internal("texture/idle.png"));
                Res.NGSprite = new Texture(Gdx.files.internal("texture/NGSprite.png"));
                break;
            case 14:
                Res.HP1 = new Texture(Gdx.files.internal("texture/НP_1.png"));
                Res.HP2 = new Texture(Gdx.files.internal("texture/НP_2.png"));
                Res.HP3 = new Texture(Gdx.files.internal("texture/НP_3.png"));
                Res.HP4 = new Texture(Gdx.files.internal("texture/НP_4.png"));
                break;
            case 15:
                Res.crossbowT = new Texture(Gdx.files.internal("texture/crossbow.png"));
                Res.fireT = new Texture(Gdx.files.internal("texture/Fire.png"));
                Res.pause = new Texture(Gdx.files.internal("texture/pause.png"));
                break;
            case 16:
                Res.spike_0 = new Texture(Gdx.files.internal("texture/spike0.png"));
                Res.spike_1 = new Texture(Gdx.files.internal("texture/spike1.png"));
                Res.menu = new Texture(Gdx.files.internal("texture/menu.png"));
                bundle = new Texture(Gdx.files.internal("texture/bundle.png"));
                red_sm_obj = new Texture(Gdx.files.internal("texture/red_sm_obj.png"));
                blue_sm_obj = new Texture(Gdx.files.internal("texture/blue_sm_obj.png"));
                green_sm_obj = new Texture(Gdx.files.internal("texture/green_sm_obj.png"));
                break;
            case 17:
                blue_obj = new Texture(Gdx.files.internal("texture/blue_obj.png"));
                green_obj = new Texture(Gdx.files.internal("texture/green_obj.png"));
                money = new Texture(Gdx.files.internal("texture/money.png"));
                shop = new Texture(Gdx.files.internal("texture/shop.png"));
                shop_ui = new Texture(Gdx.files.internal("texture/shop_ui.png"));
                break;
            case 18:
                end = true;
                break;
        }


    }

    static public Sprite crossbow(boolean stage) {


        if (stage) {
            Res.num = 0;
        } else {
            Res.num = 1;
        }
        return new Sprite(crossbowT, 16 * num, 0, 16, 16);

    }

    static public Sprite chest(boolean stage) {
        if (stage) {
            Res.num = 1;
        } else {
            Res.num = 0;
        }
        return new Sprite(chest, 16 * num, 0, 16, 16);
    }

    static public Sprite fire(int stage) {
        Res.spr1 = new Sprite(fireT, 16 * stage, 0, 16, 16);
        return Res.spr1;
    }


    static public Sprite HP(int count) {


        if (count <= 0) {
            Res.spr = null;
        }
        if (count == 1) {
            Res.spr = new Sprite(HP1, 0, 0, 16, 16);
        }
        if (count == 2) {
            Res.spr = new Sprite(HP2, 0, 0, 16, 16);
        }
        if (count == 3) {
            Res.spr = new Sprite(HP3, 0, 0, 16, 16);
        }
        if (count >= 4) {
            Res.spr = new Sprite(HP4, 0, 0, 16, 16);
        }

        return Res.spr;
    }

    static public Sprite hero(boolean flip, boolean walked, int stage) {
        if (!walked) {
            Res.hero = new Sprite(idle, stage * 16, 0, 16, 16);
        } else {
            Res.hero = new Sprite(run, stage * 16, 0, 16, 16);
        }
        if ((Res.month == 11 && Res.day == 31) || (Res.month == 0 && Res.day <= 7)) {
            Res.hero = new Sprite(NGSprite, stage * 16, 0, 16, 16);
        }
        Res.hero.flip(flip, false);
        return Res.hero;
    }

    static public Sprite door(int i) {
        return new Sprite(doors, 16 * i, 0, 16, 32);
    }

    /**
     * @return Return tile
     */
    static public Sprite tile(int x, int y) {
        return new Sprite(Res.tileset, x * 16, y * 16, 16, 16);
    }

    static public BitmapFont getFont(int size) {
        if (!fonts.containsKey(size)) {
            fonts.put(size, Font.gFont(size * Configuration.getScale(), "fonts/GFont.ttf"));
        }
        return fonts.get(size);
    }

    static public void cleanFont(){
        fonts.clear();
    }

    static public void dispose() {
        for (Map.Entry<Integer, BitmapFont> entry : fonts.entrySet()) {
            entry.getValue().dispose();
        }
        Res.doors.dispose();
        Res.clvl0.dispose();
        Res.clvl1.dispose();
        Res.clvl2.dispose();
        Res.wasted.dispose();
        Res.click[0].dispose();
        Res.click[1].dispose();
        Res.click[2].dispose();
        Res.wasted.dispose();
        Res.lov.dispose();
        Res.ui.dispose();
        Res.tileset.dispose();
        Res.logo.dispose();
        Res.bag.dispose();
        Res.boards.dispose();
        Res.damage.dispose();
        Res.dark.dispose();
        Res.firedhero.dispose();
        Res.joystick.dispose();
        Res.pause_dark.dispose();
        Res.hp_potion.dispose();
        Res.teleport.dispose();
        Res.torch.dispose();
        Res.amonghero.dispose();
        Res.sheld.dispose();
        Res.shotedhero.dispose();
        Res.use.dispose();
        Res.NGSprite.dispose();
        ;
        Res.idle.dispose();
        Res.run.dispose();
        Res.HP1.dispose();
        Res.HP2.dispose();
        Res.HP3.dispose();
        Res.HP4.dispose();
        Res.crossbowT.dispose();
        Res.fireT.dispose();
    }
}
