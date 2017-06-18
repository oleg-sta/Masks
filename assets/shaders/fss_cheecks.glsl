// Barrel distortion
//#version 120 error on adnroid 5 with this line
precision mediump float;
uniform sampler2D sTexture; // texture of original picture
varying vec2 texCoord; // current coodinates on texture
const float PI = 3.1415926535;
uniform vec2 feauturesFace[68]; // points on texture, x0, y0, x1, ...
uniform vec2 sizeScreen; //size of screen in pixels

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
  vec2 uCenter = (2.0*feauturesFace[3]/3.0 + feauturesFace[48]/3.0); // left cheeck
  vec2 uCenter2 = (2.0*feauturesFace[13]/3.0 + feauturesFace[54]/3.0); // right cheeck

  float koef = 0.5 / length(uCenter - feauturesFace[3]);

  vec2 xy = (texCoord.xy - uCenter) * koef;
  float d = length(xy);
  if (d < 1.0)
  {
    uv = Distort(xy);
    uv = uv / koef;
    uv = uv + uCenter;
  }

  koef = 0.5 / length(uCenter2 - feauturesFace[13]);
  xy = (texCoord.xy - uCenter2) * koef;
  d = length(xy);
  if (d < 1.0)
  {
    uv = Distort(xy);
    uv = uv / koef;
    uv = uv + uCenter2;
  }
  vec4 c = texture2D(sTexture, uv);
  gl_FragColor = c; // output color
}
