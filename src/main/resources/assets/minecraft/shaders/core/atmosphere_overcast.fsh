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
        v += a * sin(p.x * 1.3 + p.y * 0.8 + Time * 0.008);
        p = rot * p * 2.2;
        a *= 0.48;
    }
    return v * 0.5 + 0.5;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    float gradient = 1.0 - uv.y;

    vec3 topColor = vec3(0.18, 0.18, 0.22);
    vec3 midColor = vec3(0.28, 0.28, 0.33);
    vec3 bottomColor = vec3(0.38, 0.38, 0.42);

    vec3 skyColor = mix(bottomColor, midColor, gradient);
    skyColor = mix(skyColor, topColor, pow(gradient, 1.5));

    vec2 cloudUv = uv * 3.0 + vec2(Time * 0.008, Time * 0.003);
    float cloud1 = fbm(cloudUv);
    float cloud2 = fbm(cloudUv * 1.8 + 20.0);
    float cloudAlpha = cloud1 * 0.6 + cloud2 * 0.4;

    vec3 cloudBright = vec3(0.32, 0.32, 0.37);
    vec3 cloudDark = vec3(0.15, 0.15, 0.19);
    vec3 cloudColor = mix(cloudDark, cloudBright, cloud1);

    skyColor = mix(skyColor, cloudColor, cloudAlpha * 0.8);

    float rainChance = hash(floor(uv * 150.0 + Time * 10.0));
    float rain = step(0.98, rainChance) * 0.08;
    skyColor -= vec3(rain * 0.05);

    float fogLayer = exp(-gradient * 0.5) * 0.2;
    skyColor += vec3(fogLayer * 0.3, fogLayer * 0.3, fogLayer * 0.35);

    skyColor *= 1.0 - 0.05 * length(uv - 0.5);

    fragColor = vec4(skyColor, 1.0);
}
