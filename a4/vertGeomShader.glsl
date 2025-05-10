#version 430

layout (location=0) in vec4 vertPos;
layout (location=1) in vec4 vertNormal;

out vec3 varyingNormal; 
out vec3 varyingLightDir;
out vec3 varyingHalfVector;

struct PositionalLight
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
};
struct Material
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform int enableLighting;

void main(void)
{
	varyingNormal = (norm_matrix * vertNormal).xyz;
	varyingLightDir = light.position - (m_matrix*vertPos).xyz;	
	varyingHalfVector = normalize(varyingLightDir) + normalize(varyingNormal);
	gl_Position = m_matrix * vertPos;
}
