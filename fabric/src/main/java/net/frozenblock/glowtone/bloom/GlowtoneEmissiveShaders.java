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

package net.frozenblock.glowtone.bloom;

import com.mojang.blaze3d.shaders.ShaderType;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class GlowtoneEmissiveShaders {
	private static final Set<Identifier> LIT_SHADERS = Set.of(
		Identifier.withDefaultNamespace("core/terrain"),
		Identifier.withDefaultNamespace("core/block"),
		Identifier.withDefaultNamespace("core/entity"),
		Identifier.withDefaultNamespace("core/item"),
		Identifier.withDefaultNamespace("core/particle")
	);

	private static final Set<Identifier> SELF_LIT_SHADERS = Set.of(
		Identifier.withDefaultNamespace("core/position_tex"),
		Identifier.withDefaultNamespace("core/position_tex_color"),
		Identifier.withDefaultNamespace("core/stars"),
		Identifier.withDefaultNamespace("core/rendertype_beacon_beam"),
		Identifier.withDefaultNamespace("core/rendertype_lightning"),
		Identifier.withDefaultNamespace("core/rendertype_end_portal")
	);

	private static final String MAIN = "void main()";
	private static final String GLOWTONE_MAIN = "void glowtone_main()";
	private static final String SAMPLE_LIGHTMAP = "sample_lightmap(Sampler2, UV2)";
	private static final String GLOWTONE_SAMPLE_LIGHTMAP = "glowtone_sampleLightmap(Sampler2, UV2)";
	private static final String FRAG_COLOR_OUT = "out vec4 fragColor;";
	private static final String GLOWTONE_FRAG_COLOR_OUT = "layout(location = 0) out vec4 fragColor;";

	private static final String VERTEX_HEADER = """
		flat out float glowtone_Emissive;

		vec4 glowtone_sampleLightmap(sampler2D glowtone_lightMap, ivec2 glowtone_lightCoords) {
			glowtone_Emissive = float((glowtone_lightCoords.x & %d) != 0);
			return sample_lightmap(glowtone_lightMap, ivec2(glowtone_lightCoords.x & %d, glowtone_lightCoords.y & %d));
		}

		""".formatted(GlowtoneBloom.EMISSIVE_MARKER, GlowtoneBloom.LIGHT_COORDS_CHANNEL_MASK, GlowtoneBloom.LIGHT_COORDS_CHANNEL_MASK);

	private static final String VERTEX_FOOTER = """

		void main() {
		#ifdef EMISSIVE
			glowtone_Emissive = 1.0;
		#else
			glowtone_Emissive = 0.0;
		#endif
			glowtone_main();
		}
		""";

	private static final String FRAGMENT_HEADER = """
		flat in float glowtone_Emissive;
		layout(location = 1) out vec4 glowtone_EmissiveColor;

		""";

	private static final String FRAGMENT_FOOTER = """

		void main() {
			glowtone_main();
			glowtone_EmissiveColor = vec4(fragColor.rgb * glowtone_Emissive, fragColor.a);
		}
		""";

	private static final String SELF_LIT_FRAGMENT_HEADER = """
		layout(location = 1) out vec4 glowtone_EmissiveColor;

		""";

	private static final String SELF_LIT_FRAGMENT_FOOTER = """

		void main() {
			glowtone_main();
			glowtone_EmissiveColor = fragColor;
		}
		""";

	private GlowtoneEmissiveShaders() {}

	public static boolean isLitShader(Identifier id) {
		return LIT_SHADERS.contains(id);
	}

	public static String patch(Identifier id, ShaderType type, String source) {
		if (!source.contains(MAIN)) return source;

		if (LIT_SHADERS.contains(id)) {
			final String patched = type == ShaderType.VERTEX ? patchVertex(source) : patchFragment(source);
			return patched;
		}
		if (SELF_LIT_SHADERS.contains(id) && type == ShaderType.FRAGMENT) {
			return patchSelfLitFragment(source);
		}
		return source;
	}

	private static String patchVertex(String source) {
		if (!source.contains(SAMPLE_LIGHTMAP)) return source;
		return source.replace(SAMPLE_LIGHTMAP, GLOWTONE_SAMPLE_LIGHTMAP).replace(MAIN, VERTEX_HEADER + GLOWTONE_MAIN) + VERTEX_FOOTER;
	}

	private static String patchFragment(String source) {
		if (!source.contains(FRAG_COLOR_OUT)) return source;
		return source.replace(FRAG_COLOR_OUT, GLOWTONE_FRAG_COLOR_OUT).replace(MAIN, FRAGMENT_HEADER + GLOWTONE_MAIN) + FRAGMENT_FOOTER;
	}

	private static String patchSelfLitFragment(String source) {
		if (!source.contains(FRAG_COLOR_OUT)) return source;
		return source.replace(FRAG_COLOR_OUT, GLOWTONE_FRAG_COLOR_OUT).replace(MAIN, SELF_LIT_FRAGMENT_HEADER + GLOWTONE_MAIN)
			+ SELF_LIT_FRAGMENT_FOOTER;
	}
}
