attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_projTrans;

varying vec2 v_texCoords;
varying vec4 v_color;
void main() {
	v_texCoords = a_texCoord0;
	v_color = a_color;
	vec4 u_offset = vec4(0., 0., 0., 0.);
	gl_Position = u_projTrans
			* vec4(a_position.x, a_position.y, a_position.z, a_position.w)
			+ u_offset;
}