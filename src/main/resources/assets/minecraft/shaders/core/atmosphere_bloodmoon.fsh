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

    vec3 deepRed = vec3(0.08, 0.005, 0.005);
    vec3 darkRed = vec3(0.25, 0.015, 0.01);
    vec3 bloodRed = vec3(0.45, 0.03, 0.02);
    vec3 horizonRed = vec3(0.65, 0.08, 0.04);

    vec3 skyColor = mix(horizonRed, bloodRed, pow(gradient, 0.7));
    skyColor = mix(skyColor, darkRed, pow(gradient, 2.0));
    skyColor = mix(skyColor, deepRed, pow(gradient, 4.0));

    vec2 moonPos = vec2(0.72, 0.7);
    vec2 moonUv = (uv - moonPos) * vec2(1.0, 1.0);
    float moonDist = length(moonUv);

    float moonGlow = 0.12 / (moonDist + 0.03);
    vec3 glowColor = vec3(1.0, 0.15, 0.1);
    skyColor += glowColor * moonGlow * 0.2;

    float moonCore = smoothstep(0.06, 0.0, moonDist);
    vec3 coreColor = vec3(1.0, 0.2, 0.15);
    skyColor += coreColor * moonCore * 0.3;

    float pulse = 0.5 + 0.5 * sin(Time * 0.3 + uv.y * 2.0);
    skyColor *= 1.0 + pulse * 0.04;

    float haze = hash(floor(uv * 50.0 + Time * 0.5)) * 0.025;
    skyColor += vec3(haze * 0.5, 0.0, 0.0);

    float vignette = 1.0 - 0.3 * length(uv - 0.5);
    skyColor *= vignette;

    float bloodDrip = sin(uv.x * 15.0 + Time) * 0.5 + 0.5;
    bloodDrip *= sin(uv.y * 20.0 - Time * 0.5) * 0.5 + 0.5;
    bloodDrip *= 0.02;
    skyColor += vec3(bloodDrip * 0.5, 0.0, 0.0);

    fragColor = vec4(skyColor, 1.0);
}
