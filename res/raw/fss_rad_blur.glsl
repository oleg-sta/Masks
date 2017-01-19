// Radial Blur(volumentric light effect)

precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;
#define T texture2D(sTexture,.5+(p.xy*=.992))

void main()
{
  vec3 p = vec3(texCoord.xy, 0.0) - 0.5;
  vec3 o = T.rbb;
  for (float i=0.;i<100.;i++)
    p.z += pow(max(0.,.5-length(T.rg)),2.)*exp(-i*.08);
  gl_FragColor=vec4(o*o+p.z,1);
  //gl_FragColor=texture2D(sTexture, texCoord);
}