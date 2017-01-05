precision mediump float;
uniform sampler2D sTexture;
varying vec4 Vertex_UV;
varying vec2 texCoord;
const float PI = 3.1415926535;
uniform vec2 uCenter;

void main()
{
  float aperture = 80.0;
  float apertureHalf = 0.5 * aperture * (PI / 180.0);
  float maxFactor = sin(apertureHalf);

  vec2 uv;
  vec2 xy = 2.0 * (texCoord.xy - uCenter);
  vec2 xy2 = xy;
  xy2.y = xy2.y * 960.0 / 540.0;
  float d = length(xy2);
  if (d < 0.2)
  {
    d = length(xy * maxFactor);
    float z = sqrt(1.0 - d * d);
    float r = atan(d, z) / PI;
    float phi = atan(xy.y, xy.x);

    uv.x = r * cos(phi) + uCenter.x;
    uv.y = r * sin(phi) + uCenter.y;
  }
  else
  {
    uv = texCoord.xy;
  }
  vec4 c = texture2D(sTexture, uv);
  gl_FragColor = c;
}