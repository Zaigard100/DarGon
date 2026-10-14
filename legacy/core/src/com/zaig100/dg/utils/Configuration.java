package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.zaig100.dg.Main;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Configuration {

    static File conf;

    public static JSONObject jsonObject;

    static float Scale = 6;
    static int Music = 50;
    static int Sound = 50;
    static boolean sensor = true;
    static boolean debug = false;
    static int shader = -1;
    ////static boolean fulscreen = false;
    //static String shader = "shaders/DefautCRT.frag";

    public static void load(Main m) throws IOException {

        if (m.isAndroid()) {
            conf = Gdx.files.local("DGConfig.json").file();
        } else {
            conf = new File(new File("").getAbsoluteFile(), "DGconfig.json");
        }
        JSONParser jsonParser = new JSONParser();

            try {
                jsonObject = (JSONObject) jsonParser.parse("{}");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        if(!(conf.exists())) {
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

    static void load() {
        try {
            Scale = ((Double) jsonObject.get("Scale")).floatValue();
            Music = ((Long) jsonObject.get("Music")).intValue();
            Sound = ((Long) jsonObject.get("Sound")).intValue();
            sensor = (Boolean) jsonObject.get("Sensor");
            debug = (Boolean) jsonObject.get("Debug");
            shader = ((Long) jsonObject.get("Shader")).intValue();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    static public void save(File f) {

        //System.out.println(conf.getAbsolutePath());

        jsonObject.put("Scale", Scale);
        jsonObject.put("Music", Music);
        jsonObject.put("Sound", Sound);
        jsonObject.put("Sensor", sensor);
        jsonObject.put("Debug", debug);
        jsonObject.put("Shader", shader);
        try {
            FileWriter file = new FileWriter(f);
            file.write(jsonObject.toJSONString());
            file.flush();
            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private static void createDefaultConfig(File f) {
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        save(f);

    }

    static public float getScale() {
        return Scale;
    }

    static public void setScale(float scale) {
        Scale = scale;
    }
    static public void setScale(int scale) {
        Scale = scale;
    }

    static public int getMusic() {
        return Music;
    }

    static public void setMusic(int music) {
        Music = music;
    }

    static public int getSound() {
        return Sound;
    }

    static public void setSound(int sound) {
        Sound = sound;
    }

    static public File getConf() {
        return conf;
    }

    static public boolean isSensor() {
        return sensor;
    }

    static public void setSensor(boolean sensor) {
        Configuration.sensor = sensor;
    }

    static public boolean isDebug() {
        return debug;
    }

    static public void setDebug(boolean debug) {
        Configuration.debug = debug;
    }

    public static int getShader() {
        return shader;
    }

    public static void setShader(int shader) {
        Configuration.shader = shader;
    }
}
