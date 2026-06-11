#version 150

uniform sampler2D Sampler0;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 unitRange = vec2(2.0) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    vec3 msd = texture(Sampler0, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    float alpha = clamp(screenPxRange() * (sd - 0.5) + 0.5, 0.0, 1.0);

    if (alpha <= 0.001) {
        discard;
    }

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}
