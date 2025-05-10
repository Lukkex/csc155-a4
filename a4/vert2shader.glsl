#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec3 vertNormal;
layout (location = 2) in vec2 tex_coord;

out vec3 vertEyeSpacePos;
out vec2 tc;
out vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec; 
out vec4 shadow_coord;

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

uniform float alpha;
uniform float flipNormal;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D s;

void main(void)
{	//output the vertex position to the rasterizer for interpolation
	varyingVertPos = (m_matrix * vec4(vertPos,1.0)).xyz;
        
	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	
	// calculate the half vector (L+V)
	varyingHalfVec = (varyingLightDir-varyingVertPos).xyz;
	
	shadow_coord = shadowMVP * vec4(vertPos,1.0);
	
	gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
	tc = tex_coord;
	vec4 p = vec4(vertPos,1.0);
	vertEyeSpacePos = (v_matrix * m_matrix * p).xyz;
}
