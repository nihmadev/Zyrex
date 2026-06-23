#version 150

uniform vec4 SkyTopColor;
uniform vec4 SkyBottomColor;
uniform vec4 FogColor;
uniform vec2 Resolution;
uniform float Time;
uniform float LightningIntensity;
uniform float StarBrightness;
uniform float CloudCoverage;
uniform vec4 Preset1;
uniform vec4 Preset2;

out vec4 fragColor;

vec3 palette(float t) {
    vec3 a = vec3(0.5, 0.5, 0.5);
    vec3 b = vec3(0.5, 0.5, 0.5);
    vec3 c = vec3(1.0, 1.0, 1.0);
    vec3 d = vec3(0.263, 0.416, 0.557);
    return a + b * cos(6.28318 * (c * t + d));
}

float hash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float starField(vec2 uv, float brightness) {
    vec2 p = uv * 120.0;
    vec2 grid = floor(p);
    vec2 local = fract(p) - 0.5;
    float h = hash21(grid);
    float twinkle = sin(Time * (1.5 + h * 4.0) + h * 628.3) * 0.5 + 0.5;
    float size = 0.015 + h * 0.035;
    float d = length(local);
    float alpha = smoothstep(0.005, size, d);
    float threshold = 0.985 - brightness * 0.014;
    float star = step(threshold, h);
    float colorIdx = floor(h * 5.0) / 5.0;
    vec3 starColor = palette(colorIdx + 0.5);
    return alpha * star * twinkle * brightness * 0.8;
}

float lightningFlash(vec2 uv, float time) {
    float flash = 0.0;
    for (int i = 0; i < 6; i++) {
        vec2 pos = vec2(
            hash21(vec2(float(i) * 7.137, floor(time * 0.08))),
            hash21(vec2(float(i) * 13.291, floor(time * 0.08)))
        );
        float delay = hash21(vec2(float(i) * 3.791, 0.0));
        float ft = fract(time * 0.12 + delay);
        float dur = 0.03 + hash21(vec2(float(i) * 5.423, 0.0)) * 0.06;
        float bright = smoothstep(dur, 0.0, ft) * LightningIntensity;
        vec2 d = uv - pos;
        d.x *= Resolution.x / Resolution.y;
        float dist = length(d * vec2(1.0, 3.0));
        flash += bright * exp(-dist * 25.0);
    }
    return clamp(flash, 0.0, 1.0);
}

float cloudLayer(vec2 uv, float time, float scale, float speed) {
    vec2 p = uv * scale + time * speed;
    float c = 0.0;
    c += sin(p.x * 1.7 + p.y * 0.9 + time * 0.012) * 0.5;
    c += sin(p.x * 3.2 - p.y * 2.1 + time * 0.009) * 0.35;
    c += sin(p.x * 5.0 + p.y * 3.8 + time * 0.006) * 0.2;
    c += sin(p.x * 7.3 - p.y * 5.2) * 0.15;
    c += sin((p.x + p.y) * 2.4 + time * 0.015) * 0.25;
    return c * 0.5 + 0.5;
}

float horizonGlow(vec2 uv) {
    float h = abs(uv.y - 0.5) * 2.0;
    return exp(-h * h * 8.0) * 0.3;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    float gradient = 1.0 - uv.y;

    vec3 skyColor = mix(SkyBottomColor.rgb, SkyTopColor.rgb, pow(gradient, 0.8));

    float horizon = horizonGlow(uv);
    vec3 horizonColor = mix(vec3(1.0, 0.7, 0.4), vec3(0.8, 0.5, 0.6), gradient);
    skyColor += horizonColor * horizon;

    vec3 starCol = vec3(0.0);
    float starTotal = 0.0;
    for (int i = 0; i < 3; i++) {
        float scale = 1.0 + float(i) * 0.5;
        float s = starField(uv * scale, StarBrightness);
        starCol += vec3(s * 0.7, s * 0.8, s);
        starTotal += s;
    }
    skyColor += starCol * (1.0 - CloudCoverage * 0.9) * 0.6;

    float flash = lightningFlash(uv, Time);
    skyColor += vec3(flash * 0.9, flash * 0.95, flash);

    float cloud1 = cloudLayer(uv, Time, 2.5, 0.008);
    float cloud2 = cloudLayer(uv * 1.5 + 30.0, Time, 4.0, 0.005);
    float cloudAlpha = (cloud1 * 0.6 + cloud2 * 0.4) * CloudCoverage;

    vec3 cloudColor = mix(
        vec3(0.45, 0.45, 0.5),
        vec3(0.55, 0.55, 0.6),
        cloud1
    );
    skyColor = mix(skyColor, cloudColor, cloudAlpha * 0.6);

    float fogFactor = 0.12 * (1.0 - gradient * 0.4);
    skyColor = mix(skyColor, FogColor.rgb, fogFactor);

    float vignette = 1.0 - 0.08 * length(uv - 0.5) * 2.0;
    skyColor *= vignette;

    skyColor += Preset1.rgb * 0.05 * (1.0 - gradient);

    fragColor = vec4(skyColor, 1.0);
}
