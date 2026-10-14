package com.zaig100.dg.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.zaig100.dg.utils.contain.CrossbowAIC;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.LevelRead;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.contain.ButtonС;
import com.zaig100.dg.utils.contain.ChestC;
import com.zaig100.dg.utils.contain.CrossbowC;
import com.zaig100.dg.utils.contain.DoorC;
import com.zaig100.dg.utils.contain.FlamefrowerC;
import com.zaig100.dg.utils.contain.FlimsyTileC;
import com.zaig100.dg.utils.contain.HideTrapC;
import com.zaig100.dg.utils.contain.ItemC;
import com.zaig100.dg.utils.contain.ShopC;
import com.zaig100.dg.utils.contain.SpikeC;
import com.zaig100.dg.utils.contain.SpinneyC;
import com.zaig100.dg.utils.contain.StairC;
import com.zaig100.dg.utils.contain.TabletC;
import com.zaig100.dg.utils.contain.TeleportC;
import com.zaig100.dg.utils.contain.ZonaC;
import com.zaig100.dg.utils.contain.mobC.KamikadzeC;
import com.zaig100.dg.world.objects.Button;
import com.zaig100.dg.world.objects.Chest;
import com.zaig100.dg.world.objects.CrossbowAI;
import com.zaig100.dg.world.objects.Crossbow;
import com.zaig100.dg.world.objects.Door;
import com.zaig100.dg.world.objects.Flamefrower;
import com.zaig100.dg.world.objects.FlimsyTile;
import com.zaig100.dg.world.objects.HideTrap;
import com.zaig100.dg.world.objects.Items;
import com.zaig100.dg.world.objects.Obj;
import com.zaig100.dg.world.objects.Shop;
import com.zaig100.dg.world.objects.Spike;
import com.zaig100.dg.world.objects.Spinney;
import com.zaig100.dg.world.objects.Stair;
import com.zaig100.dg.world.objects.Tablet;
import com.zaig100.dg.world.objects.Teleport;
import com.zaig100.dg.world.objects.Zona;
import com.zaig100.dg.world.objects.mobs.Kamikaze;

import java.util.ArrayList;
import java.util.Iterator;

public class Map {

    private int idNum = 0;

    ArrayList<String> text;
    boolean textShow;

    private Iterator iter;

    public ArrayList<Obj> objectsU = new ArrayList<>();
    public ArrayList<Obj> objectsO = new ArrayList<>();
    public ArrayList<Stair> stair = new ArrayList<>();

    public int mapWidht, mapHeight;
    boolean isDark;
    public int[] map;
    int j;
    int i;

    public Shop shop;

    GlyphLayout textLayout = new GlyphLayout();

    public Map(int mapWidht, int mapHeight, int[] map, boolean isDark) {
        this.mapWidht = mapWidht;
        this.mapHeight = mapHeight;
        this.map = map;
        this.isDark = isDark;

    }

    public boolean isGround(int oldX, int oldY) {
        if ((oldX >= mapWidht) || (oldY >= mapHeight) || (oldX < 0) || (oldY < 0)) {
            return false;
        } else {
            return map[oldX + oldY * mapWidht] == 11;
        }
    }

    public void setShop(Shop shop) {
        World.player.isShop = true;
        this.shop = shop;
    }

