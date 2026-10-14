package com.zaig100.dg.utils;


import com.zaig100.dg.Main;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class PackManager {

    static boolean isAndroid;
    static File destory;

    static ArrayList<JSONObject> list = new ArrayList<JSONObject>();

    static JSONParser parser = new JSONParser();

    public PackManager(boolean isAndroid, Main m) {

        PackManager.isAndroid = isAndroid;

        if (isAndroid) {
            destory = new File(Main.getSd(), "packs");
        } else {
            destory = new File(new File("").getAbsoluteFile(), "packs");
        }
        if (!destory.exists()) {
            destory.mkdirs();
        }
        if (destory.list() != null) {
            if (destory.list().length > 0) {
                for (int i = 0; i < destory.list().length; i++) {
                    if (new File(destory, destory.list()[i]).isDirectory()) {
                        if (new File(destory, destory.list()[i] + "/pack.json").exists()) {
                            try {
                                try {
                                    list.add((JSONObject) parser.parse(new InputStreamReader(new FileInputStream(new File(destory, destory.list()[i] + "/pack.json")))));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

    }

    static public File getDestory() {
        return destory;
    }

    static public ArrayList<JSONObject> getList() {
        return list;
    }




}
