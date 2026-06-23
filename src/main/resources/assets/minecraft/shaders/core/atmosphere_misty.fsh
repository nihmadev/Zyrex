#version 150

uniform vec2 Resolution;
uniform float Time;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(443.8975, 397.2973));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    float gradient = 1.0 - uv.y;

    vec3 topFog = vec3(0.5, 0.52, 0.56);
    vec3 midFog = vec3(0.58, 0.6, 0.64);
    vec3 bottomFog = vec3(0.65, 0.67, 0.7);

    vec3 skyColor = mix(bottomFog, midFog, gradient);
    skyColor = mix(skyColor, topFog, pow(gradient, 0.7));

    vec2 mistUv = uv * 2.5 + vec2(Time * 0.005, Time * 0.002);
    float mist1 = sin(mistUv.x * 1.5 + mistUv.y * 1.0 + Time * 0.01) * 0.5 + 0.5;
    float mist2 = sin(mistUv.x * 2.8 - mistUv.y * 1.5 + Time * 0.008) * 0.5 + 0.5;
    float mist3 = sin(mistUv.x * 4.0 + mistUv.y * 2.5 + Time * 0.005) * 0.5 + 0.5;
    float mist = (mist1 * 0.4 + mist2 * 0.35 + mist3 * 0.25);

    float mistAlpha = mist * 0.25 * (0.5 + 0.5 * (1.0 - gradient));
    vec3 mistColor = vec3(0.62, 0.64, 0.68);
    skyColor = mix(skyColor, mistColor, mistAlpha);

    float fogDensity = exp(-gradient * 1.5) * 0.25;
    vec3 fogColor = vec3(0.68, 0.7, 0.73);
    skyColor = mix(skyColor, fogColor, fogDensity);

    float sunGlow = exp(-pow((uv.x - 0.5) * 4.0, 2.0) - pow((uv.y - 0.3) * 6.0, 2.0));
    skyColor += vec3(sunGlow * 0.03, sunGlow * 0.035, sunGlow * 0.04);

    skyColor *= 1.0 - 0.03 * length(uv - 0.5);

    fragColor = vec4(skyColor, 1.0);
}
