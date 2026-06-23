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
        v += a * sin(p.x * 1.5 + p.y * 0.8 + Time * 0.05);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v * 0.5 + 0.5;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    float gradient = 1.0 - uv.y;

    vec3 topColor = vec3(0.15, 0.0, 0.0);
    vec3 midColor = vec3(0.6, 0.05, 0.0);
    vec3 bottomColor = vec3(0.9, 0.15, 0.02);

    vec3 skyColor = mix(topColor, midColor, 1.0 - pow(gradient, 2.0));
    skyColor = mix(skyColor, bottomColor, pow(1.0 - gradient, 3.0));

    vec2 fireUv = uv * 3.0 + vec2(Time * 0.03, 0.0);
    float fire = fbm(fireUv);
    float fireAlpha = pow(max(0.0, 1.0 - uv.y * 1.5), 3.0);
    vec3 fireColor = vec3(1.0, 0.4, 0.05);
    skyColor += fireColor * fire * fireAlpha * 0.3;

    vec2 emberUv = uv * 40.0 + vec2(Time * 2.0, Time * 1.5);
    vec2 grid = floor(emberUv);
    vec2 local = fract(emberUv) - 0.5;
    float h = hash(grid);
    float ember = step(0.98, h) * exp(-length(local) * 10.0);
    ember *= 0.5 + 0.5 * sin(Time * 3.0 + h * 100.0);
    skyColor += vec3(ember * 0.6, ember * 0.15, 0.0);

    float glow = exp(-gradient * 2.0) * 0.15;
    skyColor += vec3(glow * 0.8, glow * 0.2, 0.0);

    float haze = hash(floor(uv * 60.0)) * 0.04;
    skyColor += vec3(haze * 0.6, 0.0, 0.0);

    skyColor *= 1.0 - 0.1 * length(uv - 0.5);

    fragColor = vec4(skyColor, 1.0);
}
