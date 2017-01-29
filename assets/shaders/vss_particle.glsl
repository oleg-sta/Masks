attribute vec3 vPosition;
attribute vec2 a_TexCoordinate;
const vec3 Gravity = vec3(0.0,-0.05,0.0);
uniform float iGlobalTime;

uniform mat4 u_MVPMatrix;

void main()
{
float t = iGlobalTime / 40.0;
vec3 pos = vPosition * t + Gravity * t * t;
gl_Position = u_MVPMatrix * vec4(pos, 1.0);
//gl_PointSize = 5.0;
}