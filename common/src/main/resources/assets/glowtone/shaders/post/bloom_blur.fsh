#version 330

uniform sampler2D InSampler;

layout(std140) uniform BloomConfig {
    vec2 BlurDir;
    float Radius;
    float Strength;
};

in vec2 texCoord;

out vec4 fragColor;

const int SAMPLES = 8;

void main() {
    vec4 blurred = vec4(0.0);
    float total = 0.0;
    float stepSize = Radius / float(SAMPLES);
    float sigma = max(Radius * 0.5, 0.0001);

    for (int i = -SAMPLES; i <= SAMPLES; i++) {
        float offset = float(i) * stepSize;
        float weight = exp(-(offset * offset) / (2.0 * sigma * sigma));
        blurred += texture(InSampler, texCoord + BlurDir * offset) * weight;
        total += weight;
    }

    fragColor = blurred / total;
}
