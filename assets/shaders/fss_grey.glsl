precision mediump float;
uniform sampler2D sTexture;
uniform vec2 uCenter;
uniform vec2 uCenter2;
varying vec2 texCoord;
void main() {
  float width = uCenter2.x;
  float x1 = texCoord.x;
  float x2 = texCoord.x + 1.0 / width;
  float x3 = texCoord.x + 2.0 / width;
  float x4 = texCoord.x + 3.0 / width;
  vec4 col = vec4(0.0);
  float y = 1.0 - texCoord.y;
  for (int i = 0; i < 4; i++ )
  {
      float x = texCoord.x + float(i) / width;
      vec4 colr = texture2D(sTexture, vec2(x, y));
      float comp = (colr.r + colr.g + colr.b) / 3.0;
      col[i] = comp;
  }
  gl_FragColor = col;
}