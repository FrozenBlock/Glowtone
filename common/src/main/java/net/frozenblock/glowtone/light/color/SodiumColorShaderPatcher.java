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

import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFeatures;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public final class SodiumColorShaderPatcher {
	private static final String INIT = "_vert_init();";
	private static final String OUT_COLOR = "out vec4 v_Color;";
	static final String SET_COLOR = "v_Color = _vert_color * texture(u_LightTex, _vert_tex_light_coord);";

	private static final String IN_CHROMA = """
		in vec4 a_GlowtoneChroma;
		in vec4 a_GlowtoneSkyChroma;

		""";

	private static String chromaInputs() {
		return GlowtoneVertexFeatures.shaders().pivot()
			? IN_CHROMA + "in vec4 a_GlowtonePivot;" + System.lineSeparator() + System.lineSeparator()
			: IN_CHROMA;
	}

	private static final String SPLIT = """
		    vec4 glowtone_fullLight = texture(u_LightTex, _vert_tex_light_coord);
		    vec4 glowtone_skyOnlyLight = texture(u_LightTex, vec2(0.0, _vert_tex_light_coord.y));
		    vec3 glowtone_blockLight = max(glowtone_fullLight.rgb - glowtone_skyOnlyLight.rgb, vec3(0.0));
		    float glowtone_nightVision = smoothstep(0.35, 0.7, dot(texture(u_LightTex, vec2(0.0, 0.0)).rgb, vec3(0.2126, 0.7152, 0.0722)));
		    const float GLOWTONE_CHROMA_SCALE = 2.0;
		    v_Color = _vert_color * vec4(
		        glowtone_skyOnlyLight.rgb * mix(a_GlowtoneSkyChroma.rgb, vec3(1.0), glowtone_nightVision)
		            + glowtone_blockLight * mix(a_GlowtoneChroma.rgb * GLOWTONE_CHROMA_SCALE, vec3(1.0), glowtone_nightVision),
		        glowtone_fullLight.a);
		""";

	static String patchTerrain(String source) {
		if (!source.contains(INIT)
			|| !source.contains(SET_COLOR)
			|| !source.contains(OUT_COLOR)
			|| source.contains("a_GlowtoneChroma")
		) {
			return source;
		}

		return source
			.replace(OUT_COLOR, chromaInputs() + OUT_COLOR)
			.replace(SET_COLOR, SPLIT);
	}

	private SodiumColorShaderPatcher() {}
}
