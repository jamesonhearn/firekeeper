#version 120

uniform sampler2D u_texture;
uniform sampler2D u_occlusionMap;

uniform vec2 u_cameraOffset;
uniform vec2 u_viewSize;
uniform vec2 u_viewOrigin;

uniform float u_falloff;
uniform float u_ambient;
uniform int u_lightCount;

const int MAX_LIGHTS = 16;

uniform vec2  u_lightPos[MAX_LIGHTS];
uniform vec3  u_lightColor[MAX_LIGHTS];
uniform float u_lightIntensity[MAX_LIGHTS];
uniform float u_lightRadius[MAX_LIGHTS];

varying vec4 v_color;
varying vec2 v_texCoords;

/* 5x5 Gaussian kernel */
const float gaussianKernel[25] = float[25](
1.0,  4.0,  7.0,  4.0, 1.0,
4.0, 16.0, 26.0, 16.0, 4.0,
7.0, 26.0, 41.0, 26.0, 7.0,
4.0, 16.0, 26.0, 16.0, 4.0,
1.0,  4.0,  7.0,  4.0, 1.0
);

const float gaussianSum = 273.0;

void main() {

    // - Screen -> world position -
    vec2 pixel = v_texCoords * u_viewSize;

    vec2 worldPos = vec2(
    u_viewOrigin.x + pixel.x + u_cameraOffset.x,
    u_viewOrigin.y + (u_viewSize.y - pixel.y) + u_cameraOffset.y
    );

    // - Base lighting -
    vec3 totalLight = vec3(u_ambient);

    // Precompute once
    float invMaxLights = 1.0 / float(MAX_LIGHTS);
    vec2 blurStep = vec2(
    invMaxLights / u_viewSize.x,
    1.0 / u_viewSize.y
    );

    float falloffExp = max(u_falloff, 0.0001);

    // - Light loop -
    for (int i = 0; i < MAX_LIGHTS; ++i) {
        if (i >= u_lightCount) break;

        // UV into this light's occlusion slice
        vec2 baseUV = vec2(
        (v_texCoords.x + float(i)) * invMaxLights,
        v_texCoords.y
        );

        //  Gaussian filtered visibility 
        float visibility = 0.0;
        int k = 0;

        for (int y = -2; y <= 2; ++y) {
            for (int x = -2; x <= 2; ++x) {
                vec2 offset = vec2(float(x), float(y)) * blurStep;
                visibility += texture2D(u_occlusionMap, baseUV + offset).a
                * gaussianKernel[k++];
            }
        }

        visibility *= (1.0 / gaussianSum);

        //  Distance attenuation 
        float dist = distance(worldPos, u_lightPos[i]);
        float normDist = clamp(dist / u_lightRadius[i], 0.0, 1.0);

        float falloff = pow(normDist, falloffExp);
        float distanceLight = 1.0 - smoothstep(0.0, 1.0, falloff);

        //  Accumulate 
        float strength = visibility * distanceLight * u_lightIntensity[i];
        totalLight += u_lightColor[i] * strength;
    }

    // - Convert lighting -> alpha mask -
    float brightness = clamp(
    max(totalLight.r, max(totalLight.g, totalLight.b)),
    0.0, 1.0
    );

    float alpha = 1.0 - brightness;

    vec4 base = texture2D(u_texture, v_texCoords) * v_color;
    gl_FragColor = vec4(base.rgb, alpha);
}
