precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;
uniform int u_facing;

// shader from convert NV21 to RGBA

void main() {
  //gl_FragColor = vec4(0.5f, 0.5f, 0.5f, 0.5f);
  //gl_FragColor = texture2D(sTexture,texCoord) / 2.0;
  vec4 c1 = vec4(1.0, texCoord.x, 0.0, 1.0);
  vec2 coord = vec2(texCoord.y, texCoord.x);
  if (u_facing == 0) coord.x = 1.0 - coord.x;
  coord.y = 2.0 / 3.0 - coord.y / 1.5;

  vec2 realCoord = vec2(coord.x * 960.0, coord.y * 540.0);
  vec4 c2 = texture2D(sTexture, coord);
  float y = c2[int(mod(realCoord.x, 4.0))];



  gl_FragColor = vec4(y, y, y, 1.0);
}