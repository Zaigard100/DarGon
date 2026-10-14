// Темнота тёмного уровня, плавная (рисуется в разрешении окна поверх пиксельного мира).
// Без факела — круг света вокруг игрока, как dark.png из DarGon3 (радиус 22 пикселя тайла,
// мягкий переход до 27). Зажжённый факел (u_torch 0..1) плавно раздвигает темноту к краям
// экрана; там она остаётся рамкой, внутренний край которой неравномерно дрожит, как свет
// от пламени.
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform vec2 u_view;      // размер вида в пикселях тайла (112×80)
uniform vec2 u_center;    // центр игрока в пикселях тайла (Y вниз, как в уровне)
uniform float u_torch;    // 0 — нет факела, 1 — разгорелся
uniform float u_time;

const vec3 DARK = vec3(25.0, 29.0, 21.0) / 255.0;
const float HOLE = 22.0;
const float SOFT = 5.0;

void main() {
    vec2 p = v_texCoords * u_view;
    float d = distance(p, u_center);

    float grow = u_torch * u_torch * (3.0 - 2.0 * u_torch);
    float radius = HOLE + grow * length(u_view);
    float circle = smoothstep(radius, radius + SOFT, d);

    // Рамка: расстояние до края экрана с неровным, дрожащим краем.
    float edge = min(min(p.x, p.y), min(u_view.x - p.x, u_view.y - p.y));
    // Положение вдоль периметра — угол от центра экрана. Частоты по углу целые, чтобы на стыке
    // -180°/180° не было шва; по времени — медленно, чтобы край «дышал», а не мельтешил.
    vec2 fromCenter = p - u_view * 0.5;
    float a = atan(fromCenter.y, fromCenter.x);
    float flicker = 1.6 * sin(5.0 * a + u_time * 1.3)
                  + 1.1 * sin(9.0 * a - u_time * 1.9 + 1.7)
                  + 0.6 * sin(14.0 * a + u_time * 2.7 + 4.1);
    float frame = 1.0 - smoothstep(2.0, 11.0, edge + flicker);

    float alpha = max(circle, frame * grow);
    gl_FragColor = vec4(DARK, alpha);
}
