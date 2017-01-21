// Object highlight from shadertoy but doesn't work

precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;

uniform float iGlobalTime;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;


// highlight parameters
const float waveHeight = 0.5;
const float waveSpeed = 0.5;
const float wavePause = 0.8;
const vec3 highlightCol = vec3(0.7, 0.7, 0.9);

// scene parameters
const float highlightRadius = 0.43;
const float sRadius = 0.4;

const vec3 lightPos = vec3(1.3, 1.4, 2.5);
const vec3 viewerPos = vec3(0., 0., 4.);
const vec3 ambientCol = vec3(0.2, 0.2, 0.4);
const vec3 lightCol = vec3(0.35, 0.2, 0.2);

vec3 computeSpherePos(vec2 p, vec2 center, float r)
{
    vec2 local = p - center;
    return vec3(local, sqrt(r * r - dot(local, local)));
}

vec2 computeSphereMapCoord(vec3 c, float r)
{
    return vec2(atan(c.z, c.x), acos(c.y / r));
}

void main()
{
	vec2 p = texCoord.xy;
    p.x *= 200.0/320.0;

    vec2 sCenter = 0.5 * vec2(200.0/320.0, 1.);

    vec3 sPos = computeSpherePos(p, sCenter, sRadius);
    vec3 sNorm = normalize(sPos);
    vec3 lightDir = normalize(lightPos - sPos);

    float cosIncidence = dot(lightDir, sNorm);
    float diffuse = clamp(cosIncidence, 0., 1.);

    vec3 refl = reflect(lightDir, sNorm);
    vec3 viewDir = normalize(sPos - viewerPos);

    float specularTerm = clamp(dot(viewDir, refl), 0., 1.);
    float specular = pow(specularTerm, 4.);

    vec3 map = texture2D(sTexture, computeSphereMapCoord(sPos, sRadius)).xyz;
    vec3 col = ambientCol * map + lightCol * (diffuse + specular);

    float inBackground = dot(p - sCenter, p - sCenter) - sRadius * sRadius;
    col *= inBackground > 0.0 ? vec3(0.66, 0.83, 0.55) : vec3(1);

    vec3 highlightPos = computeSpherePos(p, sCenter, highlightRadius);
    float wave = mod(highlightPos.y - iGlobalTime * waveSpeed, waveHeight + wavePause); // [0, h+p]
	wave = max(0., wave - wavePause) / waveHeight; // [0, 1]

	if (dot(p - sCenter, p - sCenter) < highlightRadius * highlightRadius) {
        col = mix(col, highlightCol, wave / 3.);
    }

    gl_FragColor = vec4(col, 1.0);
}