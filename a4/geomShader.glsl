#version 430

layout (triangles) in;

in vec3 varyingNormal[];
in vec3 varyingLightDir[];
in vec3 varyingHalfVector[];

out vec3 varyingNormalG;
out vec3 varyingLightDirG;
out vec3 varyingHalfVectorG;

layout (triangle_strip, max_vertices=9) out;

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

void main (void)
{	if (mod(gl_PrimitiveIDIn,3)!=0)
	{	for (int i=0; i<3; i++)
		{	gl_Position = p_matrix * v_matrix * gl_in[i].gl_Position;
			varyingNormalG = varyingNormal[i];
			varyingLightDirG = varyingLightDir[i];
			varyingHalfVectorG = varyingHalfVector[i];
			EmitVertex();
		}
	}
	EndPrimitive();
}
