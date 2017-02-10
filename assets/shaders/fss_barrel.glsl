// Barrel distortion
//#version 120 error on adnroid 5 with this line
precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;
const float PI = 3.1415926535;
//uniform float BarrelPower;
uniform vec2 uCenter;
uniform vec2 uCenter2;
uniform vec2 uRadius;

vec2 Distort(vec2 p)
{
    float theta  = atan(p.y, p.x);
    float radius = length(p);
    float BarrelPower = 2.0;
    radius = pow(radius, BarrelPower);
    p.x = radius * cos(theta);
    p.y = radius * sin(theta);
    //return 0.5 * (p + 1.0);
    return p;
}

void main()
{
  vec2 uv = texCoord.xy;

  vec2 xy = (texCoord.xy - uCenter) * 10.0;
  float d = length(xy);
  if (d < 1.0)
  {
    uv = Distort(xy);
    uv = uv / 10.0;
    uv = uv + uCenter;
  }
  xy = (texCoord.xy - uCenter2) * 10.0;
  d = length(xy);
  if (d < 1.0)
  {
    uv = Distort(xy);
    uv = uv / 10.0;
    uv = uv + uCenter2;
  }
  vec4 c = texture2D(sTexture, uv);
  gl_FragColor = c;
}