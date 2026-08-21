#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

#define PI 3.14159265359

vec3 animateFoliage(vec3 pos, float gameTime) {
    float xOffset = 0.0;
    float yOffset = 0.0;
    float zOffset = 0.0;

    vec3 repeatPos = pos / 2.0 * PI;
    float animTime = time * 4000.0;

    xOffset = sin(repeatPos.x + (repeatPos.y / 2.0) + animTime) / 32.0;
    yOffset = cos(repeatPos.z + (repeatPos.y / 2.0) + animTime) / 32.0;

    return vec3(xOffset, yOffset, zOffset);
}

void main() {
    vec3 pos = (Position + animateFoliage(Position, GameTime)) + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
