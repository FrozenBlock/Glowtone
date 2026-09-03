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
import com.mojang.logging.LogUtils;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;
import net.frozenblock.glowtone.config.GlowtoneShaderDump;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import org.slf4j.Logger;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public final class EmissiveShaderPatcher {
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
	private static final Identifier TERRAIN = Identifier.withDefaultNamespace("core/terrain");
	public static final Identifier TERRAIN_SHADER = TERRAIN;
	public static final String OPAQUE_TERRAIN_DEFINE = "GLOWTONE_OPAQUE_TERRAIN";
	public static final String SHADED_TERRAIN_DEFINE = "GLOWTONE_SHADED_TERRAIN";
	public static final String TRANSLUCENT_TERRAIN_DEFINE = "GLOWTONE_TRANSLUCENT_TERRAIN";

	private static final float EDGE_ANCHOR = 0.95F;
	private static final float EDGE_FLATTEN = 0.85F;
	private static final float EDGE_WHITEN = 0.70F;
	private static final float AO_RADIUS_UNITS = GlowtoneContactRects.RADIUS_UNITS;

	private static final Logger MATERIAL_LOGGER = LogUtils.getLogger();

	private static boolean debugView() {
		return aoDebug() || edgeDebugColour();
	}

	private static boolean aoDebug() {
		return GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.AMBIENT_OCCLUSION);
	}

	public static boolean toggleAoDebug() {
		return GlowtoneDebugEntries.toggle(GlowtoneDebugEntries.AMBIENT_OCCLUSION);
	}

	private static boolean edgeDebugColour() {
		return GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.EDGE_HIGHLIGHT);
	}

	public static boolean toggleEdgeDebugColour() {
		return GlowtoneDebugEntries.toggle(GlowtoneDebugEntries.EDGE_HIGHLIGHT);
	}

	private static final String EDGE_DATA_HEADER = """
		in float glowtone_Height;
		in vec4 glowtone_EdgeDist;
		in vec4 glowtone_EdgeMask;
		in vec4 glowtone_Shade;
		in float glowtone_ViewDist;
		flat in vec4 glowtone_Contact0;
		flat in vec4 glowtone_Contact1;
		flat in vec4 glowtone_Contact2;
		flat in vec4 glowtone_Contact3;

		float glowtone_keepVaryings() {
			return 1e-20 * (glowtone_Height + glowtone_ViewDist
				+ dot(glowtone_EdgeDist, vec4(1.0)) + dot(glowtone_EdgeMask, vec4(1.0))
				+ dot(glowtone_Contact0, vec4(1.0)) + dot(glowtone_Contact1, vec4(1.0))
				+ dot(glowtone_Contact2, vec4(1.0)) + dot(glowtone_Contact3, vec4(1.0))
				+ dot(glowtone_Shade, vec4(1.0)));
		}

		uint glowtone_contactBits(vec4 glowtone_packed) {
			return (uint(glowtone_packed.r * 255.0 + 0.5) << 24)
				| (uint(glowtone_packed.g * 255.0 + 0.5) << 16)
				| (uint(glowtone_packed.b * 255.0 + 0.5) << 8)
				| uint(glowtone_packed.a * 255.0 + 0.5);
		}

		vec4 glowtone_edgeReach(float glowtone_width) {
			vec4 glowtone_units = glowtone_EdgeDist * 255.0;
			vec4 glowtone_slope = max(fwidth(glowtone_units), vec4(0.0001));
			return clamp(
				(vec4(glowtone_width) + 0.5 * glowtone_slope - glowtone_units) / glowtone_slope,
				vec4(0.0), vec4(1.0)
			);
		}

		""";

	private static final String AO_HEADER = """
		uint glowtone_field(uint glowtone_w[4], uint glowtone_bit, uint glowtone_width) {
			uint glowtone_word = glowtone_bit >> 5u;
			uint glowtone_offset = glowtone_bit & 31u;
			uint glowtone_raw = glowtone_w[glowtone_word] >> glowtone_offset;
			if (glowtone_offset + glowtone_width > 32u) {
				glowtone_raw |= glowtone_w[glowtone_word + 1u] << (32u - glowtone_offset);
			}
			return glowtone_raw & ((1u << glowtone_width) - 1u);
		}

		float glowtone_kernelBelow(float glowtone_t) {
			float glowtone_reach = min(abs(glowtone_t) / %s, 1.0);
			float glowtone_half = 0.5 * glowtone_reach * (2.0 - glowtone_reach);
			return glowtone_t < 0.0 ? 0.5 - glowtone_half : 0.5 + glowtone_half;
		}

		float glowtone_fromRects(uint glowtone_w[4], vec2 glowtone_here) {
			float glowtone_nearest = 0.0;

			for (uint glowtone_i = 0u; glowtone_i < 4u; glowtone_i++) {
				uint glowtone_at = glowtone_i * 24u;
				float glowtone_u0 = float(glowtone_field(glowtone_w, glowtone_at, 6u)) - 16.0;
				float glowtone_u1 = float(glowtone_field(glowtone_w, glowtone_at + 6u, 6u)) - 16.0;
				if (glowtone_u1 < glowtone_u0) continue;

				float glowtone_v0 = float(glowtone_field(glowtone_w, glowtone_at + 12u, 6u)) - 16.0;
				float glowtone_v1 = float(glowtone_field(glowtone_w, glowtone_at + 18u, 6u)) - 16.0;

				float glowtone_du = max(max(glowtone_u0 - glowtone_here.x, glowtone_here.x - glowtone_u1), 0.0);
				float glowtone_dv = max(max(glowtone_v0 - glowtone_here.y, glowtone_here.y - glowtone_v1), 0.0);

				glowtone_nearest = max(glowtone_nearest,
					2.0 * (1.0 - glowtone_kernelBelow(length(vec2(glowtone_du, glowtone_dv)))));
			}

			return glowtone_nearest;
		}

		float glowtone_node(uint glowtone_w[4], int glowtone_i, int glowtone_j) {
			return float(glowtone_field(glowtone_w, uint(glowtone_i * 5 + glowtone_j) * 5u, 5u)) / 31.0;
		}

		float glowtone_fromGrid(uint glowtone_w[4], vec2 glowtone_where) {
			vec2 glowtone_t = clamp(glowtone_where, 0.0, 1.0) * 4.0;
			vec2 glowtone_base = min(floor(glowtone_t), 3.0);
			vec2 glowtone_f = glowtone_t - glowtone_base;
			int glowtone_i = int(glowtone_base.x);
			int glowtone_j = int(glowtone_base.y);

			return mix(
				mix(glowtone_node(glowtone_w, glowtone_i, glowtone_j),
					glowtone_node(glowtone_w, glowtone_i, glowtone_j + 1), glowtone_f.y),
				mix(glowtone_node(glowtone_w, glowtone_i + 1, glowtone_j),
					glowtone_node(glowtone_w, glowtone_i + 1, glowtone_j + 1), glowtone_f.y),
				glowtone_f.x);
		}

		float glowtone_ambientOcclusion() {
			uint glowtone_last = glowtone_contactBits(glowtone_Contact3);
			if ((glowtone_last & %s) == 0u) return 0.0;

			uint glowtone_w[4] = uint[4](
				glowtone_contactBits(glowtone_Contact0),
				glowtone_contactBits(glowtone_Contact1),
				glowtone_contactBits(glowtone_Contact2),
				glowtone_last
			);

			vec4 glowtone_span = glowtone_EdgeDist * 255.0;

			if ((glowtone_w[3] & %s) != 0u) {
				float glowtone_wU = max(glowtone_span.r + glowtone_span.g, 1.0);
				float glowtone_wV = max(glowtone_span.b + glowtone_span.a, 1.0);
				return glowtone_fromGrid(glowtone_w,
					vec2(glowtone_span.r / glowtone_wU, glowtone_span.b / glowtone_wV));
			}

			return glowtone_fromRects(glowtone_w, vec2(glowtone_span.r, glowtone_span.b));
		}

		""";

	private static final String EDGE_HEADER = """
		float glowtone_liquidFacing() {
			vec4 glowtone_reach = glowtone_edgeReach(%s);
		%s
			vec4 glowtone_facing = glowtone_reach * step(vec4(0.5), glowtone_EdgeMask);
			return max(max(glowtone_facing.r, glowtone_facing.g), max(glowtone_facing.b, glowtone_facing.a));
		}

		vec4 glowtone_edgeLit() {
			vec4 glowtone_reach = glowtone_edgeReach(%s);
		%s
			return glowtone_reach * step(vec4(%s), glowtone_EdgeMask);
		}

		float glowtone_edgeRim(vec4 glowtone_lit) {
		%s
		}

		float glowtone_fade(float glowtone_max) {
			return 1.0 - smoothstep(glowtone_max * 0.75, glowtone_max, %s);
		}

		vec3 glowtone_edgeHighlight(vec3 glowtone_colour, float glowtone_strength) {
			vec4 glowtone_lit = glowtone_edgeLit();
			float glowtone_rim = glowtone_edgeRim(glowtone_lit);
		%s
		}

		vec4 glowtone_liquidHighlight(vec4 glowtone_colour, float glowtone_strength) {
			float glowtone_rim = min(1.0, glowtone_strength * glowtone_liquidFacing() * glowtone_fade(%s));
			float glowtone_level = dot(glowtone_colour.rgb, vec3(0.2126, 0.7152, 0.0722));
			vec3 glowtone_foam =
				mix(glowtone_colour.rgb, vec3(max(glowtone_level, %s)), %s) * %s;

			return vec4(
				mix(glowtone_colour.rgb, glowtone_foam, glowtone_rim),
				mix(glowtone_colour.a, max(glowtone_colour.a, %s), glowtone_rim)
			);
		}

		""";

	private static final String EDGE_RETURN_NORMAL = """
			const vec3 glowtone_luma = vec3(0.2126, 0.7152, 0.0722);
			float glowtone_level = dot(glowtone_colour, glowtone_luma);
			float glowtone_peak = max(max(vertexColor.r, vertexColor.g), vertexColor.b);
			float glowtone_light = min(1.0, glowtone_peak);
			vec3 glowtone_tint = vertexColor.rgb / max(glowtone_peak, 1e-4);

			float glowtone_anchor = max(glowtone_level, %s * glowtone_light);
			float glowtone_target = mix(glowtone_level, glowtone_anchor, %s);
			vec3 glowtone_scaled = glowtone_colour * (glowtone_target / max(glowtone_level, 1e-4));
			vec3 glowtone_paled = mix(glowtone_scaled, glowtone_tint * glowtone_target, %s);
			vec3 glowtone_band = glowtone_paled
				/ max(max(max(glowtone_paled.r, glowtone_paled.g), glowtone_paled.b), 1.0);

			return mix(glowtone_colour, glowtone_band, glowtone_rim * glowtone_strength);""";

	private static final String EDGE_RETURN_DEBUG = """
			vec3 glowtone_dbg = glowtone_lit.r * vec3(1.0, 0.0, 0.0)
				+ glowtone_lit.g * vec3(0.0, 1.0, 0.0)
				+ glowtone_lit.b * vec3(0.0, 0.4, 1.0)
				+ glowtone_lit.a * vec3(1.0, 1.0, 0.0);
			return mix(glowtone_colour, clamp(glowtone_dbg, 0.0, 1.0), glowtone_rim);""";

	private static final String EMISSIVE_WRITE =
		"glowtone_EmissiveColor = vec4(fragColor.rgb * glowtone_Emissive, fragColor.a);";

	private static final String APPLY_FOG = "fragColor = apply_fog(";

	private static final String PRE_FOG_CAPTURE = "	vec3 glowtone_preFog = fragColor.rgb;";

	private static final String FOGGED_EMISSIVE_WRITE =
		"glowtone_EmissiveColor = vec4(glowtone_preFog * glowtone_Emissive"
			+ " * (1.0 - glowtone_FogAmount) * ChunkVisibility, fragColor.a);";

	private static final String CHUNK_FADE =
		"    color = mix(FogColor * vec4(1, 1, 1, color.a), color, ChunkVisibility);";

	private static final String CHUNK_FADE_ALPHA =
		"    color.a *= mix(FogColor.a, 1.0, ChunkVisibility);";
	private static final String DEFER_FOG = "fragColor = glowtone_deferFog(";

	private static final String FOG_HEADER = """
		float glowtone_FogAmount = 0.0;
		vec3 glowtone_FogTint = vec3(0.0);

		vec4 glowtone_deferFog(
			vec4 glowtone_colour, float glowtone_spherical, float glowtone_cylindrical,
			float glowtone_environmentalStart, float glowtone_environmentalEnd,
			float glowtone_distanceStart, float glowtone_distanceEnd, vec4 glowtone_fog
		) {
			glowtone_FogAmount = total_fog_value(
				glowtone_spherical, glowtone_cylindrical,
				glowtone_environmentalStart, glowtone_environmentalEnd,
				glowtone_distanceStart, glowtone_distanceEnd) * glowtone_fog.a;
			glowtone_FogTint = glowtone_fog.rgb;
			return glowtone_colour;
		}

		""";

	private static final String FOG_APPLY =
		"	fragColor.rgb = mix(glowtone_FogTint, fragColor.rgb, ChunkVisibility);"
			+ System.lineSeparator()
			+ "	fragColor.rgb = mix(fragColor.rgb, glowtone_FogTint, glowtone_FogAmount);";

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
			glowtone_Material = (glowtone_lightCoords.y >> 8) & %d;
			glowtone_Gui = (glowtone_lightCoords.x & %d) != 0 ? 1 : 0;
			glowtone_Light = vec2(glowtone_lightCoords & ivec2(%d)) / 240.0;
			return sample_lightmap(glowtone_lightMap, ivec2(glowtone_lightCoords.x & %d, glowtone_lightCoords.y & %d));
		}

		""".formatted(
			BloomHelper.EMISSIVE_MARKER,
			BlockMaterialRenderer.MAX_SHADER_INDEX,
			BlockMaterialRenderer.GUI_MARKER,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK
		);

	private static final String VERTEX_HEADER_PLAIN = """
		flat out float glowtone_Emissive;

		vec4 glowtone_sampleLightmap(sampler2D glowtone_lightMap, ivec2 glowtone_lightCoords) {
			glowtone_Emissive = float((glowtone_lightCoords.x & %d) != 0);
			return sample_lightmap(glowtone_lightMap, ivec2(glowtone_lightCoords.x & %d, glowtone_lightCoords.y & %d));
		}

		""".formatted(
			BloomHelper.EMISSIVE_MARKER,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK
		);

	private static final String MATERIAL_VERTEX_DECLARATIONS = """
		flat out int glowtone_Gui;
		flat out int glowtone_Material;
		out vec3 glowtone_WorldPos;
		out vec3 glowtone_AbsPos;
		out vec2 glowtone_Light;
		out vec4 glowtone_ScreenProj;

		""";

	private static final String MATERIAL_VERTEX_SETUP =
		"	glowtone_Gui = 0;" + System.lineSeparator()
			+ "	glowtone_Material = 0;" + System.lineSeparator()
			+ "	glowtone_Light = vec2(0.0);" + System.lineSeparator()
			+ "	glowtone_WorldPos = Position;" + System.lineSeparator()
			+ "	glowtone_AbsPos = vec3(0.0);" + System.lineSeparator() + "	";

	private static final String MATERIAL_VERTEX_PROJECT =
		System.lineSeparator() + "	glowtone_ScreenProj = projection_from_position(gl_Position);";

	private static String vertexDeclarations() {
		return withoutUnusedInputs(MATERIAL_VERTEX_DECLARATIONS, "out");
	}

	private static String fragmentInputs() {
		return withoutUnusedInputs(MATERIAL_FRAGMENT_IN, "in");
	}

	private static boolean usesScreenProj() {
		return MaterialShaderPatcher.usesInput(MaterialShaderPatcher.SCREEN_PROJ);
	}

	private static String screenProjArgument() {
		return usesScreenProj() ? MaterialShaderPatcher.SCREEN_PROJ : "vec4(0.0)";
	}

	private static boolean usesBlockPos() {
		return MaterialShaderPatcher.usesInput(MaterialShaderPatcher.BLOCK_POS) || usesLocalPos();
	}

	private static boolean usesLocalPos() {
		return MaterialShaderPatcher.usesInput(MaterialShaderPatcher.LOCAL_POS);
	}

	private static String blockPosArgument() {
		return usesBlockPos() ? "floor(glowtone_AbsPos)" : "vec3(0.0)";
	}

	private static String localPosArgument() {
		return usesLocalPos() ? "(glowtone_AbsPos - floor(glowtone_AbsPos))" : "vec3(0.0)";
	}

	private static String withoutUnusedInputs(String declarations, String qualifier) {
		String result = declarations;
		if (!usesScreenProj()) result = result.replace(qualifier + " vec4 " + MaterialShaderPatcher.SCREEN_PROJ + ";\n", "");
		if (!usesBlockPos()) result = result.replace(qualifier + " vec3 " + MaterialShaderPatcher.BLOCK_POS + ";\n", "");
		if (!usesLocalPos()) result = result.replace(qualifier + " vec3 " + MaterialShaderPatcher.LOCAL_POS + ";\n", "");

		return result;
	}

	private static String withoutUnusedWrites(String writes) {
		String result = writes;
		if (!usesBlockPos()) result = removeAssignment(result, "glowtone_AbsPos");

		return result;
	}

	private static String removeAssignment(String source, String name) {
		final StringBuilder kept = new StringBuilder(source.length());
		for (String line : source.split("\n", -1)) {
			if (line.contains(name + " =")) continue;
			if (kept.length() > 0) kept.append('\n');

			kept.append(line);
		}

		return kept.toString();
	}

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

	private static String vertexHeader(String source) {
		if (!MaterialShaderPatcher.anyFragment() && !MaterialShaderPatcher.anyVertex()) return VERTEX_HEADER_PLAIN;
		if (!MaterialShaderPatcher.anyVertex()) return vertexDeclarations() + VERTEX_HEADER;

		// Only terrain imports the globals block, and displacement needs its game time everywhere.
		final String globals = source.contains(GAME_TIME_UNIFORM) ? "" : GLOBALS_BLOCK;
		return vertexDeclarations() + globals + MaterialShaderPatcher.generateVertexFunctions()
			+ vertexDisplaceFunction() + VERTEX_HEADER;
	}

	private static String vertexDisplaceFunction() {
		return VERTEX_DISPLACE_FUNCTION.formatted(
			BlockMaterialRenderer.MAX_SHADER_INDEX,
			MaterialShaderPatcher.VERTEX_DISPATCH,
			BloomHelper.LIGHT_COORDS_CHANNEL_MASK
		);
	}

	private static final String TERRAIN_POSITION_ANCHOR =
		"vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;";

	private static final String TERRAIN_POSITION_DISPLACED =
		"vec3 pos = (Position + glowtone_displace(Position, Position + vec3(ChunkPosition), UV2, GameTime))"
			+ " + (ChunkPosition - CameraBlockPos) + CameraOffset;";

	// block.vsh offsets the model before projecting; the others project the raw attribute.
	private static final String MODEL_OFFSET_ANCHOR = "vec3 pos = Position + ModelOffset;";

	private static final String MODEL_OFFSET_DISPLACED =
		"vec3 pos = (Position + glowtone_displace(Position, Position, UV2, GameTime)) + ModelOffset;";

	private static final String GENERIC_POSITION_ANCHOR = "vec4(Position, 1.0)";

	private static final String GENERIC_POSITION_DISPLACED =
		"vec4(Position + glowtone_displace(Position, Position, UV2, GameTime), 1.0)";

	private static String vertexFooter() {
		if (!MaterialShaderPatcher.anyFragment() && !MaterialShaderPatcher.anyVertex()) return VERTEX_FOOTER;

		return VERTEX_FOOTER.replace(
			"	glowtone_main();",
			withoutUnusedWrites(MATERIAL_VERTEX_SETUP) + "glowtone_main();" + (usesScreenProj() ? MATERIAL_VERTEX_PROJECT : "")
		);
	}

	// Reads the index off the light coords directly - the varying is not assigned until the lightmap is sampled after the position is used.
	private static final String VERTEX_DISPLACE_FUNCTION = """
		vec3 glowtone_displace(vec3 glowtone_pos, vec3 glowtone_world, ivec2 glowtone_coords, float glowtone_time) {
			int glowtone_index = (glowtone_coords.y >> 8) & %d;
			if (glowtone_index == 0) return vec3(0.0);

			vec3 glowtone_block = floor(glowtone_world);
			return %s(glowtone_index, glowtone_pos, glowtone_block, glowtone_world - glowtone_block,
				vec2(glowtone_coords & ivec2(%d)) / 240.0, glowtone_time, 0);
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

	private EmissiveShaderPatcher() {}

	private static final Identifier SODIUM_TERRAIN_FRAGMENT =
		Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");

	public static boolean usesMaterialSamplers(Identifier fragmentShader) {
		return LIT_SHADERS.contains(fragmentShader);
	}

	public static boolean isLitShader(Identifier id) {
		return LIT_SHADERS.contains(id) || SODIUM_TERRAIN_FRAGMENT.equals(id);
	}

	private static final String MIX_LIGHT =
		"vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);";
	private static final String MIX_LIGHT_BACK =
		"vertexPerFaceColorBack = minecraft_mix_light_separate(-light, Color);";
	private static final String MIX_LIGHT_FRONT =
		"vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);";

	private static final String DIFFUSE_ACCUM = "float lightAccum = min(1.0, (lightValue.x + lightValue.y) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);";
	private static final String DIFFUSE_FLAT = "float lightAccum = 1.0;";

	private static String unshadeEmissiveFaces(String source) {
		if (GlowtoneConstants.GLOWTONE_NO_SHADING || !GlowtoneConstants.GLOWTONE_SHADING) return source;
		if (!source.contains(MIX_LIGHT) && !source.contains(MIX_LIGHT_FRONT)) return source;

		final String emissive = "((UV2.x & " + BloomHelper.EMISSIVE_MARKER + ") != 0)";
		return source
			.replace(MIX_LIGHT, "vertexColor = " + emissive
				+ " ? Color : minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);")
			.replace(MIX_LIGHT_BACK, "vertexPerFaceColorBack = " + emissive
				+ " ? Color : minecraft_mix_light_separate(-light, Color);")
			.replace(MIX_LIGHT_FRONT, "vertexPerFaceColorFront = " + emissive
				+ " ? Color : minecraft_mix_light_separate(light, Color);");
	}

	private static String flattenDiffuse(String source) {
		if (!GlowtoneConstants.GLOWTONE_NO_SHADING) return source;

		return source.replace(DIFFUSE_ACCUM, DIFFUSE_FLAT);
	}

	public static String patch(Identifier id, ShaderType type, String source) {
		return GlowtoneShaderDump.record(id, type, source, patchStages(id, type, source));
	}

	private static String patchStages(Identifier id, ShaderType type, String source) {
		if (!source.contains(MAIN)) return source;

		if (source.contains(SODIUM_VERTEX_MARKER) || source.contains(SODIUM_FOG_CALL)) {
			return patchSodium(type, source);
		}

		source = flattenDiffuse(source);

		if (LIT_SHADERS.contains(id)) {
			if (type == ShaderType.VERTEX) {
				final String vertex = patchVertex(source);
				final String finalVertex = id.equals(TERRAIN)
					? patchTerrainVertex(vertex)
					: unshadeEmissiveFaces(vertex);
				return finalVertex;
			}

			final boolean terrain = id.equals(TERRAIN);
			final String patched = patchFragment(source, id, terrain);
			final String finalFragment = terrain ? patchTerrainLines(patched) : patched;
			return finalFragment;
		}
		if (SELF_LIT_SHADERS.contains(id) && type == ShaderType.FRAGMENT) {
			return patchSelfLitFragment(source);
		}
		return source;
	}

	private static final String SODIUM_VERTEX_MARKER = "_vert_init();";
	private static final String SODIUM_COLOR_OUT = "out vec4 v_Color;";
	private static final String SODIUM_VERTEX_TAIL = "v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;";
	private static final String SODIUM_FRAG_OUT = "out vec4 fragColor;";
	private static final String SODIUM_FOG_CALL = "fragColor = _linearFog(color,";
	private static final String SODIUM_TARGET = "color";

	private static final String SODIUM_EMISSIVE_ATTRIBUTES = """
		in vec4 a_GlowtoneFlags;

		flat out float glowtone_Emissive;

		""";

	private static final String SODIUM_EMISSIVE_WRITE_VERTEX = System.lineSeparator() + "    glowtone_Emissive = a_GlowtoneFlags.r;";
	private static final String SODIUM_EMISSIVE_FRAGMENT = """
		flat in float glowtone_Emissive;
		layout(location = 1) out vec4 glowtone_EmissiveColor;

		""";

	private static final String SODIUM_FRAG_OUT_RELOCATED = "layout(location = 0) out vec4 fragColor;";
	private static final String SODIUM_FOG_SURVIVAL = """
		float glowtone_fogSurvival() {
		#ifdef USE_FOG
			float glowtone_fog = max(1.0 - fadeFactor, total_fog_value(
				v_FragDistance.y, v_FragDistance.x,
				u_EnvironmentFog.x, u_EnvironmentFog.y, u_RenderFog.x, u_RenderFog.y));
			return clamp(1.0 - glowtone_fog * u_FogColor.a, 0.0, 1.0);
		#else
			return 1.0;
		#endif
		}

		""";

	private static final String SODIUM_ATTRIBUTES = """
		in vec4 a_GlowtoneEdge;
		in vec4 a_GlowtoneEdgeMask;
		in vec4 a_GlowtoneContact0;
		in vec4 a_GlowtoneContact1;
		in vec4 a_GlowtoneContact2;
		in vec4 a_GlowtoneContact3;

		out float glowtone_Height;
		out vec4 glowtone_EdgeDist;
		out vec4 glowtone_EdgeMask;
		out vec4 glowtone_Shade;
		out float glowtone_ViewDist;
		flat out vec4 glowtone_Contact0;
		flat out vec4 glowtone_Contact1;
		flat out vec4 glowtone_Contact2;
		flat out vec4 glowtone_Contact3;

		""";

	private static final String SODIUM_WRITES = """

			glowtone_Height = position.y;
			glowtone_ViewDist = length(position);
			glowtone_EdgeDist = a_GlowtoneEdge;
			glowtone_EdgeMask = a_GlowtoneEdgeMask;
			glowtone_Shade = _vert_color;
			glowtone_Contact0 = a_GlowtoneContact0;
			glowtone_Contact1 = a_GlowtoneContact1;
			glowtone_Contact2 = a_GlowtoneContact2;
			glowtone_Contact3 = a_GlowtoneContact3;
		""";

	private static final String SODIUM_MATERIAL_VERTEX = """
		flat out int glowtone_Material;
		out vec3 glowtone_WorldPos;
		out vec3 glowtone_AbsPos;
		out vec2 glowtone_Light;
		out vec4 glowtone_ScreenProj;
		out float glowtone_GameTime;

		""";

	private static final String SODIUM_MATERIAL_WRITES =
		System.lineSeparator()
			+ "    glowtone_Material = int(a_GlowtoneFlags.g * 255.0 + 0.5);" + System.lineSeparator()
			+ "    glowtone_WorldPos = position;" + System.lineSeparator()
			+ "    glowtone_AbsPos = position + vec3(CameraBlockPos) - CameraOffset;" + System.lineSeparator()
			+ "    glowtone_Light = vec2(0.0);" + System.lineSeparator()
			+ "    vec4 glowtone_halfClip = gl_Position * 0.5;" + System.lineSeparator()
			+ "    glowtone_ScreenProj = vec4(glowtone_halfClip.x + glowtone_halfClip.w,"
			+ " glowtone_halfClip.y + glowtone_halfClip.w, gl_Position.z, gl_Position.w);" + System.lineSeparator()
			+ "    glowtone_GameTime = float(u_CurrentTime % 1200000) / 1200000.0;";

	private static final String SODIUM_MATERIAL_FRAGMENT = """
		flat in int glowtone_Material;
		in vec3 glowtone_WorldPos;
		in vec3 glowtone_AbsPos;
		in vec2 glowtone_Light;
		in vec4 glowtone_ScreenProj;
		in float glowtone_GameTime;

		vec3 glowtone_faceNormal() {
			return normalize(cross(dFdx(glowtone_WorldPos), dFdy(glowtone_WorldPos)));
		}

		vec4 glowtone_sampleSlot(sampler2D glowtone_atlas, vec4 glowtone_rect, vec2 glowtone_uv) {
			vec2 glowtone_span = vec2(glowtone_rect.y - glowtone_rect.x, glowtone_rect.w - glowtone_rect.z);
			vec2 glowtone_coord = vec2(glowtone_rect.x, glowtone_rect.z) + fract(glowtone_uv) * glowtone_span;
			return textureGrad(
				glowtone_atlas,
				glowtone_coord,
				dFdx(glowtone_uv) * glowtone_span,
				dFdy(glowtone_uv) * glowtone_span
			);
		}

		vec4 glowtone_sampleSlotProj(sampler2D glowtone_atlas, vec4 glowtone_rect, vec4 glowtone_uv) {
			return glowtone_sampleSlot(glowtone_atlas, glowtone_rect, glowtone_uv.xy / glowtone_uv.w);
		}

		""";

	private static String sodiumMaterialCall() {
		return "if (glowtone_Material != 0) color = " + MaterialShaderPatcher.DISPATCH
			+ "(u_BlockTex, glowtone_Material, color, v_TexCoord, glowtone_WorldPos, " + blockPosArgument() + ","
			+ " " + localPosArgument() + ", glowtone_faceNormal(), " + screenProjArgument() + ", glowtone_Light, GameTime, 0);"
			+ System.lineSeparator() + "    ";
	}

	private static final String SODIUM_LIGHT_COORD = "_vert_tex_light_coord";

	private static final String SODIUM_POSITION_ANCHOR = "vec3 position =";

	// Sodium's vertex stage has no Globals block of its own, and the block position needs the camera.
	private static String sodiumDisplace(String source) {
		final int declaration = source.indexOf(SODIUM_POSITION_ANCHOR);
		final int end = declaration < 0 ? -1 : source.indexOf(';', declaration);
		if (end < 0) {
			MATERIAL_LOGGER.error("Glowtone could not apply material vertex shaders: Sodium's position anchor is missing");
			return source;
		}

		final String displace = System.lineSeparator()
			+ "    int glowtone_vIndex = int(a_GlowtoneFlags.g * 255.0 + 0.5);" + System.lineSeparator()
			+ "    if (glowtone_vIndex != 0) {" + System.lineSeparator()
			+ "        vec3 glowtone_vAbs = position + vec3(CameraBlockPos) - CameraOffset;" + System.lineSeparator()
			+ "        vec3 glowtone_vBlock = floor(glowtone_vAbs);" + System.lineSeparator()
			+ "        position += " + MaterialShaderPatcher.VERTEX_DISPATCH
			+ "(glowtone_vIndex, position, glowtone_vBlock, glowtone_vAbs - glowtone_vBlock, vec2(0.0), GameTime, 0);"
			+ System.lineSeparator() + "    }";

		return source.substring(0, end + 1) + displace + source.substring(end + 1);
	}

	private static String patchSodiumMaterialVertex(String source) {
		if (!MaterialShaderPatcher.any()
			|| !source.contains(SODIUM_COLOR_OUT)
			|| !source.contains(SODIUM_VERTEX_TAIL)
			|| source.contains("glowtone_Material")
		) {
			return source;
		}

		final boolean hasLight = source.contains(SODIUM_LIGHT_COORD);

		if (MaterialShaderPatcher.anyVertex()) {
			source = sodiumDisplace(
				source.replace(SODIUM_COLOR_OUT, GLOBALS_BLOCK + MaterialShaderPatcher.generateVertexFunctions() + SODIUM_COLOR_OUT)
			);
		}

		final String patched = source
			.replace(SODIUM_COLOR_OUT, SODIUM_MATERIAL_VERTEX + SODIUM_COLOR_OUT)
			.replace(SODIUM_VERTEX_TAIL, SODIUM_VERTEX_TAIL + withoutUnusedWrites(SODIUM_MATERIAL_WRITES));

		return hasLight
			? patched.replace("glowtone_Light = vec2(0.0);", "glowtone_Light = " + SODIUM_LIGHT_COORD + ";")
			: patched;
	}

	private static String patchSodiumMaterialFragment(String source) {
		if (!source.contains(SODIUM_FOG_CALL) || source.contains("GlowtoneMaterialTex0")) return source;

		final String keep = "color.r += glowtone_keepSamplers();" + System.lineSeparator() + "    ";
		final String declared = source
			.replace(MAIN, samplerDeclarations() + MAIN)
			.replace(SODIUM_FOG_CALL, keep + SODIUM_FOG_CALL);

		if (!MaterialShaderPatcher.any()) return declared;

		MATERIAL_LOGGER.info("Glowtone injected {} material shaders into Sodium's terrain fragment shader", MaterialShaderPatcher.loaded().size());
		return declared
			.replace(MAIN, GLOBALS_BLOCK + SODIUM_MATERIAL_FRAGMENT + MaterialShaderPatcher.generateFunctions(true) + MAIN)
			.replace(SODIUM_FOG_CALL, sodiumMaterialCall() + SODIUM_FOG_CALL);
	}

	private static String patchSodium(ShaderType type, String source) {
		final float shade = AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING ? OcclusionStrengthOption.strength() : 0F;
		final float option = EdgeHighlightOption.strength();
		final float highlight = blockHighlight(option);
		final float liquid = option * GlowtonePackSettings.waterStrength();
		final boolean occlusionView = aoDebug();

		final String emissive = type == ShaderType.VERTEX
			? patchSodiumEmissiveVertex(source)
			: patchSodiumEmissiveFragment(source);

		final String material = type == ShaderType.VERTEX
			? patchSodiumMaterialVertex(emissive)
			: patchSodiumMaterialFragment(emissive);

		if (shade <= 0F && highlight <= 0F && liquid <= 0F && !occlusionView) return material;

		return type == ShaderType.VERTEX
			? patchSodiumVertex(material)
			: patchSodiumFragment(material, shade, highlight, liquid, occlusionView);
	}

	private static String patchSodiumEmissiveVertex(String source) {
		if (!source.contains(SODIUM_COLOR_OUT)
			|| !source.contains(SODIUM_VERTEX_TAIL)
			|| source.contains("a_GlowtoneFlags")
		) {
			return source;
		}

		return source
			.replace(SODIUM_COLOR_OUT, SODIUM_EMISSIVE_ATTRIBUTES + SODIUM_COLOR_OUT)
			.replace(SODIUM_VERTEX_TAIL, SODIUM_VERTEX_TAIL + SODIUM_EMISSIVE_WRITE_VERTEX);
	}

	private static String patchSodiumEmissiveFragment(String source) {
		if (!source.contains(SODIUM_FRAG_OUT)
			|| !source.contains(SODIUM_FOG_CALL)
			|| source.contains("glowtone_EmissiveColor")
		) {
			return source;
		}

		final String newline = System.lineSeparator();
		return source
			.replace(SODIUM_FRAG_OUT, SODIUM_FRAG_OUT_RELOCATED + newline + newline + SODIUM_EMISSIVE_FRAGMENT)
			.replace(SODIUM_FOG_CALL, SODIUM_FOG_CALL)
			.replace(MAIN, SODIUM_FOG_SURVIVAL + MAIN)
			.replace(
				"fadeFactor);",
				"fadeFactor);" + newline
					+ "    glowtone_EmissiveColor = vec4("
					+ "color.rgb * glowtone_Emissive * glowtone_fogSurvival(), fragColor.a);"
			);
	}

	private static String patchSodiumVertex(String source) {
		if (!source.contains(SODIUM_COLOR_OUT)
			|| !source.contains(SODIUM_VERTEX_TAIL)
			|| source.contains("a_GlowtoneEdge")
		) {
			return source;
		}

		return source
			.replace(SODIUM_COLOR_OUT, SODIUM_ATTRIBUTES + SODIUM_COLOR_OUT)
			.replace(SODIUM_VERTEX_TAIL, SODIUM_VERTEX_TAIL + SODIUM_WRITES);
	}

	private static String patchSodiumFragment(
		String source, float shade, float highlight, float liquid, boolean occlusionView
	) {
		final String fragOut = source.contains(SODIUM_FRAG_OUT_RELOCATED)
			? SODIUM_FRAG_OUT_RELOCATED : SODIUM_FRAG_OUT;
		if (!source.contains(fragOut) || !source.contains(SODIUM_FOG_CALL)) return source;

		final String newline = System.lineSeparator();
		final boolean lines = (highlight > 0F || liquid > 0F) && (!occlusionView || edgeDebugColour());

		final StringBuilder header = new StringBuilder(EDGE_DATA_HEADER);
		final StringBuilder body = new StringBuilder();

		if (occlusionView || shade > 0F) header.append(aoHeader());
		if (lines) header.append(sodiumEdgeHeader());

		if (occlusionView) body.append(sodiumOcclusionBody(shade, true)).append(newline);
		if (lines) body.append(sodiumHighlightBody(highlight, liquid)).append(newline);
		if (shade > 0F && !occlusionView) body.append(sodiumOcclusionBody(shade, false)).append(newline);

		body.append("    ").append(SODIUM_TARGET).append(".rgb += vec3(glowtone_keepVaryings());")
			.append(newline).append("    ");

		return source
			.replace(fragOut, fragOut + newline + newline + header)
			.replace(SODIUM_FOG_CALL, body + SODIUM_FOG_CALL);
	}

	private static String sodiumEdgeHeader() {
		final String edgeReturn = edgeDebugColour() ? EDGE_RETURN_DEBUG
			: EDGE_RETURN_NORMAL.formatted(
				Float.toString(EDGE_ANCHOR),
				Float.toString(EDGE_FLATTEN),
				Float.toString(EDGE_WHITEN)
			).replace("vertexColor", "v_Color");

		return EDGE_HEADER.formatted(
			Float.toString(GlowtonePackSettings.waterSize()),
			edgeStyle(GlowtonePackSettings.waterStyle(), GlowtonePackSettings.waterSize()),
			Float.toString(GlowtonePackSettings.highlightSize()),
			edgeStyle(GlowtonePackSettings.highlightStyle(), GlowtonePackSettings.highlightSize()),
			"0.5",
			edgeCorners(GlowtonePackSettings.highlightCorners()),
			"glowtone_ViewDist",
			edgeReturn,
			Float.toString(GlowtonePackSettings.waterDistance()),
			Float.toString(GlowtonePackSettings.waterFloor()),
			Float.toString(GlowtonePackSettings.waterWhiten()),
			Float.toString(GlowtonePackSettings.waterLift()),
			Float.toString(GlowtonePackSettings.waterOpacity())
		);
	}

	private static String sodiumOcclusionBody(float strength, boolean view) {
		final String depth = Float.toString(view ? occlusionDepth() : strength);
		final String shaded = "dot(glowtone_Shade.rgb, vec3(0.2126, 0.7152, 0.0722))";
		if (!view) {
			return "    " + SODIUM_TARGET + ".rgb *= max(1.0 - glowtone_ambientOcclusion() * "
				+ depth + ", 0.0);";
		}
		return "    " + SODIUM_TARGET + ".rgb = vec3(" + shaded
			+ (AmbientOcclusionOption.vanillaActive() ? ""
				: " * max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0)")
			+ ");";
	}

	private static String sodiumHighlightBody(float edgeStrength, float liquidStrength) {
		final String newline = System.lineSeparator();
		final String isLiquid = "(glowtone_contactBits(glowtone_Contact3) & "
			+ hex(GlowtoneContactRects.LIQUID_FLAG) + ") != 0u";
		final String liquid = edgeDebugColour()
			? SODIUM_TARGET + ".rgb = glowtone_edgeHighlight(" + SODIUM_TARGET + ".rgb, 1.0);"
			: SODIUM_TARGET + " = glowtone_liquidHighlight(" + SODIUM_TARGET + ", " + liquidStrength + ");";

		return "    if (" + isLiquid + ") {" + newline
			+ "        " + liquid + newline
			+ "    } else {" + newline
			+ "        " + SODIUM_TARGET + ".rgb = glowtone_edgeHighlight(" + SODIUM_TARGET + ".rgb, " + edgeStrength + ");" + newline
			+ "    }";
	}

	private static final String TERRAIN_IN_ANCHOR = "in ivec2 UV2;";
	private static final String TERRAIN_OUT_ANCHOR = "out vec2 texCoord0;";
	private static final String TERRAIN_WRITE_ANCHOR = "texCoord0 = UV0;";

	private static final String TERRAIN_ATTRIBUTES = """
		in vec4 GlowtoneEdge;
		in vec4 GlowtoneEdgeMask;
		in vec4 GlowtoneContact0;
		in vec4 GlowtoneContact1;
		in vec4 GlowtoneContact2;
		in vec4 GlowtoneContact3;
		""";

	private static final String TERRAIN_VARYINGS = """
		out float glowtone_Height;
		out float glowtone_ViewDist;
		out vec4 glowtone_EdgeDist;
		out vec4 glowtone_EdgeMask;
		out vec4 glowtone_Shade;
		flat out vec4 glowtone_Contact0;
		flat out vec4 glowtone_Contact1;
		flat out vec4 glowtone_Contact2;
		flat out vec4 glowtone_Contact3;
		""";

	private static final String TERRAIN_WRITES = """
			glowtone_Height = pos.y;
			glowtone_EdgeDist = GlowtoneEdge;
			glowtone_EdgeMask = GlowtoneEdgeMask;
			glowtone_Shade = Color;
			glowtone_ViewDist = length(pos);
			glowtone_Contact0 = GlowtoneContact0;
			glowtone_Contact1 = GlowtoneContact1;
			glowtone_Contact2 = GlowtoneContact2;
			glowtone_Contact3 = GlowtoneContact3;
		""";

	private static final String MATERIAL_WRITES = """
			glowtone_WorldPos = pos;
			glowtone_AbsPos = Position + vec3(ChunkPosition);
			""";

	private static final String MATERIAL_FRAGMENT_IN = """
		flat in int glowtone_Gui;
		flat in int glowtone_Material;
		in vec3 glowtone_WorldPos;
		in vec3 glowtone_AbsPos;
		in vec2 glowtone_Light;
		in vec4 glowtone_ScreenProj;

		vec3 glowtone_faceNormal() {
			return normalize(cross(dFdx(glowtone_WorldPos), dFdy(glowtone_WorldPos)));
		}

		vec4 glowtone_sampleSlot(sampler2D glowtone_atlas, vec4 glowtone_rect, vec2 glowtone_uv) {
			vec2 glowtone_span = vec2(glowtone_rect.y - glowtone_rect.x, glowtone_rect.w - glowtone_rect.z);
			vec2 glowtone_coord = vec2(glowtone_rect.x, glowtone_rect.z) + fract(glowtone_uv) * glowtone_span;
			return textureGrad(
				glowtone_atlas,
				glowtone_coord,
				dFdx(glowtone_uv) * glowtone_span,
				dFdy(glowtone_uv) * glowtone_span
			);
		}

		vec4 glowtone_sampleSlotProj(sampler2D glowtone_atlas, vec4 glowtone_rect, vec4 glowtone_uv) {
			return glowtone_sampleSlot(glowtone_atlas, glowtone_rect, glowtone_uv.xy / glowtone_uv.w);
		}

		""";

	private static final String GAME_TIME_UNIFORM = "float GameTime;";

	private static final String GLOBALS_BLOCK = """
		layout(std140) uniform Globals {
			ivec3 CameraBlockPos;
			vec3 CameraOffset;
			vec2 ScreenSize;
			float GlintAlpha;
			float GameTime;
			int MenuBlurRadius;
			int UseRgss;
		};

		""";

	private static String materialCall(String time, String context) {
		return "	if (glowtone_Material != 0) fragColor = " + MaterialShaderPatcher.DISPATCH
			+ "(Sampler0, glowtone_Material, fragColor, texCoord0, glowtone_WorldPos, " + blockPosArgument() + ","
			+ " " + localPosArgument() + ", glowtone_faceNormal(), " + screenProjArgument() + ", glowtone_Light, "
			+ time + ", " + context + ");"
			+ System.lineSeparator() + "	";
	}

	private static String contextFor(Identifier id) {
		if (id.equals(TERRAIN)) return "0";

		final String base;
		if (id.equals(Identifier.withDefaultNamespace("core/block"))) {
			base = "1";
		} else if (id.equals(Identifier.withDefaultNamespace("core/entity"))) {
			base = "2";
		} else if (id.equals(Identifier.withDefaultNamespace("core/item"))) {
			base = "3";
		} else {
			base = "4";
		}

		return "(glowtone_Gui != 0 ? 5 : " + base + ")";
	}

	private static String samplerDeclarations() {
		return MaterialShaderPatcher.anySamplers() ? MaterialSamplers.declarations() : "";
	}

	private static String keepSamplers() {
		return MaterialShaderPatcher.anySamplers() ? KEEP_SAMPLERS : "";
	}

	private static final String KEEP_SAMPLERS =
		"	fragColor.r += glowtone_keepSamplers();" + System.lineSeparator() + "	";

	private static final String GENERIC_FOG_APPLY =
		"	fragColor.rgb = mix(fragColor.rgb, glowtone_FogTint, glowtone_FogAmount);"
			+ System.lineSeparator() + "	";

	private static boolean defersFog(String source, boolean terrain) {
		return !terrain && MaterialShaderPatcher.anyFragment() && source.contains(APPLY_FOG);
	}

	private static String fragmentHeader(String source, boolean terrain) {
		if (!MaterialShaderPatcher.anyFragment()) return FRAGMENT_HEADER;

		final String globals = source.contains(GAME_TIME_UNIFORM) ? "" : GLOBALS_BLOCK;
		final String functions = MaterialShaderPatcher.generateFunctions(true);
		final String fog = defersFog(source, terrain) ? FOG_HEADER : "";

		return globals + samplerDeclarations() + fragmentInputs() + functions + fog + FRAGMENT_HEADER;
	}

	private static String fragmentFooter(String source, Identifier id, boolean terrain) {
		if (!MaterialShaderPatcher.anyFragment()) return FRAGMENT_FOOTER;
		if (terrain) return FRAGMENT_FOOTER.replace(EMISSIVE_WRITE, keepSamplers() + EMISSIVE_WRITE);

		final String fog = defersFog(source, terrain) ? GENERIC_FOG_APPLY : "";
		return FRAGMENT_FOOTER.replace(
			EMISSIVE_WRITE,
			materialCall("GameTime", contextFor(id)) + fog + keepSamplers() + EMISSIVE_WRITE
		);
	}

	private static String materialCall() {
		return "	if (glowtone_Material != 0) fragColor = " + MaterialShaderPatcher.DISPATCH
			+ "(Sampler0, glowtone_Material, fragColor, texCoord0, glowtone_WorldPos, " + blockPosArgument() + ","
			+ " " + localPosArgument() + ", glowtone_faceNormal(), " + screenProjArgument() + ", glowtone_Light, GameTime, 0);"
			+ System.lineSeparator()
			+ "	";
	}

	private static String patchMaterialFragment(String source) {
		if (!MaterialShaderPatcher.anyFragment()) return source;
		if (!source.contains(EMISSIVE_WRITE)) {
			MATERIAL_LOGGER.error("Glowtone could not inject material shaders into the terrain fragment shader: anchor missing");
			return source;
		}

		MATERIAL_LOGGER.info("Glowtone injected {} material shaders into the terrain fragment shader", MaterialShaderPatcher.loaded().size());
		return source.replace(EMISSIVE_WRITE, materialCall() + EMISSIVE_WRITE);
	}

	private static String patchTerrainVertex(String source) {
		if (!source.contains(TERRAIN_IN_ANCHOR)
			|| !source.contains(TERRAIN_OUT_ANCHOR)
			|| !source.contains(TERRAIN_WRITE_ANCHOR)) {
			return source;
		}

		final String varyings = TERRAIN_VARYINGS;
		final boolean materials = MaterialShaderPatcher.anyFragment() || MaterialShaderPatcher.anyVertex();
		final String writes = materials ? TERRAIN_WRITES + withoutUnusedWrites(MATERIAL_WRITES) : TERRAIN_WRITES;

		if (MaterialShaderPatcher.anyVertex()) {
			if (source.contains(TERRAIN_POSITION_ANCHOR)) {
				source = source.replace(TERRAIN_POSITION_ANCHOR, TERRAIN_POSITION_DISPLACED);
			} else {
				MATERIAL_LOGGER.error("Glowtone could not apply material vertex shaders: the terrain position anchor is missing");
			}
		}

		return source
			.replace(TERRAIN_IN_ANCHOR, TERRAIN_IN_ANCHOR + System.lineSeparator() + TERRAIN_ATTRIBUTES)
			.replace(TERRAIN_OUT_ANCHOR, TERRAIN_OUT_ANCHOR + System.lineSeparator() + varyings)
			.replace(TERRAIN_WRITE_ANCHOR, TERRAIN_WRITE_ANCHOR + System.lineSeparator() + writes);
	}

	private static String patchVertex(String source) {
		if (!source.contains(SAMPLE_LIGHTMAP)) return source;

		final String displaced = MaterialShaderPatcher.anyVertex() ? displaceGeneric(source) : source;

		return displaced.replace(SAMPLE_LIGHTMAP, GLOWTONE_SAMPLE_LIGHTMAP)
			.replace(MAIN, vertexHeader(source) + GLOWTONE_MAIN) + vertexFooter();
	}

	private static String displaceGeneric(String source) {
		if (source.contains(MODEL_OFFSET_ANCHOR)) return source.replace(MODEL_OFFSET_ANCHOR, MODEL_OFFSET_DISPLACED);
		if (source.contains(GENERIC_POSITION_ANCHOR)) return source.replace(GENERIC_POSITION_ANCHOR, GENERIC_POSITION_DISPLACED);

		return source;
	}

	private static String patchFragment(String source, Identifier id, boolean terrain) {
		if (!source.contains(FRAG_COLOR_OUT)) return source;

		final String deferred = defersFog(source, terrain) ? source.replace(APPLY_FOG, DEFER_FOG) : source;
		return deferred.replace(FRAG_COLOR_OUT, GLOWTONE_FRAG_COLOR_OUT)
			.replace(MAIN, fragmentHeader(source, terrain) + GLOWTONE_MAIN) + fragmentFooter(source, id, terrain);
	}

	private static String patchTerrainLines(String source) {
		final float shade = AmbientOcclusionOption.glowtoneActive()
			&& AmbientOcclusionOption.SHADER_CONTACT_SHADING ? OcclusionStrengthOption.strength() : 0F;
		final float option = EdgeHighlightOption.strength();
		final float highlight = blockHighlight(option);
		final float liquid = option * GlowtonePackSettings.waterStrength();
		final boolean occlusionView = aoDebug();
		if (shade <= 0F && highlight <= 0F && liquid <= 0F && !occlusionView) {
			return MaterialShaderPatcher.anyFragment() ? deferFog(patchMaterialFragment(source)) : source;
		}

		String patched = source.replace(GLOWTONE_MAIN, EDGE_DATA_HEADER + GLOWTONE_MAIN);

		final boolean lines = (highlight > 0F || liquid > 0F) && (!occlusionView || edgeDebugColour());

		if (occlusionView) patched = patchAmbientOcclusion(patched, shade);
		if (lines) patched = patchEdgeHighlight(patched, highlight, liquid);
		if (shade > 0F && !occlusionView) patched = patchAmbientOcclusion(patched, shade);

		return MaterialShaderPatcher.anyFragment()
			? deferFog(patchMaterialFragment(patched))
			: deferFog(patched);
	}

	private static String deferFog(String source) {
		if (!source.contains(APPLY_FOG) || !source.contains(EMISSIVE_WRITE)) return source;

		final String faded = source.contains(CHUNK_FADE)
			? source.replace(CHUNK_FADE, CHUNK_FADE_ALPHA)
			: source;

		final String deferred = faded
			.replace(APPLY_FOG, DEFER_FOG)
			.replace(GLOWTONE_MAIN, FOG_HEADER + GLOWTONE_MAIN);
		if (debugView()) return deferred;

		final String newline = System.lineSeparator();
		final String apply = PRE_FOG_CAPTURE + newline + FOG_APPLY + newline + "	";

		return deferred.replace(EMISSIVE_WRITE, apply + FOGGED_EMISSIVE_WRITE);
	}

	private static float occlusionDepth() {
		if (AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING) {
			return OcclusionStrengthOption.strength();
		}
		if (AmbientOcclusionOption.vanillaActive()) {
			return OcclusionStrengthOption.VANILLA_DEPTH * OcclusionStrengthOption.COVERAGE_REFERENCE;
		}
		return 0F;
	}

	private static float blockHighlight(float option) {
		if (GlowtonePackSettings.highlightSource() == GlowtonePackSettings.Source.POST) return 0F;
		return option * GlowtonePackSettings.highlightStrength();
	}

	private static String edgeCorners(GlowtonePackSettings.Corners corners) {
		if (corners == GlowtonePackSettings.Corners.NUB) {
			return "			vec4 glowtone_rest = 1.0 - clamp(glowtone_lit, 0.0, 1.0);" + System.lineSeparator()
				+ "			return 1.0 - glowtone_rest.r * glowtone_rest.g * glowtone_rest.b * glowtone_rest.a;";
		}

		return "			return max(max(glowtone_lit.r, glowtone_lit.g), max(glowtone_lit.b, glowtone_lit.a));";
	}

	private static String edgeStyle(GlowtonePackSettings.Style style, float size) {
		if (style == GlowtonePackSettings.Style.HARD) {
			return "			glowtone_reach = step(vec4(1.0E-4), glowtone_reach);";
		}

		final String width = Float.toString(Math.max(size, 1.0E-4F));
		return "			glowtone_reach *= clamp("
			+ "(vec4(" + width + ") - glowtone_EdgeDist * 255.0) / " + width + ", 0.0, 1.0);";
	}

	private static String aoHeader() {
		return AO_HEADER.formatted(
			Float.toString(AO_RADIUS_UNITS),
			hex(GlowtoneContactRects.OCCUPIED_FLAG),
			hex(GlowtoneContactRects.GRID_FLAG)
		);
	}

	private static String patchAmbientOcclusion(String source, float strength) {
		final boolean view = aoDebug();
		final String depth = Float.toString(view ? occlusionDepth() : strength);
		final String shaded = "dot(glowtone_Shade.rgb, vec3(0.2126, 0.7152, 0.0722))";
		final String occlusion = view
			? "	fragColor.rgb = vec3(" + shaded
				+ (AmbientOcclusionOption.vanillaActive() ? ""
					: " * max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0)")
				+ ");"
			: "	fragColor.rgb *= max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0);";
		final String body = occlusion
			+ System.lineSeparator()
			+ "	fragColor.rgb += vec3(glowtone_keepVaryings());";

		return source
			.replace(GLOWTONE_MAIN, aoHeader() + GLOWTONE_MAIN)
			.replace(EMISSIVE_WRITE, guarded(SHADED_TERRAIN_DEFINE, body) + EMISSIVE_WRITE);
	}

	private static String patchEdgeHighlight(String source, float edgeStrength, float liquidStrength) {
		final float overlayStrength = edgeStrength / Math.max(EdgeHighlightOption.strength(), 1.0E-4F);
		final String edgeReturn = edgeDebugColour() ? EDGE_RETURN_DEBUG
			: EDGE_RETURN_NORMAL.formatted(
				Float.toString(EDGE_ANCHOR),
				Float.toString(EDGE_FLATTEN),
				Float.toString(EDGE_WHITEN)
			);
		final String header = EDGE_HEADER.formatted(
			Float.toString(GlowtonePackSettings.waterSize()),
			edgeStyle(GlowtonePackSettings.waterStyle(), GlowtonePackSettings.waterSize()),
			Float.toString(GlowtonePackSettings.highlightSize()),
			edgeStyle(GlowtonePackSettings.highlightStyle(), GlowtonePackSettings.highlightSize()),
			"0.5",
			edgeCorners(GlowtonePackSettings.highlightCorners()),
			"glowtone_ViewDist",
			edgeReturn,
			Float.toString(GlowtonePackSettings.waterDistance()),
			Float.toString(GlowtonePackSettings.waterFloor()),
			Float.toString(GlowtonePackSettings.waterWhiten()),
			Float.toString(GlowtonePackSettings.waterLift()),
			Float.toString(GlowtonePackSettings.waterOpacity())
		);
		final String newline = System.lineSeparator();
		final String isLiquid = "(glowtone_contactBits(glowtone_Contact3) & "
			+ hex(GlowtoneContactRects.LIQUID_FLAG) + ") != 0u";
		final String foam = edgeDebugColour()
			? "fragColor.rgb = glowtone_edgeHighlight(fragColor.rgb, 1.0);"
			: "fragColor = glowtone_liquidHighlight(fragColor, "
				+ Float.toString(liquidStrength) + ");";

		final String opaque = "	if (" + isLiquid + ") {" + newline
			+ "		" + foam + newline
			+ "	} else {" + newline
			+ "		fragColor.rgb = glowtone_edgeHighlight(fragColor.rgb, "
			+ Float.toString(edgeStrength) + ");" + newline
			+ "	}";

		final String liquid = "	if (" + isLiquid + ") {" + newline
			+ "		" + foam + newline
			+ "	}";

		final String keep = "	fragColor.rgb += vec3(glowtone_keepVaryings());" + newline;

		return source
			.replace(GLOWTONE_MAIN, header + GLOWTONE_MAIN)
			.replace(EMISSIVE_WRITE, guarded(OPAQUE_TERRAIN_DEFINE, opaque)
				+ guarded(TRANSLUCENT_TERRAIN_DEFINE, liquid) + keep + EMISSIVE_WRITE);
	}

	private static String hex(int bits) {
		return "0x" + Integer.toHexString(bits) + "u";
	}

	private static String guarded(String define, String body) {
		final String newline = System.lineSeparator();
		return "#ifdef " + define + newline + body + newline + "#endif" + newline + "	";
	}

	private static String patchSelfLitFragment(String source) {
		if (!source.contains(FRAG_COLOR_OUT)) return source;
		return source.replace(FRAG_COLOR_OUT, GLOWTONE_FRAG_COLOR_OUT).replace(MAIN, SELF_LIT_FRAGMENT_HEADER + GLOWTONE_MAIN)
			+ SELF_LIT_FRAGMENT_FOOTER;
	}
}
