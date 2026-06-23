#version 150

uniform vec2 Resolution;
uniform float Time;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(443.8975, 397.2973));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 5; i++) {
        v += a * sin(p.x * 1.2 + p.y * 0.7 + Time * 0.02);
        p = rot * p * 2.1;
        a *= 0.45;
    }
    return v * 0.5 + 0.5;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    float gradient = 1.0 - uv.y;

    vec3 abyss = vec3(0.005, 0.0, 0.01);
    vec3 darkPurple = vec3(0.02, 0.005, 0.04);
    vec3 darkBlue = vec3(0.04, 0.01, 0.08);
    vec3 horizon = vec3(0.08, 0.02, 0.12);

    vec3 skyColor = mix(horizon, darkBlue, pow(gradient, 0.8));
    skyColor = mix(skyColor, darkPurple, pow(gradient, 2.0));
    skyColor = mix(skyColor, abyss, pow(gradient, 4.0));

    vec2 starUv = uv * 200.0 + Time * 0.02;
    vec2 grid = floor(starUv);
    vec2 local = fract(starUv) - 0.5;
    float h = hash(grid);
    float star = step(0.998, h);
    float twinkle = 0.5 + 0.5 * sin(Time * (1.0 + h * 3.0) + h * 500.0);
    float size = 0.01 + h * 0.03;
    float starDist = length(local);
    float starAlpha = smoothstep(size, 0.0, starDist);
    vec3 starColor = mix(
        vec3(0.6, 0.5, 0.8),
        vec3(0.8, 0.7, 1.0),
        h
    );
    skyColor += starColor * star * starAlpha * twinkle * 1.5;

    vec2 riftUv = uv * 5.0 + vec2(Time * 0.01, Time * 0.005);
    float rift = fbm(riftUv);
    float riftMask = pow(max(0.0, 1.0 - uv.y * 2.0), 2.0);
    vec3 riftColor = mix(
        vec3(0.15, 0.0, 0.25),
        vec3(0.05, 0.0, 0.15),
        rift
    );
    skyColor += riftColor * rift * riftMask * 0.2;

    float tendril = sin(uv.x * 12.0 + Time * 0.2) * 0.5 + 0.5;
    tendril *= sin(uv.y * 8.0 - Time * 0.15) * 0.5 + 0.5;
    tendril *= pow(1.0 - uv.y, 3.0) * 0.06;
    skyColor += vec3(0.0, 0.0, tendril * 0.5);

    float vignette = 1.0 - 0.5 * length(uv - 0.5);
    skyColor *= vignette;

    float haze = hash(floor(uv * 80.0 + Time)) * 0.02;
    skyColor += vec3(0.0, 0.0, haze * 0.3);

    fragColor = vec4(skyColor, 1.0);
}
