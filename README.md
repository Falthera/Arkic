# Arkic — GPU-driven rendering backend for Sodium
# Minecraft 1.21.11+ | Fabric | OpenGL 4.6 | Compute Shaders | SSBO | MultiDrawIndirect

## What is Arkic?
Arkic replaces Sodium's chunk rendering backend with a GPU-driven pipeline:
- CPU: minimal chunk submission + async uploads
- GPU: visibility, culling, indirect draw generation, rendering
- Automatic fallback to Sodium if OpenGL 4.6 / compute shaders unavailable

## Building
```bash
./gradlew build
```

## Backends
1. `advanced` — GPU-driven culling + MultiDrawIndirect + compute shaders
2. `compute` — Compute shaders with standard draw calls
3. `compat` — Optimized standard OpenGL rendering
4. `sodium` — Fallback to Sodium's renderer

## License
MIT
