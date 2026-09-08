#version 330

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

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

const int SAMPLES = 8;
const float WEIGHTS[9] = float[9](
    1.0, 0.96923323, 0.88249690, 0.75483960, 0.60653066,
    0.45783336, 0.32465247, 0.21626517, 0.13533528
);
const float OCCLUSION_MARGIN = 0.15;
const float OCCLUSION_RELATIVE = 0.01;
const float OCCLUSION_SLOPE = 2.0;
const float OCCLUSION_LIMIT = 1.5;
const float OCCLUSION_LIMIT_RELATIVE = 0.05;

float visibility(vec2 uv, float centre, float tolerance) {
    float source = texture(DepthSampler, uv).r;
    if (source >= Far) return 1.0;

    return 1.0 - smoothstep(tolerance, tolerance * 2.0, source - centre);
}

void main() {
    float centre = texture(DepthSampler, texCoord).r;
    float slopeX = abs(dFdx(centre));
    float slopeY = abs(dFdy(centre));
    float slope = BlurDir.x > 0.0 ? slopeX : slopeY;

    float base = max(OCCLUSION_MARGIN, centre * OCCLUSION_RELATIVE);
    float limit = max(OCCLUSION_LIMIT, centre * OCCLUSION_LIMIT_RELATIVE);

    vec4 blurred = vec4(0.0);
    float total = 0.0;
    float stepSize = Radius / float(SAMPLES);

    for (int i = -SAMPLES; i <= SAMPLES; i++) {
        float offset = float(i) * stepSize;
        vec2 uv = texCoord + BlurDir * offset;
        float tolerance = min(base + slope * abs(offset) * OCCLUSION_SLOPE, limit);
        float weight = WEIGHTS[abs(i)] * visibility(uv, centre, tolerance);
        blurred += texture(InSampler, uv) * weight;
        total += weight;
    }

    fragColor = blurred / total;
}
