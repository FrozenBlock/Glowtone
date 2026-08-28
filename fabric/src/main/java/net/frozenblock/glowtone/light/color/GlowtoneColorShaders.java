/*
 * Copyright 2026 FrozenBlock
 * This file is part of Glowtone.
 *
 * This program is free software; you can modify it under
 * the terms of version 1 of the FrozenBlock Modding Oasis License
 * as published by FrozenBlock Modding Oasis.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * FrozenBlock Modding Oasis License for more details.
 *
 * You should have received a copy of the FrozenBlock Modding Oasis License
 * along with this program; if not, see <https://github.com/FrozenBlock/Licenses>.
 */

package net.frozenblock.glowtone.light.color;

import com.mojang.blaze3d.shaders.ShaderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class GlowtoneColorShaders {
	private static final Identifier TERRAIN_ID = Identifier.withDefaultNamespace("core/terrain");
	private static final String MAIN = "void main()";
	private static final String UNIFORM = "uniform ";
	private static final String SET_VERTEX_COLOR = "vertexColor = Color *";
	private static final String SAMPLE_LIGHTMAP = "sample_lightmap(Sampler2, UV2)";

	private static final String IN_GLOWTONE_CHROMA = """
		in vec4 GlowtoneChroma;    // colour of the BLOCK light reaching this vertex, stored at 1/GLOWTONE_CHROMA_SCALE
		in vec4 GlowtoneSkyChroma; // colour DAYLIGHT has picked up on its way down, white where nothing tints it

		""";

	// Split the lightmap: sky-only (block = 0) is the neutral base, and the block-light contribution is
	// whatever the full sample adds on top of it — only THAT is tinted. This keeps daylight neutral and keeps
	// colour saturated even at full block light, where vanilla's ramp is nearly white.
	private static final String SPLIT_LIGHTMAP = """
		    vec4 fullLight = sample_lightmap(Sampler2, UV2);
		    vec4 skyOnlyLight = sample_lightmap(Sampler2, ivec2(0, UV2.y));
		    vec3 blockLight = max(fullLight.rgb - skyOnlyLight.rgb, vec3(0.0));

		""";

	// GlowtoneChroma stores the light multiplier divided by GLOWTONE_CHROMA_SCALE, because a vertex attribute byte can
	// only hold 0..1. Multiplying it back gives the tint room to exceed one, which is what lets a deep hue —
	// blue above all — reach full brightness by AMPLIFYING its own channel instead of being mixed with white.
	// Neutral is therefore 0.5, not 1.0. Must match GlowtoneChromaBlend.CHROMA_SCALE.
	private static final String DEFINE_GLOWTONE_CHROMA_SCALE = """
			const float GLOWTONE_CHROMA_SCALE = 2.0;

		""";

	// Sky light is tinted by whatever it passed through on the way down (a stained-glass roof), and block light
	// by the colour of the source. GlowtoneSkyChroma only ever ATTENUATES — it is plain white where nothing overhead
	// colours the daylight — so open sky stays exactly as vanilla renders it.
	private static final String SAMPLE_LIGHTMAP_REPLACEMENT = "vec4(skyOnlyLight.rgb * GlowtoneSkyChroma.rgb + blockLight * GlowtoneChroma.rgb * GLOWTONE_CHROMA_SCALE, fullLight.a)";

	private GlowtoneColorShaders() {}

	private static final String SODIUM_INIT = "_vert_init();";
	private static final String SODIUM_OUT_COLOR = "out vec4 v_Color;";
	private static final String SODIUM_SET_COLOR = "v_Color = _vert_color * texture(u_LightTex, _vert_tex_light_coord);";

	private static final String SODIUM_IN_CHROMA = """
		in vec4 a_GlowtoneChroma;
		in vec4 a_GlowtoneSkyChroma;

		""";

	private static final String SODIUM_SPLIT = """
		    vec4 glowtone_fullLight = texture(u_LightTex, _vert_tex_light_coord);
		    vec4 glowtone_skyOnlyLight = texture(u_LightTex, vec2(0.0, _vert_tex_light_coord.y));
		    vec3 glowtone_blockLight = max(glowtone_fullLight.rgb - glowtone_skyOnlyLight.rgb, vec3(0.0));
		    const float GLOWTONE_CHROMA_SCALE = 2.0;
		    v_Color = _vert_color * vec4(
		        glowtone_skyOnlyLight.rgb * a_GlowtoneSkyChroma.rgb
		            + glowtone_blockLight * a_GlowtoneChroma.rgb * GLOWTONE_CHROMA_SCALE,
		        glowtone_fullLight.a);""";

	private static String patchSodiumTerrain(String source) {
		if (!source.contains(SODIUM_INIT) || !source.contains(SODIUM_SET_COLOR)
			|| !source.contains(SODIUM_OUT_COLOR) || source.contains("a_GlowtoneChroma")) {
			return source;
		}

		return source
			.replace(SODIUM_OUT_COLOR, SODIUM_IN_CHROMA + SODIUM_OUT_COLOR)
			.replace(SODIUM_SET_COLOR, SODIUM_SPLIT);
	}

	public static String patchTerrainShader(Identifier id, ShaderType type, String source) {
		if (type != ShaderType.VERTEX || !source.contains(MAIN)) return source;
		if (source.contains(SODIUM_SET_COLOR)) return patchSodiumTerrain(source);
		if (!id.equals(TERRAIN_ID) || !source.contains(UNIFORM) || !source.contains(SET_VERTEX_COLOR)) return source;

		final String preTerrainSource = source.substring(0, source.lastIndexOf("#version"));
		String terrainOnlySource = source.substring(source.lastIndexOf("#version"));

		// Glowtone — override of vanilla 26.2 core/terrain.vsh.
		// Coloured lighting is BAKED into the chunk mesh: each vertex carries an RGB chroma (GlowtoneChroma) produced by
		// the coloured-light engine at mesh time. Here we recolour ONLY the block-light half of the lightmap: the
		// sky-light contribution is sampled separately and left untouched, so daylight stays neutral while block light
		// takes the emitter's colour. Costs nothing per frame and works at full render distance.

		injectUniforms: {
			String preUniform = terrainOnlySource.substring(0, terrainOnlySource.indexOf(UNIFORM));
			preUniform = preUniform + IN_GLOWTONE_CHROMA;
			final String postUniform = terrainOnlySource.substring(terrainOnlySource.indexOf(UNIFORM));
			terrainOnlySource = preUniform + postUniform;
		}

		patchLightmap:{
			String preColor = terrainOnlySource.substring(0, terrainOnlySource.indexOf(SET_VERTEX_COLOR));
			preColor = preColor + SPLIT_LIGHTMAP + DEFINE_GLOWTONE_CHROMA_SCALE;

			String postColor = terrainOnlySource.substring(terrainOnlySource.indexOf(SET_VERTEX_COLOR));
			patchLightmapSampler:
			{
				String postColorOnly = postColor.substring(0, postColor.indexOf(";"));
				postColorOnly = postColorOnly.replace(SAMPLE_LIGHTMAP, SAMPLE_LIGHTMAP_REPLACEMENT);
				String postPostColor = postColor.substring(postColor.indexOf(";"));
				postColor = postColorOnly + postPostColor;
			}

			terrainOnlySource = preColor + postColor;
		}

		return preTerrainSource + terrainOnlySource;
	}
}
