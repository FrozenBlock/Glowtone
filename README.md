# Glowtone

Allows for emissive textures to be quickly added as an overlay on both Block and Item models, as well as
changing the render layers of blocks.

## Minecraft 1.21.1 — Fabric and NeoForge

This branch targets **Minecraft 1.21.1** and ships for both **Fabric** and **NeoForge** from a shared
common source set:

- `common/` — loader agnostic code and all assets, compiled once and bundled into both loader jars.
- `fabric/` — Fabric entrypoint + Fabric Rendering API (Indigo) emissive model wrapping.
- `neoforge/` — NeoForge entrypoint + `BakedModelWrapper` / `QuadTransformers` emissive model wrapping.

Because 1.21.1 predates the per-quad `lightEmission` model system, emissive rendering is implemented against
each loader's rendering API rather than ported directly.

### Building

```
./gradlew build               # builds both fabric and neoforge jars
./gradlew :fabric:runClient   # dev client (Fabric)
./gradlew :neoforge:runClient # dev client (NeoForge)
```

Output jars: `fabric/build/libs/` and `neoforge/build/libs/`.
