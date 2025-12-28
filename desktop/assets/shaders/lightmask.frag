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
uniform vec2 u_lightPos[MAX_LIGHTS];
uniform vec3 u_lightColor[MAX_LIGHTS];
uniform float u_lightIntensity[MAX_LIGHTS];
uniform float u_lightRadius[MAX_LIGHTS];

varying vec4 v_color;
varying vec2 v_texCoords;

const float gaussianKernel[25] = float[25](
1.0, 4.0, 7.0, 4.0, 1.0,
4.0, 16.0, 26.0, 16.0, 4.0,
7.0, 26.0, 41.0, 26.0, 7.0,
4.0, 16.0, 26.0, 16.0, 4.0,
1.0, 4.0, 7.0, 4.0, 1.0
);
const float gaussianSum = 273.0;

void main() {
    vec2 tileCoord = v_texCoords * u_viewSize;

    // Continuous world position
    vec2 worldPos = vec2(
    u_viewOrigin.x + tileCoord.x + u_cameraOffset.x,
    u_viewOrigin.y + (u_viewSize.y - tileCoord.y) + u_cameraOffset.y
    );

    vec3 totalLight = vec3(u_ambient);
    vec2 baseTexel = vec2(1.0 / (u_viewSize.x * float(MAX_LIGHTS)), 1.0 / u_viewSize.y);

    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= u_lightCount) {
            break;
        }
        vec2 occlusionCoord = vec2((v_texCoords.x + float(i)) / float(MAX_LIGHTS), v_texCoords.y);
        float visibility = 0.0;
        int index = 0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                vec2 offset = vec2(float(dx), float(dy)) * baseTexel;
                float sample = texture2D(u_occlusionMap, occlusionCoord + offset).a;
                visibility += sample * gaussianKernel[index];
                index++;
            }
        }
        visibility /= gaussianSum;

        float dist = distance(worldPos, u_lightPos[i]);
        float normalizedDistance = clamp(dist / u_lightRadius[i], 0.0, 1.0);
        float distanceLight = 1.0 - smoothstep(0.0, 1.0, pow(normalizedDistance, max(u_falloff, 0.0001)));
        float light = visibility * distanceLight * u_lightIntensity[i];
        totalLight += u_lightColor[i] * light;
    }

    float brightness = clamp(max(max(totalLight.r, totalLight.g), totalLight.b), 0.0, 1.0);
    float alpha = 1.0 - brightness;

    vec4 base = texture2D(u_texture, v_texCoords) * v_color;
    gl_FragColor = vec4(base.rgb, alpha);
}
