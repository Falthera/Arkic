#version 430 core

in vec4 vColor;
in vec2 vUv;
in vec3 vNormal;

out vec4 fragColor;

void main() {
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));
    float diff = max(dot(normalize(vNormal), lightDir), 0.0) * 0.7 + 0.3;
    fragColor = vec4(vColor.rgb * diff, vColor.a);
}
