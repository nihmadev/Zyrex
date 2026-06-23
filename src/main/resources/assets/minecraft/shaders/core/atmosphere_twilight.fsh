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

    vec3 deepPurple = vec3(0.05, 0.0, 0.12);
    vec3 midPurple = vec3(0.35, 0.1, 0.35);
    vec3 twilightPink = vec3(0.6, 0.25, 0.45);
    vec3 horizonOrange = vec3(0.85, 0.45, 0.2);

    vec3 skyColor = mix(horizonOrange, twilightPink, pow(gradient, 0.6));
    skyColor = mix(skyColor, midPurple, pow(gradient, 1.5));
    skyColor = mix(skyColor, deepPurple, pow(gradient, 3.0));

    float stars = hash(floor(uv * 150.0 + Time * 0.05));
    float starAlpha = step(0.995, stars);
    float twinkle = 0.5 + 0.5 * sin(Time * 1.5 + stars * 300.0);
    vec3 starColor = vec3(0.8, 0.7, 1.0);
    skyColor += starColor * starAlpha * twinkle * 0.6 * (1.0 - uv.y * 0.5);

    vec2 auroraUv = uv * vec2(3.0, 1.5) + vec2(Time * 0.02, Time * 0.005);
    float aurora1 = sin(auroraUv.x * 2.0 + auroraUv.y * 1.5 + Time * 0.1) * 0.5 + 0.5;
    float aurora2 = sin(auroraUv.x * 3.5 - auroraUv.y * 2.0 + Time * 0.08) * 0.5 + 0.5;
    float aurora3 = sin(auroraUv.x * 5.0 + auroraUv.y * 3.0 + Time * 0.05) * 0.5 + 0.5;
    float aurora = (aurora1 * 0.4 + aurora2 * 0.35 + aurora3 * 0.25);
    float auroraMask = max(0.0, 1.0 - uv.y * 2.5) * aurora;
    vec3 auroraColor = mix(
        vec3(0.2, 0.5, 0.8),
        vec3(0.1, 0.8, 0.6),
        aurora1
    );
    skyColor += auroraColor * auroraMask * 0.2;

    float horizonGlow = exp(-gradient * 4.0) * 0.2;
    skyColor += vec3(horizonGlow * 0.8, horizonGlow * 0.3, horizonGlow * 0.1);

    skyColor *= 1.0 - 0.08 * length(uv - 0.5);

    fragColor = vec4(skyColor, 1.0);
}
