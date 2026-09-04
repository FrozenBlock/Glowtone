#version 330

uniform sampler2D DepthSampler;

layout(std140) uniform BloomConfig {
    vec2 TexelSize;
    float Radius;
    float Strength;
    float Near;
    float Far;
    float Reversed;
};

in vec2 texCoord;

out vec4 fragColor;

float depthAt(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    return Reversed > 0.5 ? 1.0 - depth : depth;
}

float linearise(float depth) {
    if (depth >= 1.0) return Far;

    float ndc = depth * 2.0 - 1.0;
    return (2.0 * Near * Far) / (Far + Near - ndc * (Far - Near));
}

// Half-resolution linear scene depth, taken while the world depth is still intact.
// The nearest of the four covered texels wins, so silhouettes stay on the occluding side.
void main() {
    vec2 corner = TexelSize * 0.5;
    float nearest = min(
        min(depthAt(texCoord + vec2(-corner.x, -corner.y)), depthAt(texCoord + vec2(corner.x, -corner.y))),
        min(depthAt(texCoord + vec2(-corner.x, corner.y)), depthAt(texCoord + vec2(corner.x, corner.y)))
    );

    fragColor = vec4(linearise(nearest), 0.0, 0.0, 0.0);
}
