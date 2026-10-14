package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.zaig100.dg.Main;
import com.zaig100.dg.world.World;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Save {

    static File conf;

    static JSONObject jsonObject;

    static String path = "levels/01.json";
    static int hp;
    static int money;
    static JSONArray arr;

    public Save(Main m, String pack, String derect) throws IOException {
        path = derect;
        if (m.isAndroid()) {
            conf = Gdx.files.local("save" + pack + ".json").file();
        } else {
            conf = new File(new File("").getAbsoluteFile(), "save" + pack + ".json");
        }

        JSONParser jsonParser = new JSONParser();

        if(!(conf.exists())) {
            try {
                jsonObject = (JSONObject) jsonParser.parse("{}");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            createDefaultConfig(conf);
        }

        Reader in = new InputStreamReader(new FileInputStream(conf));
        try {
            jsonObject = (JSONObject) jsonParser.parse(in);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        load();
        in.close();
    }

    void load(){
        try {
            path = (String) jsonObject.get("Path");
            hp = ((Number) jsonObject.get("HP")).intValue();
            money = ((Number) jsonObject.get("Money")).intValue();
            arr = (JSONArray) jsonObject.get("Inventory");
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    public void save(File f){
        jsonObject.put("Path", path);
        jsonObject.put("HP", hp);
        jsonObject.put("Money", money);
        jsonObject.put("Inventory", World.player.inventory.inventoryToJSON());
        try {
            FileWriter file = new FileWriter(f);
            file.write(jsonObject.toJSONString());
            file.flush();
            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void createDefaultConfig(File f) {
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        save(f);
    }

    public File getConf() {
        return conf;
    }

    public void setConf(File conf) {
        Save.conf = conf;
    }

    public String getsPath() {
        return path;
    }

    public void setPath(String path) {
        Save.path = path;
    }

    public static int getHp() {
        return hp;
    }

    public static void setHp(int hp) {
        Save.hp = hp;
    }

    public static int getMoney() {
        return money;
    }

    public static void setMoney(int money) {
        Save.money = money;
    }

    public JSONArray getArr() {
        return arr;
    }

    public static void setArr(JSONArray arr) {
        Save.arr = arr;
    }
}
