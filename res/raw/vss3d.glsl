attribute vec3 vPosition;
attribute vec2 a_TexCoordinate;
varying vec2 v_TexCoordinate;
uniform mat4 u_MVPMatrix; // A constant representing the combined model/view/projection matrix

void main() {
  v_TexCoordinate = a_TexCoordinate;
  gl_Position = u_MVPMatrix * vec4(vPosition, 1.0);
}