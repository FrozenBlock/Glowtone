#version 330

uniform sampler2D DepthSampler;

layout(std140) uniform EdgeConfig {
    vec2 TexelSize;
    float Size;
    float Strength;
    float Near;
    float Far;
    float MaxDistance;
    float Threshold;
    float Reversed;
};

in vec2 texCoord;

out vec4 fragColor;

float depthAt(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    return Reversed > 0.5 ? 1.0 - depth : depth;
}

float linearise(float depth) {
    float ndc = depth * 2.0 - 1.0;
    return (2.0 * Near * Far) / (Far + Near - ndc * (Far - Near));
}

void main() {
    float raw = depthAt(texCoord);
    float centre = linearise(raw);

    vec2 step = TexelSize * Size;

    float slope = fwidth(centre) * Size;

    if (raw >= 1.0) discard;

    float behind = 0.0;
    behind = max(behind, linearise(depthAt(texCoord + vec2( step.x, 0.0))) - centre);
    behind = max(behind, linearise(depthAt(texCoord + vec2(-step.x, 0.0))) - centre);
    behind = max(behind, linearise(depthAt(texCoord + vec2(0.0,  step.y))) - centre);
    behind = max(behind, linearise(depthAt(texCoord + vec2(0.0, -step.y))) - centre);
    if (behind <= 0.0) discard;

    float limit = max(slope * 2.0, centre * Threshold);
    float edge = smoothstep(limit, limit * 3.0, behind);
    if (edge <= 0.0) discard;

    float fade = 1.0 - smoothstep(MaxDistance * 0.75, MaxDistance, centre);
    if (fade <= 0.0) discard;

    fragColor = vec4(vec3(edge * Strength * fade), 1.0);
}
