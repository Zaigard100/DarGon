package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.zaig100.dg.Main;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ShaderManager {

    static boolean isAndroid;
    static File destory;
    static JSONParser jsonParser = new JSONParser();
    static JSONObject jsonObject, JO;
    static JSONArray jsonArray, JA;
    static Reader in;
    static Iterator iterator, iter;
    static ArrayList<ShaderConfig> shaders = new ArrayList<ShaderConfig>();
    static HashMap<String, String> uniforms = new HashMap<String, String>();
    static String u;

    public static void load(boolean isAndroid, Main m) {

        ShaderManager.isAndroid = isAndroid;

        /*if (isAndroid) {
            destory = new File(Main.getSd(), "DarGon/shaders/shader.json");
        } else {
            destory = new File(new File("").getAbsoluteFile(), "shaders/shader.json");
        }
        if (!destory.exists()) {
            try {
                jsonObject = (JSONObject) jsonParser.parse("{\"ShaderList\":[]}");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                destory.getParentFile().mkdir();
                destory.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try{
                FileWriter file = new FileWriter(destory);
                file.write(jsonObject.toJSONString());
                file.flush();
                file.close();
            } catch (IOException ex){
                ex.printStackTrace();
            }
        }

        Reader in = null;
        try {
            in = new InputStreamReader(new FileInputStream(destory));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            jsonObject = (JSONObject) jsonParser.parse(in);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonArray = (JSONArray) jsonObject.get("ShaderList");
        iterator = jsonArray.iterator();
        while(iterator.hasNext()) {
            JO = (JSONObject) iterator.next();
            JA = (JSONArray) JO.get("Uniforms");
            iter = JA.iterator();
            while(iter.hasNext()){
                u = (String) iter.next();
                uniforms.put(u, (String) JO.get(u));
            }
            shaders.add(new ShaderConfig((String)JO.get("Name"),uniforms));
            shaders.get(shaders.size()-1).setShader(new ShaderProgram(Gdx.files.internal("shaders/default.vert"),Gdx.files.absolute((destory.getParent() + ((String) JO.get("Path"))))));
            if(JO.get("FlipX")!=null) {
                shaders.get(shaders.size() - 1).setFlipX((Boolean) JO.get("FlipX"));
            }
            if(JO.get("FlipY")!=null){
            shaders.get(shaders.size()-1).setFlipY((Boolean) JO.get("FlipY"));
            }
        }*/
        ShaderProgram.pedantic = false;
        shaders.add(new ShaderConfig("CRT", new HashMap<String, String>()));
        shaders.get(shaders.size() - 1).setShader(new ShaderProgram(Gdx.files.internal("shaders/default.vert"), Gdx.files.internal("shaders/DefautCRT.frag")));
        shaders.get(shaders.size() - 1).getUniforms().put("iTime", "time");
        shaders.get(shaders.size() - 1).getUniforms().put("iResolution", "resolution");
        shaders.get(shaders.size() - 1).setFlipY(true);

        shaders.add(new ShaderConfig("Disco", new HashMap<String, String>()));
        shaders.get(shaders.size() - 1).setShader(new ShaderProgram(Gdx.files.internal("shaders/default.vert"), Gdx.files.internal("shaders/Disco.glsl")));
        shaders.get(shaders.size() - 1).getUniforms().put("iTime", "time");
        shaders.get(shaders.size() - 1).getUniforms().put("iResolution", "resolution");
        shaders.get(shaders.size() - 1).setFlipX(true);
    }

    public static ArrayList<ShaderConfig> getShaders() {
        return shaders;
    }

    public static void dispose() {
        iter = shaders.iterator();
        while (iter.hasNext()) {
            ((ShaderConfig) iter.next()).dispose();
        }
    }
}
