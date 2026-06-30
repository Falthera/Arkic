#version 430 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in vec2 UV1;
layout(location = 4) in vec3 Normal;

layout(std430, binding = 3) buffer DrawBlock {
    uint visibleCount;
    uint visibleOffsets[];
} draw;

out vec4 vColor;
out vec2 vUv;
out vec3 vNormal;
out vec3 vWorldPos;

uniform mat4 ProjectionMatrix;
uniform mat4 ModelViewMatrix;

void main() {
    vColor = Color;
    vUv = UV0;
    vNormal = Normal;
    vWorldPos = Position;
    gl_Position = ProjectionMatrix * ModelViewMatrix * vec4(Position, 1.0);
}
