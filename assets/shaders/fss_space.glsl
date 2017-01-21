// https://www.shadertoy.com/view/4tcGRn


precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;

uniform float iGlobalTime;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;

void main()
{
    vec2 ndc = texCoord - 0.5;
    vec3 lens = normalize(vec3(ndc, 0.05));
	vec3 location = lens * 15.0 + vec3(0.0, 0.0, iGlobalTime);
	vec3 cellId = floor(location);
	vec3 relativeToCell = fract(location);
    vec3 locationOfStarInCell = fract(cross(cellId, vec3(2.154, -6.21, 0.42))) * 0.5 + 0.25;
	float star = max(0.0, 10.0 * (0.1 - distance(relativeToCell, locationOfStarInCell)));
	gl_FragColor = texture2D(sTexture, texCoord) * (1.0 - star) + star * vec4(1.0, 1.0, 1.0, 1.0);
	//gl_FragColor = vec4(star, star, star, 1.0);
}