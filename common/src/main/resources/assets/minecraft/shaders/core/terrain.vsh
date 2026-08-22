#version 330

// Glowtone — override of vanilla 26.2 core/terrain.vsh.
// Coloured lighting is BAKED into the chunk mesh: each vertex carries an RGB chroma (GlowtoneChroma) produced by
// the coloured-light engine at mesh time. Here we recolour ONLY the block-light half of the lightmap: the
// sky-light contribution is sampled separately and left untouched, so daylight stays neutral while block light
// takes the emitter's colour. Costs nothing per frame and works at full render distance.

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec4 GlowtoneChroma;    // colour of the BLOCK light reaching this vertex, stored at 1/GLOWTONE_CHROMA_SCALE
in vec4 GlowtoneSkyChroma; // colour DAYLIGHT has picked up on its way down, white where nothing tints it

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

    // Split the lightmap: sky-only (block = 0) is the neutral base, and the block-light contribution is
    // whatever the full sample adds on top of it — only THAT is tinted. This keeps daylight neutral and keeps
    // colour saturated even at full block light, where vanilla's ramp is nearly white.
    vec4 fullLight = sample_lightmap(Sampler2, UV2);
    vec4 skyOnlyLight = sample_lightmap(Sampler2, ivec2(0, UV2.y));
    vec3 blockLight = max(fullLight.rgb - skyOnlyLight.rgb, vec3(0.0));
    // GlowtoneChroma stores the light multiplier divided by GLOWTONE_CHROMA_SCALE, because a vertex attribute byte can
    // only hold 0..1. Multiplying it back gives the tint room to exceed one, which is what lets a deep hue —
    // blue above all — reach full brightness by AMPLIFYING its own channel instead of being mixed with white.
    // Neutral is therefore 0.5, not 1.0. Must match GlowtoneChromaBlend.CHROMA_SCALE.
    const float GLOWTONE_CHROMA_SCALE = 2.0;
    // Sky light is tinted by whatever it passed through on the way down (a stained-glass roof), and block light
    // by the colour of the source. GlowtoneSkyChroma only ever ATTENUATES — it is plain white where nothing overhead
    // colours the daylight — so open sky stays exactly as vanilla renders it.
    vertexColor = Color * vec4(
        skyOnlyLight.rgb * GlowtoneSkyChroma.rgb + blockLight * GlowtoneChroma.rgb * GLOWTONE_CHROMA_SCALE,
        fullLight.a
    );

    texCoord0 = UV0;
}
