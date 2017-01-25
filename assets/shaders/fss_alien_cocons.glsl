//https://www.shadertoy.com/view/XsfGWN#

precision mediump float;
uniform sampler2D sTexture;
varying vec2 texCoord;
uniform sampler2D u_Texture2;

uniform float iGlobalTime;

// for compatibility
uniform vec2 uCenter;
uniform vec2 uCenter2;



const vec2 iResolution = vec2(400.0, 400.0);
const vec3 iMouse = vec3(200.0, 200.0, 0.0);


float sphere(vec3 p, float r)
{
	return length(p) - r;
}



float sdBox( vec3 p, vec3 b )
{
  vec3 d = abs(p) - b;
  return min(max(d.x,max(d.y,d.z)),0.0) +
         length(max(d,0.0));
}


float sdTorus( vec3 p, vec2 t )
{
  vec2 q = vec2(length(p.xz)-t.x,p.y);
  return length(q)-t.y;
}




float smink( float a, float b, float k )
{
    float h = clamp( 0.5+0.5*(b-a)/k, 0.0, 1.0 );
    return mix( b, a, h ) - k*h*(1.0-h);
}



#define MAT_MORPH 16.0
#define MAT_S6_WATER 19.0

#define EPS 0.01


vec2 un(vec2 a, vec2 b)
{
	return a.x < b.x ? a : b;
}



vec2 sunk(vec2 a, vec2 b, float k)
{
	float sm = smink(a.x,b.x, k);
	float m = min(a.x, b.x);
	float ca = abs(sm -a.x);
	float cb = abs(sm -b.x);

	return ca < cb ? vec2(sm, a.y) : vec2(m, b.y);
}



vec2 water_6(vec3 p, vec3 rd)
{
	/*float t = mod(iGlobalTime,30.0); //tick - SCENE_2;
	if(rd.y > 0.0){
		return vec2(999999, MAT_S6_WATER);
	}

	float d = (sin(p.x + tick*0.5) + sin(p.z  + tick*0.5)) * 0.1;// +
	//length(texture(noiseP, p.xz*0.5 + vec2(0, tick*0.1)))*0.1 +
	//length(texture(noiseP, p.xz*0.5 + vec2(tick*0.13, 0)))*0.1;
	d *= 0.1;

	float h = p.y - d * 0.1;

	float dis = (0.1 -p.y)/rd.y;

	return vec2(max(h, dis), MAT_S6_WATER);*/
    return vec2(p.y,MAT_S6_WATER);
}



bool inRefraction = false;



vec2 scene(vec3 po, vec3 rd)
{
	float tic = mod(iGlobalTime,30.0);
	vec3 cm = vec3(5);
	vec3 q = mod(po, cm)-0.5*cm;
	float dis = length(po.xz);
	vec3 p = vec3(q.x, po.y - min(0.0, tic * 1.5 - dis), q.z);

	float a = sdBox(p, vec3(1.0));
	float b = sdTorus(p, vec2(1,0.3));
	float c = min(sdTorus(p - vec3(2,0,0), vec2(1,0.3)), sdTorus(p + vec3(2,0,0), vec2(1,0.3)));
	float d = sphere(p, 1.0);

	float t = 5.0;
	float ti = tic - sqrt(po.x*po.x + po.z*po.z) * 0.7;

	float t1 = smoothstep(t*0.0, t*1.0, ti);
	float t2 = smoothstep(t*1.0, t*2.0, ti);
	float t3 = smoothstep(t*2.0, t*3.0, ti);

	float res = a*(1.0-t1) + b*t1*(1.0-t2) + c*t2*(1.0- t3) + d*t3;

	return sunk(water_6(po - vec3(0, -1, 0), rd), vec2(res, MAT_MORPH), 0.8);
}



vec3 getNormal(vec3 p, vec3 rd, vec3 ro)
{
    vec3 normal;
    vec3 ep = vec3(0.01, 0, 0);
    normal.x = scene(p + ep.xyz, rd).x - scene(p - ep.xyz, rd).x;
    normal.y = scene(p + ep.yxz, rd).x - scene(p - ep.yxz, rd).x;
    normal.z = scene(p + ep.yzx, rd).x - scene(p - ep.yzx, rd).x;
    return normalize(normal);
}


float specular(vec3 normal, vec3 light, vec3 viewdir, float s)
{
	float nrm = (s + 8.0) / (3.1415 * 8.0);
	float k = max(0.0, dot(viewdir, reflect(light, normal)));
    return  pow(k, s);
}




