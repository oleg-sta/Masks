// https://www.shadertoy.com/view/ldGXzR#
// doesn't work correctly

precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;

uniform float iGlobalTime;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;

const vec2 iResolution = vec2(200.0, 320.0);
//#define spin
#define wavy

const float PI = 3.14159;

mat2 rot2D(float a){
    return mat2(cos(a), -sin(a),
                sin(a),  cos(a));
}

float circle(vec2 p){
    return length(p);
}

vec2 hash( vec2 p ) {
    p = vec2( dot(p,vec2(127.1,311.7)),
              dot(p,vec2(269.5,183.3)) );
    return -1. + 2.*fract(sin(p+20.)*53758.5453123);
}
float noise( in vec2 p ) {
    vec2 i = floor((p)), f = fract((p));
    vec2 u = f*f*(3.-2.*f);
    return mix( mix( dot( hash( i + vec2(0.,0.) ), f - vec2(0.,0.) ),
                     dot( hash( i + vec2(1.,0.) ), f - vec2(1.,0.) ), u.x),
                mix( dot( hash( i + vec2(0.,1.) ), f - vec2(0.,1.) ),
                     dot( hash( i + vec2(1.,1.) ), f - vec2(1.,1.) ), u.x), u.y);
}

void main( )
{
	float t=iGlobalTime / 10.0;
	vec2 uv = texCoord;
    //uv = uv * 2.0 - 1.0;
    //uv.x *= iResolution.x / iResolution.y;

    uv.y+=noise((uv*.5) + t)*.5;
    uv.x+=noise((uv*.3) + t)*.4;

	gl_FragColor = texture2D(sTexture, uv);
}

// 2016 - Passion
// Reference for the star shape
//https://www.shadertoy.com/view/XlsSR4 'Psycho dots' - nrx


/*
float star(vec2 p){
    return circle(p)*atan(p.y, p.x);
}
*/
