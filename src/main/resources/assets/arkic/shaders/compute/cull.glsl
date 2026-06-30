#version 430 core

layout(local_size_x = 64) in;

layout(std430, binding = 0) buffer ChunkMetaBlock {
    vec4  offsetAabbMin;
    vec4  aabbMin;
    vec4  aabbMax;
    float center;
    float translucent;
    float render;
    float discard;
} chunks[];

layout(std430, binding = 1) buffer IndirectBlock {
    uint drawCounts[];
} indirect;

layout(std430, binding = 2) uniform CameraBlock {
    mat4 projectionMatrix;
    mat4 viewMatrix;
    vec4 cameraPos;
    vec4 viewFrustum[6];
} uboCamera;

uniform uint totalChunks;
uniform uint frameIndex;

bool intersectAABB(vec3 min, vec3 max, vec4 frustum[6]) {
    for (int i = 0; i < 6; i++) {
        vec3 n = vec3(frustum[i].x, frustum[i].y, frustum[i].z);
        float d = frustum[i].w;
        vec3 p = dot(n, min) > d ? max : min;
        if (dot(n, p) < d) return false;
    }
    return true;
}

void main() {
    uint id = gl_GlobalInvocationID.x;
    if (id >= totalChunks) return;

    chunks[id].render = 0u;
    bool vis = intersectAABB(chunks[id].aabbMin.xyz, chunks[id].aabbMax.xyz, uboCamera.viewFrustum);
    if (!vis) return;

    chunks[id].render = 1u;
}
