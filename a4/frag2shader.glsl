#version 430

in vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec;
in vec4 shadow_coord;
in vec2 tc;
in vec3 vertEyeSpacePos;
out vec4 fragColor;
 
struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};

struct Material
{	vec4 ambient, diffuse, specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;

uniform float hueShift;
uniform float alpha;
uniform float flipNormal;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D s;

void main(void)
{	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
	vec3 H = normalize(varyingHalfVec);

	vec4 fogColor = vec4(0.7, 0.8, 0.9, 1.0);	// bluish gray
	float fogStart = 4;
	float fogEnd = 5.8;
	
	float notInShadow = textureProj(shadowTex, shadow_coord);
	float dist = length(vertEyeSpacePos.xyz);
	float fogFactor = clamp(((fogEnd-dist)/(fogEnd-fogStart)), 0.0, 1.0);

	fragColor = mix(fogColor, (globalAmbient * material.ambient
				+ light.ambient * material.ambient + texture(s,tc)), fogFactor);

	if (notInShadow == 1.0)
	{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
				+ light.specular * material.specular
				* pow(max(dot(H,N),0.0),material.shininess*3.0);
	}

	//Trippy Hue Effect
	float hue, chroma;
    vec4 addedColor = texture(s,tc);

	vec4 R, G, B, Y, I, Q;

	//Approximate conversion ratios
    R = vec4(1.0, 0.8, 0.6, 0.0); G = vec4(1.0, -0.3, -0.6, 0.0); B = vec4(1.0, -1.0, 1.6, 0.0);
	Y = vec4(0.4, 0.5, 0.2, 0.0); I = vec4(0.5, -0.4, -0.3, 0.0); Q = vec4(0.4, -0.4, 0.2, 0.0);

	//YIQ space instead of typical HLS / HSV color system
	//Adjust chroma levels after in YIQ space
    chroma = sqrt(pow(dot(addedColor, Q), 2) + pow(dot(addedColor, I), 2));
    hue = atan(dot(addedColor, I), dot(addedColor, Q)) + hueShift;

    vec4 YIQ = vec4(dot(addedColor, Y), (chroma * cos(hue)), (chroma * sin(hue)), 0.0);
    addedColor.r = dot(YIQ, R); addedColor.g = dot(YIQ, G); addedColor.b = dot(YIQ, B);

    fragColor *= addedColor*2;

	//Transparency w/ new added color applied for color changing effect
    //fragColor *= addedColor*2;
	fragColor = vec4(fragColor.xyz, alpha);
}
