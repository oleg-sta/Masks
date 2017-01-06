attribute vec3 vPosition;
uniform mat4 u_MVPMatrix; // A constant representing the combined model/view/projection matrix

void main() {
  gl_Position = u_MVPMatrix * vec4(vPosition, 1.0);
}