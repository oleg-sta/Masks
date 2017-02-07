precision mediump float;
uniform sampler2D sTexture; // y - texture
uniform sampler2D sTexture2; //uv texture
varying vec2 texCoord;
uniform int u_facing;
uniform float cameraWidth;
uniform float cameraHeight;

// shader from convert NV21 to RGBA

void main() {
  vec4 c1 = vec4(1.0, texCoord.x, 0.0, 1.0);
  vec2 coord = vec2(texCoord.y, texCoord.x);
  if (u_facing == 0) coord.x = 1.0 - coord.x;
  coord.y = 1.0 - coord.y;

  vec2 realCoord = vec2(coord.x * cameraWidth, coord.y * cameraHeight);
  vec2 uCoord = vec2(floor(realCoord.x / 2.0) * 2.0 / cameraWidth, floor(realCoord.y / 2.0) * 2.0 / cameraHeight);
  vec2 vCoord = vec2((floor(realCoord.x / 2.0) * 2.0 + 1.0) / cameraWidth, floor(realCoord.y / 2.0) * 2.0 / cameraHeight);

  float y = texture2D(sTexture, coord).r;
  float u = texture2D(sTexture2, uCoord).r;
  float v = texture2D(sTexture2, vCoord).r;
  vec4 color;
  color.r = (1.164 * (y - 0.0625)) + (1.596 * (v - 0.5));
  color.g = (1.164 * (y - 0.0625)) - (0.391 * (u - 0.5)) - (0.813 * (v - 0.5));
  color.b = (1.164 * (y - 0.0625)) + (2.018 * (u - 0.5));
  color.a = 1.0;
  gl_FragColor = color;
}