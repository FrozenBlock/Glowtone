#version 330

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
uniform sampler2D ResolvedSampler;

layout(std140) uniform BloomConfig {
    vec2 BlurDir;
    float Radius;
    float Strength;
    float Near;
    float Far;
    float Reversed;
};

in vec2 texCoord;

out vec4 fragColor;

const float FOLD_MARGIN = 0.05;

float depthAt(sampler2D depths, vec2 uv) {
    float depth = texture(depths, uv).r;
    return Reversed > 0.5 ? 1.0 - depth : depth;
}

float linearise(float depth) {
    if (depth >= 1.0) return Far;

    float ndc = depth * 2.0 - 1.0;
    return (2.0 * Near * Far) / (Far + Near - ndc * (Far - Near));
}

// Improved Transparency gives translucent terrain its own target, with a depth buffer copied before
// entities were drawn — so its emissive is written even where an entity now covers it. Add it back
// only where the depth resolved across every target agrees nothing nearer was drawn on top.
void main() {
    float source = linearise(depthAt(DepthSampler, texCoord));
    float scene = texture(ResolvedSampler, texCoord).r;
    if (source > scene + FOLD_MARGIN) discard;

    fragColor = texture(InSampler, texCoord);
}
