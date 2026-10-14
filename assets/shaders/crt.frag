// Эффект «ЭЛТ-монитор»: изгиб, хроматическая аберрация, виньетка и строки развёртки.
// По мотивам DefautCRT.frag из DarGon3, переписан под GLSL ES 1.00 (работает и на Android).
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform vec2 u_size;

vec2 curve(vec2 uv) {
    uv = (uv - 0.5) * 2.0;
    uv *= 1.1;
    uv.x *= 1.0 + pow(abs(uv.y) / 5.0, 2.0);
    uv.y *= 1.0 + pow(abs(uv.x) / 4.0, 2.0);
    uv = uv / 2.0 + 0.5;
    return uv * 0.92 + 0.04;
}

void main() {
    vec2 uv = curve(v_texCoords);
    vec3 col;
    col.r = texture2D(u_texture, vec2(uv.x + 0.003, uv.y)).r;
    col.g = texture2D(u_texture, uv).g;
    col.b = texture2D(u_texture, vec2(uv.x - 0.003, uv.y)).b;
    float inside = step(0.0, uv.x) * step(0.0, uv.y) * step(uv.x, 1.0) * step(uv.y, 1.0);
    col *= inside;
    col *= 0.5 + 0.5 * 16.0 * uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    col *= vec3(0.95, 1.05, 0.95);
    col *= 0.9 + 0.1 * sin(10.0 * u_time + uv.y * u_size.y * 1.5);
    col *= 0.99 + 0.01 * sin(110.0 * u_time);
    gl_FragColor = vec4(col, 1.0);
}