#define jumps 2
#define imax 600
void mainImage(out vec4 fragColor, in vec2 fragCoord)
{
	vec3 eye = vec3(0);
	vec3 light = vec3(0);
	vec3 tar = vec3(0);
	float lightInvSize = 0.5;
	float shadowAmbient = 0.3;
	float lightIntensity = 0.004;


	float refJumpDistance = 0.02;



	bool cubicDis = false;
	vec3 rollV = vec3(0, 1, 0);
	float time = mod(iGlobalTime,30.0);
    if (time < 15.0) {
        eye = vec3(sin(time * 0.3) * 10.0, 3.0, cos(time * 0.3) * 10.0);
        light = eye + vec3(0, 3, 0);
    } else {
        eye = vec3(sin(time * 0.3) * 10.0, 3.0, cos(time * 0.3) * 10.0);
        eye += vec3(0, 0, time - 15.0);
        tar = mix(vec3(0.0), eye + vec3(0.0, -0.5, -1.0), smoothstep(0.0, 10.0, time - 15.0));
    }
    light = tar + vec3(sin(time * 0.5) * 3.0, 3.0, cos(time * 0.5) * 3.0);



    lightIntensity = 0.001;
    lightInvSize = 100.0;
    float tmax = 70.0;
    cubicDis = true;
	vec3 dir = normalize(tar - eye);
	vec3 right = normalize(cross(rollV, dir));
 	vec3 up = cross(dir, right);

    float u = (fragCoord.x / iResolution.x) * 2.0 - 1.0;
    float v = ((fragCoord.y / iResolution.y) * 2.0 - 1.0) * (iResolution.y/iResolution.x);

    vec3 color = vec3(0.0);

    float t = 0.0;
	vec3 ro = eye;
	vec3 rd = normalize(dir + right*u + up*v);

	float ref = 1.0;
	float lightAura = 0.0;
    float breakVar = 0.0;
	for(int j = 0; j < jumps; ++j)
    {
        if (breakVar > 0.5) {
            break;
        }
    	t = 0.0;
    	 for(int i = 0; i < imax; ++i)
   		 {
             if (t >= tmax) {
             	break;
             }
	        vec3 p = ro + rd * t;
	        vec2 dm = scene(p, rd);
	        float d = dm.x;
	        float m = dm.y;


	        if(d < EPS || i == imax || t >= tmax) //d < 0.001
	        {
	        	vec3 x0 = light;
	        	vec3 x1 = ro;
	        	vec3 x2 = ro + rd;
	        	float ldis = pow(length(cross(x2 - x1, x1 - x0)),2.0) / pow( distance(x2, x1), 2.0);
	        	vec3 normal = getNormal(p, rd, ro);

				vec3 invLight = normalize(light - p);
	        	float diffuse = max(0.,dot(invLight, normal));
	        	vec3 refrd = reflect(rd, normal);


	        	vec3 n = floor(p);
				vec3 c = vec3(0.5);


				 if (m == MAT_MORPH) {
					vec3 pc = p + vec3(90);
	        		vec3 matCol = vec3(pc.x/10.0, (pc.x + pc.z) / 5.0, pc.z/8.0);
	        		c = (sin(matCol) + 1.0) * 0.5;
				} else if (m == MAT_S6_WATER) {
					c = vec3(0.6, 0.6, 1.0);
				}



                c = 0.7*c* (1.0 + diffuse);

                c += specular(normal, -invLight, normalize(eye - p), 70.0);


	            float dis = length(light - p);
	            float disFact = 1.0 / (1.0 + lightIntensity*dis*dis * (cubicDis ? dis : 1.0 ));
	            c *= disFact;


	        	float tl = -dot(x1 - x0, x2 - x1)/pow(distance(x2,x1), 2.0);

	        	lightAura = max(lightAura, 1.0/(0.01 + lightInvSize*ldis));


				color = mix(color, c, ref);

				if( m == MAT_S6_WATER){

                    rd = reflect(rd, normal);
                    ro = p + rd*0.02;

				} else {
					rd = reflect(rd, normal);
	         		ro = p + rd*refJumpDistance;
				}

	        	if (m == MAT_S6_WATER) {
					ref *= 0.8;
	       		} else if (m == MAT_MORPH) {
	        		ref *= 0.4;
	        	} else {
		        	ref = 0.0;
	        	}
	        	if (ref <= 0.01) {
					breakVar = 1.0;
	        	}
	           	break;
	        }

	        t += d;
    	}
    }


    fragColor = vec4(color + vec3(lightAura),  1.0);
}


void main()
{
    vec4 re;
    mainImage(re, texCoord * 400.0);
    gl_FragColor = re;
}