#version 330

uniform sampler2D InSampler;

layout(std140) uniform BloomConfig {
    vec2 BlurDir;
    float Radius;
    float Strength;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 bloom = texture(InSampler, texCoord).rgb;
    fragColor = vec4(bloom * Strength, 0.0);
}
