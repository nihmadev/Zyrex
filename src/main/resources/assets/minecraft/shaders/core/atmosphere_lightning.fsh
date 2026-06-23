#version 150

uniform vec4 BoltColor;
uniform float Brightness;
uniform float Time;

out vec4 fragColor;

void main() {
    float pulse = 0.8 + 0.2 * sin(Time * 50.0);
    float fade = exp(-Time * 3.0);
    float alpha = clamp(pulse * fade * Brightness, 0.0, 1.0);
    fragColor = vec4(BoltColor.rgb * Brightness, alpha);
}
