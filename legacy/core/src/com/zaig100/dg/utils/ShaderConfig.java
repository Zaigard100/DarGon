package com.zaig100.dg.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.Iterator;

public class ShaderConfig {

    String name, path, key, val;
    Iterator<String> iter;
    HashMap<String, String> uniforms;
    ShaderProgram shader;
    private boolean flipX = false, flipY = false;
    float time = 0;


    public ShaderConfig(String name, HashMap<String, String> uniforms) {
        this.name = name;
        this.uniforms = uniforms;
    }

    public ShaderConfig(String name, String path, HashMap<String, String> uniforms) {
        this.name = name;
        this.path = path;
        this.uniforms = uniforms;
        shader = new ShaderProgram(Gdx.files.internal("shaders/default.vert"), Gdx.files.absolute(path));

    }

    public void shader_update() {
        shader.begin();
        iter = uniforms.keySet().iterator();
        while (iter.hasNext()) {
            key = iter.next();
            val = uniforms.get(key);
            if (val == "time") {
                shader.setUniformf(key, time);
                time += Gdx.graphics.getDeltaTime();
            }
            if (val == "resolution") {
                shader.setUniformf(key, new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            }
            if (val == "screen") {
                shader.setUniformi(key, 1);
            }
            if (val == "mouse") {
                shader.setUniformf(key, new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            }
            if (val.split(":")[0] == "float") {
                shader.setUniformf(key, Float.parseFloat(val.split(":")[1]));
            }
            if (val.split(":")[0] == "int") {
                shader.setUniformi(key, Integer.parseInt(val.split(":")[1]));
            }

            shader.end();
        }
    }
    public void dispose() {
        shader.dispose();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ShaderProgram getShader() {
        return shader;
    }

    public void setShader(ShaderProgram shader) {
        this.shader = shader;
    }

    public HashMap<String, String> getUniforms() {
        return uniforms;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = !flipY;
    }
}
