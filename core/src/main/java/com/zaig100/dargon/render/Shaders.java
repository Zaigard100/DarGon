package com.zaig100.dargon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Загрузка шейдеров. Ошибка компиляции не роняет игру (в DarGon3 это был {@code System.exit}):
 * шейдер просто не применяется, причина пишется в журнал.
 */
public final class Shaders {

    private static final String VERTEX = "shaders/default.vert";
    private static final String PRECISION = "#ifdef GL_ES\nprecision mediump float;\n#endif\n";

    private Shaders() {}

    /** Шейдер с вершинной частью по умолчанию; {@code null}, если не собрался. */
    public static ShaderProgram load(String fragment) {
        ShaderProgram.pedantic = false;
        String vertex = Gdx.files.internal(VERTEX).readString("UTF-8");
        String source = Gdx.files.internal(fragment).readString("UTF-8");
        if (!source.contains("precision")) {
            source = PRECISION + source;
        }
        ShaderProgram shader = new ShaderProgram(PRECISION + vertex, source);
        if (!shader.isCompiled()) {
            Gdx.app.error("DarGon", "Шейдер " + fragment + " не собрался: " + shader.getLog());
            shader.dispose();
            return null;
        }
        return shader;
    }
}
