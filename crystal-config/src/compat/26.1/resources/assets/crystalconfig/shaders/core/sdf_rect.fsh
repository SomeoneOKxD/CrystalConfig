#version 150

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

in vec4 vertexColor;
in vec2 uv;
in float radiusNorm;

out vec4 fragColor;

float roundedBoxSdf(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 uvDx = dFdx(uv);
    vec2 uvDy = dFdy(uv);

    float fwX = length(vec2(uvDx.x, uvDy.x));
    float fwY = length(vec2(uvDx.y, uvDy.y));

    vec2 rectSize = vec2(
        fwX > 0.000001 ? 1.0 / fwX : 1.0,
        fwY > 0.000001 ? 1.0 / fwY : 1.0
    );

    vec2 halfSize = rectSize * 0.5;
    vec2 p = (uv - 0.5) * rectSize;

    float radius = clamp(radiusNorm * min(rectSize.x, rectSize.y), 0.0, min(rectSize.x, rectSize.y) * 0.5);

    float d = roundedBoxSdf(p, halfSize, radius);

    float aa = max(fwidth(d), 0.75);
    float alpha = 1.0 - smoothstep(-aa, aa, d);

    vec4 color = vertexColor * ColorModulator;
    color.a *= alpha;

    if (color.a <= 0.001) {
        discard;
    }

    fragColor = color;
}