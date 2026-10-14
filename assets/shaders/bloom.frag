// Мягкое свечение: картинка плюс размытие (по мотивам Bloom.glsl из DarGon3, GLSL ES 1.00).
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_size;

const float INTENSITY = 0.12;

void main() {
    vec2 step = 1.0 / u_size;
    vec4 sum = vec4(0.0);
    float w[5];
    w[0] = 0.16; w[1] = 0.15; w[2] = 0.12; w[3] = 0.09; w[4] = 0.05;
    sum += texture2D(u_texture, v_texCoords) * w[0] * 2.0;
    for (int i = 1; i < 5; i++) {
        float d = float(i);
        sum += texture2D(u_texture, v_texCoords + vec2(step.x * d, 0.0)) * w[i];
        sum += texture2D(u_texture, v_texCoords - vec2(step.x * d, 0.0)) * w[i];
        sum += texture2D(u_texture, v_texCoords + vec2(0.0, step.y * d)) * w[i];
        sum += texture2D(u_texture, v_texCoords - vec2(0.0, step.y * d)) * w[i];
    }
    gl_FragColor = sum * INTENSITY + texture2D(u_texture, v_texCoords);
}
