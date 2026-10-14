package com.zaig100.dg;

import com.badlogic.gdx.Game;
import com.zaig100.dg.screen.extensions.ResLoadScreen;
import com.zaig100.dg.utils.Configuration;
import com.zaig100.dg.utils.Res;
import com.zaig100.dg.utils.ShaderManager;

import java.io.File;
import java.io.IOException;

public class Main extends Game {

    static boolean isAndroid;
    static File sd = null;
    static Configuration configuration;

    public Main(boolean isAndroid) {
        Main.isAndroid = isAndroid;
    }

    @Override
    public void create() {
        if (isAndroid) {
            try {
                Configuration.load(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setScreen(new ResLoadScreen(this));

    }

    @Override
    public void render() {
        super.render();

    }

    @Override
    public void dispose() {
        super.dispose();
        Res.dispose();
        ShaderManager.dispose();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(Configuration configuration) {
        Main.configuration = configuration;
    }

    public boolean isAndroid() {
        return isAndroid;
    }

    public static File getSd() {
        return sd;
    }

    public static void setSd(File sd) {
        Main.sd = sd;
    }
}
