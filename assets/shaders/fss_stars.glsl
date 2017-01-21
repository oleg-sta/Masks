// Rotating stars https://www.shadertoy.com/view/Mls3WH

precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;

uniform float iGlobalTime;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;

const vec2 iResolution = vec2(200.0, 320.0);

void main( )
{
	float x = texCoord.x;
    vec2 p = texCoord.xy;
	vec2 c = p - vec2(0.25, 0.5);

    vec3 col = texture2D(sTexture, texCoord).rgb;
    vec3 starcol = vec3 (1.0, 1.0, 0.0);

    float rot = iGlobalTime * 1.2;

    //stars
    for (int i = 0 ; i < 8 ; i++) {
    c = p - vec2 (0.0 + 0.25 * sin ( - rot - float(i) * 0.78 ), 0.0 + 0.25 * cos ( - rot - float(i) * 0.78 )) - uCenter;
    float r = 0.05 + 0.009 * cos  ( atan (c.y, c.x) * 5.0 + rot );
  	float f = smoothstep(r, r + 0.01, length(c));
    float bg = smoothstep(r, r + 0.01, length(c));

    col *= f; //leave void for each star
    col += starcol * (1.0 - f ); //fill stars with color

    f = smoothstep(r+.01, r+ 0.04, length(c));
    col += vec3 (0.8, 0.3, 0.0) * (1.0 - f); //add orange glow

    }

    gl_FragColor = vec4 (col, 1.0);
    //bars

    float pat = iGlobalTime*5.0;
    float rep = 120.0;
    vec3 col2 = vec3(0.5 + 0.5 * sin(x/rep + 3.14 + pat), 0.5 + 0.5 * cos (x/rep + pat), 0.5 + 0.5 * sin (x/rep + pat));
    if ( p.y > 0.1 && p.y < 0.106 || p.y > 0.9 && p.y < 0.906 ) {
    gl_FragColor = vec4 ( col2, 1.0 );

    }
}