    public void render(SpriteBatch batch) {
        for (i = World.player.getY() - 4; i < World.player.getY() + 4; i++) {
            for (j = World.player.getX() - 5; j < World.player.getX() + 5; j++) {
                if (i >= 0 && i < mapHeight && j >= 0 && j < mapWidht) {
                    if (map[j + mapWidht * i] == 11) {
                        batch.draw(Res.tile(0, 0), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 12) {
                        batch.draw(Res.tile(1, 1), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 13) {
                        batch.draw(Res.tile(2, 0), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 14) {
                        batch.draw(Res.tile(1, 0), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 15) {
                        batch.draw(Res.tile(2, 1), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 16) {
                        batch.draw(Res.tile(3, 0), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 17) {
                        batch.draw(Res.tile(3, 1), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 18) {
                        batch.draw(Res.tile(4, 1), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                    if (map[j + mapWidht * i] == 19) {
                        batch.draw(Res.tile(4, 0), -World.player.wX + (j + 3) * 16 * Configuration.getScale(), -World.player.wY + (i + 2) * 16 * Configuration.getScale(), 16 * Configuration.getScale(), 16 * Configuration.getScale());
                    }
                }
            }
        }
    }

    public void show_tablet_text(Batch batch) {
        if (textShow) {
            for (int i = 0; i < text.size(); i++) {
                Res.getFont(3).draw(batch, text.get(i), 8 * Configuration.getScale(), Gdx.graphics.getHeight() / 2 + 4 * Configuration.getScale() * (text.size() / 2 - i));
            }
            if (World.player.walked) {
                textShow = false;
            }
        }
    }

    public void setTablet_text(ArrayList<String> text) {
        if (textShow) return;
        this.text = text;
        textShow = true;
    }

    public void object_update(SpriteBatch batch) {
        int i1;
        if (objectsU.size() > 0) {
            for (i1 = 0; i1 < objectsU.size(); i1++) {
                objectsU.get(i1).render(batch);
                if (!World.player.isStop) objectsU.get(i1).frame();
            }
        }
        World.player.render(batch);
        if (!World.player.isStop) World.player.tick(0.2f);
        if (!World.player.isStop) World.player.frame();

        if (objectsO.size() > 0) {
            for (i1 = 0; i1 < objectsO.size(); i1++) {
                objectsO.get(i1).render(batch);
                    if (!World.player.isStop) {
                        objectsO.get(i1).frame();
                    }
                }
            }
        }

    public void show_obj(SpriteBatch batch) {
        int i1;
        if (objectsU.size() > 0) {
            for (i1 = 0; i1 < objectsU.size(); i1++) {
                objectsU.get(i1).show_obj(batch);
            }
        }
        if (objectsO.size() > 0) {
            for (i1 = 0; i1 < objectsO.size(); i1++) {
                objectsO.get(i1).show_obj(batch);
            }
        }
        if (objectsO.size() > 0) {
            for (i1 = 0; i < stair.size(); i++) {
                stair.get(i1).show_obj(batch);
            }
        }
    }

    public void object_load(LevelRead lR) {

        World.player.setX(lR.getSpawnX());
        World.player.setY(lR.getSpawnY());
        World.player.wCordNormalize();
        setDark(lR.isDark());


        iter = lR.getStair().iterator();
        while (iter.hasNext()) {
            StairC stairС = (StairC) iter.next();
            stair.add(new Stair(stairС));
            stair.get(stair.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getZona().iterator();
        while (iter.hasNext()) {
            ZonaC zonaC = (ZonaC) iter.next();
            objectsU.add(new Zona(zonaC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getTeleport().iterator();
        while (iter.hasNext()) {
            TeleportC teleportC = (TeleportC) iter.next();
            objectsU.add(new Teleport(teleportC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getHide_trap().iterator();
        while (iter.hasNext()) {
            HideTrapC hideTrapC = (HideTrapC) iter.next();
            objectsU.add(new HideTrap(hideTrapC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getFlamefrower().iterator();
        while (iter.hasNext()) {
            FlamefrowerC flamefrowerC = (FlamefrowerC) iter.next();
            objectsO.add(new Flamefrower(flamefrowerC));
            objectsO.get(objectsO.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getCrosbow().iterator();
        while (iter.hasNext()) {
            CrossbowC crossbowC = (CrossbowC) iter.next();
            objectsO.add(new Crossbow(crossbowC));
            objectsO.get(objectsO.size() - 1).setObjID(idNum);
            idNum++;
        }
        iter = lR.getCrosbowAI().iterator();
        while (iter.hasNext()) {
            CrossbowAIC crossbowAIC = (CrossbowAIC) iter.next();
            objectsO.add(new CrossbowAI(crossbowAIC));
            objectsO.get(objectsO.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getFlimsy_tile().iterator();
        while (iter.hasNext()) {
            FlimsyTileC flimsyTileC = (FlimsyTileC) iter.next();
            objectsU.add(new FlimsyTile(flimsyTileC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getSpinney().iterator();
        while (iter.hasNext()) {
            SpinneyC spinneyC = (SpinneyC) iter.next();
            objectsO.add(new Spinney(spinneyC));
            objectsO.get(objectsO.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getSpike().iterator();
        while (iter.hasNext()) {
            SpikeC spikeC = (SpikeC) iter.next();
            objectsU.add(new Spike(spikeC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getButton().iterator();
        while (iter.hasNext()) {
            ButtonС buttonC = (ButtonС) iter.next();
            objectsU.add((new Button(buttonC, this)));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getItem().iterator();
        while (iter.hasNext()) {
            ItemC itemC = (ItemC) iter.next();
            objectsU.add(new Items(itemC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }
        iter = lR.getKamikaze().iterator();
        while (iter.hasNext()) {
            KamikadzeC kamikadzeC = (KamikadzeC) iter.next();
            objectsU.add(new Kamikaze(kamikadzeC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }

        iter = lR.getTablet().iterator();
        while (iter.hasNext()) {
            TabletC tabletC = (TabletC) iter.next();
            objectsU.add(new Tablet(tabletC));
            objectsU.get(objectsU.size() - 1).setObjID(idNum);
            idNum++;
        }
        iter = lR.getShop().iterator();
        while (iter.hasNext()) {
            ShopC shopC = (ShopC) iter.next();
            objectsU.add(new Shop(shopC));
            objectsU.get(stair.size() - 1).setObjID(idNum);
            idNum++;
        }
        iter = lR.getChest().iterator();
        while (iter.hasNext()) {
            ChestC chestC = (ChestC) iter.next();
            objectsU.add(new Chest(chestC));
            objectsU.get(stair.size() - 1).setObjID(idNum);
            idNum++;
        }
        iter = lR.getDoor().iterator();
        while (iter.hasNext()) {
            DoorC doorC = (DoorC) iter.next();
            objectsO.add(new Door(doorC));
            objectsO.get(stair.size() - 1).setObjID(idNum);
            idNum++;
        }

    }

    public void doFunc(String func) {
        try {
            if (stair.size() > 0) {
                for (i = 0; i < stair.size(); i++) {
                    if (stair.get(i).getTag().equals(func.split(":")[0])) {
                        stair.get(i).tag_activate(func.split(":")[1]);
                    }
                }
            }
            if (objectsU.size() > 0) {
                for (i = 0; i < objectsU.size(); i++) {
                    if (objectsU.get(i).getTag().equals(func.split(":")[0])) {
                        objectsU.get(i).tag_activate(func.split(":")[1]);
                    }
                }
            }
            if (objectsO.size() > 0) {
                for (i = 0; i < objectsO.size(); i++) {
                    if (objectsO.get(i).getTag().equals(func.split(":")[0])) {
                        objectsO.get(i).tag_activate(func.split(":")[1]);
                    }
                }
            }
            if (func.split(":")[0].equals("Map")) {
                tag_activate(func.split(":")[1]);
            }
        } catch (Exception ignored) {

        }
    }

    public void dark_render(SpriteBatch batch) {
        batch.draw(Res.boards, 7 * 16 * Configuration.getScale(), 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(Res.boards, 0, 5 * 16 * Configuration.getScale(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (isDark) {
            batch.draw(Res.dark, -16 * Configuration.getScale() * 4, -16 * Configuration.getScale() * 3, 240 * Configuration.getScale(), 176 * Configuration.getScale());
        }
    }

    public void tag_activate(String func) {
        System.out.println(Integer.parseInt(func.split(">")[0].split("-")[1].split(",")[0]) + mapWidht * Integer.parseInt(func.split(">")[0].split("-")[1].split(",")[1]));
        if (func.split("-")[0].equals("map")) {
            map[Integer.parseInt(func.split(">")[0].split("-")[1].split(",")[0]) + mapWidht * Integer.parseInt(func.split(">")[0].split("-")[1].split(",")[1])] = Integer.parseInt(func.split(">")[1]);
        }
        switch (func.split(">")[0]) {
            case "isDark":
                if (func.split(">")[1].equals("++") || func.split(">")[1].equals("--")) {
                    isDark = !isDark;
                } else {
                    isDark = Boolean.parseBoolean((func.split(">")[1]));
                }
                break;
        }
    }

    public int getTileId(int x,int y){
            if((x>=0&&x<mapWidht)&&(y>=0&&y<mapHeight)){
                return map[x + mapWidht * y];

            }else{
                return 0;
            }
    }

    public void del(){
        if (objectsU.size() > 0) {
            for (i = 0; i < objectsU.size(); i++) {
                objectsU.get(i).del();
            }
        }
        if (objectsO.size() > 0) {
            for (i = 0; i < objectsO.size(); i++) {
                objectsO.get(i).del();
            }
        }
    }

    public int getObjCount() {
        return objectsO.size() + objectsU.size() + stair.size();
    }

    public int getMapWidht() {
        return mapWidht;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public boolean isDark() {
        return isDark;
    }

    public void setDark(boolean dark) {
        isDark = dark;
    }

}
