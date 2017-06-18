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
    float BarrelPower = 0.5;
    radius = pow(radius, BarrelPower);
    p.x = radius * cos(theta);
    p.y = radius * sin(theta);
    return p;
}

void main()
{
  vec2 uv = texCoord.xy;

  //Multiplicator for mouth
  float multMouthVertical = 3.0;
  float multMouthHorizontal = 1.5;

  //Center of the mouth
  vec2 mouthCenter = (feauturesFace[60] + feauturesFace[62] + feauturesFace[66] + feauturesFace[64]) / 4.0;

  //Ellipse sizes
  float smallOutside = multMouthVertical * length(feauturesFace[51] - feauturesFace[57])/2.0;
  float largeOutside = multMouthHorizontal * length(feauturesFace[48] - feauturesFace[54])/2.0;
  float smallInside = length(feauturesFace[62] - feauturesFace[66])/2.0;
  float largeInside = length(feauturesFace[60] - feauturesFace[64])/2.0;

  //Angle of the ellipse
  float cosAlpha = (feauturesFace[54].x - feauturesFace[48].x) / length(feauturesFace[48] - feauturesFace[54]);
  float sinAlpha = (feauturesFace[48].y - feauturesFace[54].y) / length(feauturesFace[48] - feauturesFace[54]);

  //Check that the mouth is closed
  if(smallInside / largeInside < 1.00)//Closed mouth
  {
    //Make the center
    vec2 xy = texCoord.xy - mouthCenter;
    vec2 newXY;

    //Then we have to rotate the coordinates
    float X_rotated = xy.x * cosAlpha - xy.y * sinAlpha;
    float Y_rotated = xy.x * sinAlpha + xy.y * cosAlpha;

    //Now we have to normalize the coordinates to 1
    X_rotated = X_rotated / largeOutside;
    Y_rotated = Y_rotated / smallOutside;

    newXY.x = X_rotated;
    newXY.y = Y_rotated;

    //Now we check that we are inside the mouth area
    if(X_rotated*X_rotated + Y_rotated*Y_rotated < 1.0)
    {
        //Make distortion
        uv = Distort(newXY);
        //uv = newXY;

        //Make the trasformations back
        uv.x = uv.x * largeOutside;
        uv.y = uv.y * smallOutside;
        X_rotated = uv.x * cosAlpha + uv.y * sinAlpha;
        Y_rotated = -uv.x * sinAlpha + uv.y * cosAlpha;
        uv.x = X_rotated + mouthCenter.x;
        uv.y = Y_rotated + mouthCenter.y;
    }
    else
    {
        //Make the trasformations back
        uv = newXY;
        uv.x = uv.x * largeOutside;
        uv.y = uv.y * smallOutside;
        X_rotated = uv.x * cosAlpha + uv.y * sinAlpha;
        Y_rotated = -uv.x * sinAlpha + uv.y * cosAlpha;
        uv.x = X_rotated + mouthCenter.x;
        uv.y = Y_rotated + mouthCenter.y;
    }
  }

  vec4 c = texture2D(sTexture, uv);
  gl_FragColor = c; // output color
}