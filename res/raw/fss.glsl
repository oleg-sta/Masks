precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;
void main() {
  gl_FragColor = vec4(0.5f, 0.5f, 0.5f, 0.5f);
  gl_FragColor = texture2D(sTexture,texCoord) / 2.0;
}