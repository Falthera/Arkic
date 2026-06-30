# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Complete GPU-driven rendering backend for Sodium
- OpenGL 4.6 capability detection with vendor identification (Intel Arc, Iris Xe, UHD, AMD, NVIDIA)
- Automatic backend selection: Advanced / Compute / Compat / Sodium fallback
- SSBO-backed `GpuBufferAllocator` with persistent mapping support
- Async `ChunkUploadManager` with worker thread pool
- `ComputeCullPass` for GPU frustum culling via compute shaders
- `IndirectDrawBatch` for MultiDrawIndirect rendering
- `ChunkManager` with dirty tracking and LRU eviction
- `SodiumInterop` for seamless integration with Sodium
- `ArkicCrashHandler` with crash counting and safe mode fallback
- `ArkicConfig` with persistent properties file
- `ArkicDiagnostics` and `ArkicBenchmark` for performance monitoring
- GLSL shaders loaded from resources (no hardcoded strings)
- GitHub Actions CI/CD: build, test, compatibility checks, automated releases
- Comprehensive AGENTS.md, CHANGELOG.md, README.md

### Rendering
- AdvancedBackend: full GPU-driven pipeline (compute culling → MDI render)
- ComputeBackend: compute shaders with fallback draw path
- CompatBackend: optimized standard OpenGL for legacy systems
- Automatic fallback to Sodium if required OpenGL features unavailable

### Compatibility
- Minecraft 1.21.11+ support
- Fabric mod loader
- Works on Intel Arc, Iris Xe, UHD, AMD RDNA2/RDNA3/GCN, NVIDIA GTX/RTX
- Respects Sodium's existing chunk building pipeline
- Non-intrusive fallback on graphics errors

### Infrastructure
- Gradle wrapper for reproducible builds
- GitHub Actions workflows for CI/CD
- Version-tagged automated releases
- Semantic versioning with automated bump logic
