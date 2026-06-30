# AGENTS.md

# Arkic Build & Lint
- Build: `.\gradlew build`
- Lint/format: No explicit formatter configured; validate with compiler and checks.
- Do not commit without explicit user request.

# Fragile Points
- Minecraft Yarn mappings change between versions. Verify all mixin targets and imports for each supported version.
- OpenGL capabilities vary widely across Intel Arc, Iris Xe, UHD, AMD GCN/RDNA, NVIDIA GTX/RTX. Always query GL at runtime; never assume extension presence.
- Sodium internals are unstable. Avoid invasive mixins into Sodium internals where Arkic can instead replace the chunk submission path through the Sodium renderer API.
- Do not run network calls to Mojang or mod download endpoints during game runtime.

# Refactor Guidance
- Keep backend selection logic isolated in `dev.arkic.renderer.backend`.
- Keep OpenGL tracing in `dev.arkic.renderer.gl`.
- Keep GLSL strings out of Java source; load from resources.

# In This Repo
Mod entry: `dev.arkic.ArkicClient`
Renderer core: `dev.arkic.renderer.ArkicRenderer`
Buffer allocator: `dev.arkic.renderer.buffer.GpuBufferAllocator`
Compute culling: `dev.arkic.renderer.culling.ComputeCullPass`
Indirect draw: `dev.arkic.renderer.render.IndirectDrawBatch`
Backends: `dev.arkic.renderer.backend.*`
