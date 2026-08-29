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
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
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

	public static final String OPAQUE_TERRAIN_DEFINE = "GLOWTONE_OPAQUE_TERRAIN";
	public static final String SHADED_TERRAIN_DEFINE = "GLOWTONE_SHADED_TERRAIN";
	public static final String TRANSLUCENT_TERRAIN_DEFINE = "GLOWTONE_TRANSLUCENT_TERRAIN";

	private static final float LIQUID_STRENGTH_SCALE = 5F;
	private static final float LIQUID_FACING_FADE = 0.5F;

	private static final float EDGE_WIDTH_UNITS = 1F;
	private static final float EDGE_ANCHOR = 0.95F;
	private static final float EDGE_FLATTEN = 0.85F;
	private static final float EDGE_WHITEN = 0.70F;

	private static final float LIQUID_WHITEN = 0.8F;
	private static final float LIQUID_FLOOR = 0.6F;
	private static final float LIQUID_LIFT = 1.7F;
	private static final float LIQUID_OPACITY = 0.9F;
	private static final float AO_RADIUS_UNITS = GlowtoneContactRects.RADIUS_UNITS;

	private static final Identifier TERRAIN = Identifier.withDefaultNamespace("core/terrain");

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
		flat in vec4 glowtone_Contact0;
		flat in vec4 glowtone_Contact1;
		flat in vec4 glowtone_Contact2;
		flat in vec4 glowtone_Contact3;

		float glowtone_keepVaryings() {
			return 1e-20 * (glowtone_Height
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
			return 1.0 - smoothstep(0.0, %s, glowtone_Height);
		}

		vec4 glowtone_edgeLit() {
			return glowtone_edgeReach(%s) * step(vec4(%s), glowtone_EdgeMask);
		}

		float glowtone_edgeRim(vec4 glowtone_lit) {
			return max(max(glowtone_lit.r, glowtone_lit.g), max(glowtone_lit.b, glowtone_lit.a));
		}

		vec3 glowtone_edgeHighlight(vec3 glowtone_colour, float glowtone_strength) {
			vec4 glowtone_lit = glowtone_edgeLit();
			float glowtone_rim = glowtone_edgeRim(glowtone_lit);
		%s
		}

		vec4 glowtone_liquidHighlight(vec4 glowtone_colour, float glowtone_strength) {
			float glowtone_rim = min(1.0, glowtone_strength * glowtone_liquidFacing());
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
				glowtone_spherical,
				glowtone_cylindrical,
				glowtone_environmentalStart,
				glowtone_environmentalEnd,
				glowtone_distanceStart,
				glowtone_distanceEnd
			) * glowtone_fog.a;
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

	public static final String GLOWTONE_EMISSIVE_PATCHED_NOTATION = """

		// GLOWTONE EMISSIVE PATCHED
	""";

	public static boolean containesEmissivePatchedNotation(String source) {
		return source.contains(GLOWTONE_EMISSIVE_PATCHED_NOTATION);
	}

	private EmissiveShaderPatcher() {}

	private static final Identifier SODIUM_TERRAIN_FRAGMENT =
		Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");

	public static boolean isLitShader(Identifier id) {
		return LIT_SHADERS.contains(id) || SODIUM_TERRAIN_FRAGMENT.equals(id);
	}

	private static final String DIFFUSE_ACCUM = "float lightAccum = min(1.0, (lightValue.x + lightValue.y) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);";
	private static final String DIFFUSE_FLAT = "float lightAccum = 1.0;";

	private static String flattenDiffuse(String source) {
		if (!GlowtoneConstants.GLOWTONE_NO_SHADING) return source;

		return source.replace(DIFFUSE_ACCUM, DIFFUSE_FLAT);
	}

	public static String patch(Identifier id, ShaderType type, String source) {
		if (!source.contains(MAIN)) return source;

		if (source.contains(SODIUM_VERTEX_MARKER) || source.contains(SODIUM_FOG_CALL)) return patchSodium(type, source) + GLOWTONE_EMISSIVE_PATCHED_NOTATION;

		source = flattenDiffuse(source);

		if (LIT_SHADERS.contains(id)) {
			if (type == ShaderType.VERTEX) {
				final String vertex = patchVertex(source);
				return (id.equals(TERRAIN) ? patchTerrainVertex(vertex) : vertex) + GLOWTONE_EMISSIVE_PATCHED_NOTATION;
			}

			final String patched = patchFragment(source);
			return (id.equals(TERRAIN) ? patchTerrainLines(patched) : patched) + GLOWTONE_EMISSIVE_PATCHED_NOTATION;
		}

		if (SELF_LIT_SHADERS.contains(id) && type == ShaderType.FRAGMENT) return patchSelfLitFragment(source) + GLOWTONE_EMISSIVE_PATCHED_NOTATION;

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

	private static final String SODIUM_FRAG_OUT_RELOCATED =
		"layout(location = 0) out vec4 fragColor;";

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
		flat out vec4 glowtone_Contact0;
		flat out vec4 glowtone_Contact1;
		flat out vec4 glowtone_Contact2;
		flat out vec4 glowtone_Contact3;

		""";

	private static final String SODIUM_WRITES = """

			glowtone_Height = position.y;
			glowtone_EdgeDist = a_GlowtoneEdge;
			glowtone_EdgeMask = a_GlowtoneEdgeMask;
			glowtone_Shade = _vert_color;
			glowtone_Contact0 = a_GlowtoneContact0;
			glowtone_Contact1 = a_GlowtoneContact1;
			glowtone_Contact2 = a_GlowtoneContact2;
			glowtone_Contact3 = a_GlowtoneContact3;
		""";

	private static String patchSodium(ShaderType type, String source) {
		final float shade = AmbientOcclusionOption.glowtoneActive()
			&& AmbientOcclusionOption.SHADER_CONTACT_SHADING ? OcclusionStrengthOption.strength() : 0F;
		final float highlight = EdgeHighlightOption.strength();
		final boolean occlusionView = aoDebug();

		final String emissive = type == ShaderType.VERTEX
			? patchSodiumEmissiveVertex(source)
			: patchSodiumEmissiveFragment(source);

		if (shade <= 0F && highlight <= 0F && !occlusionView) return emissive;

		return type == ShaderType.VERTEX
			? patchSodiumVertex(emissive)
			: patchSodiumFragment(emissive, shade, highlight, occlusionView);
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
		String source, float shade, float highlight, boolean occlusionView
	) {
		final String fragOut = source.contains(SODIUM_FRAG_OUT_RELOCATED)
			? SODIUM_FRAG_OUT_RELOCATED : SODIUM_FRAG_OUT;
		if (!source.contains(fragOut) || !source.contains(SODIUM_FOG_CALL)) return source;

		final String newline = System.lineSeparator();
		final boolean lines = highlight > 0F && (!occlusionView || edgeDebugColour());

		final StringBuilder header = new StringBuilder(EDGE_DATA_HEADER);
		final StringBuilder body = new StringBuilder();

		if (occlusionView || shade > 0F) {
			header.append(AO_HEADER.formatted(
				Float.toString(AO_RADIUS_UNITS),
				hex(GlowtoneContactRects.OCCUPIED_FLAG),
				hex(GlowtoneContactRects.GRID_FLAG)
			));
		}
		if (lines) header.append(sodiumEdgeHeader());

		if (occlusionView) body.append(sodiumOcclusionBody(shade, true)).append(newline);
		if (lines) body.append(sodiumHighlightBody(highlight)).append(newline);
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
			Float.toString(LIQUID_FACING_FADE),
			Float.toString(EDGE_WIDTH_UNITS),
			"0.5",
			edgeReturn,
			Float.toString(LIQUID_FLOOR),
			Float.toString(LIQUID_WHITEN),
			Float.toString(LIQUID_LIFT),
			Float.toString(LIQUID_OPACITY)
		);
	}

	private static String sodiumOcclusionBody(float strength, boolean view) {
		final String depth = Float.toString(view ? occlusionDepth() : strength);
		final String shaded = "dot(glowtone_Shade.rgb, vec3(0.2126, 0.7152, 0.0722))";
		if (!view) return "	%s.rgb *= max(1.0 - glowtone_ambientOcclusion() * %s, 0.0);".formatted(SODIUM_TARGET, depth);

		return """
				%s.rgb = vec3(
					%s %s
				);
			""".formatted(
				SODIUM_TARGET,
				shaded,
				AmbientOcclusionOption.vanillaActive() ? "" : " * max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0)"
			);
	}

	private static String sodiumHighlightBody(float edgeStrength) {
		final String isLiquid = "(glowtone_contactBits(glowtone_Contact3) & " + hex(GlowtoneContactRects.LIQUID_FLAG) + ") != 0u";
		final String liquid = edgeDebugColour()
			? SODIUM_TARGET + ".rgb = glowtone_edgeHighlight(" + SODIUM_TARGET + ".rgb, 1.0);"
			: SODIUM_TARGET + " = glowtone_liquidHighlight(" + SODIUM_TARGET + ", " + edgeStrength * LIQUID_STRENGTH_SCALE + ");";

		return """
				if (%s) {
					%s
				} else {
					%s.rgb = glowtone_edgeHighlight(%s.rgb, %s);
				}
			""".formatted(isLiquid, liquid, SODIUM_TARGET, SODIUM_TARGET, edgeStrength);
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
			glowtone_Contact0 = GlowtoneContact0;
			glowtone_Contact1 = GlowtoneContact1;
			glowtone_Contact2 = GlowtoneContact2;
			glowtone_Contact3 = GlowtoneContact3;
		""";

	private static String patchTerrainVertex(String source) {
		if (!source.contains(TERRAIN_IN_ANCHOR)
			|| !source.contains(TERRAIN_OUT_ANCHOR)
			|| !source.contains(TERRAIN_WRITE_ANCHOR)) {
			return source;
		}

		return source
			.replace(TERRAIN_IN_ANCHOR, TERRAIN_IN_ANCHOR + System.lineSeparator() + TERRAIN_ATTRIBUTES)
			.replace(TERRAIN_OUT_ANCHOR, TERRAIN_OUT_ANCHOR + System.lineSeparator() + TERRAIN_VARYINGS)
			.replace(TERRAIN_WRITE_ANCHOR, TERRAIN_WRITE_ANCHOR + System.lineSeparator() + TERRAIN_WRITES);
	}

	private static String patchVertex(String source) {
		if (!source.contains(SAMPLE_LIGHTMAP)) return source;
		return source.replace(SAMPLE_LIGHTMAP, GLOWTONE_SAMPLE_LIGHTMAP).replace(MAIN, VERTEX_HEADER + GLOWTONE_MAIN) + VERTEX_FOOTER;
	}

	private static String patchFragment(String source) {
		if (!source.contains(FRAG_COLOR_OUT)) return source;
		return source.replace(FRAG_COLOR_OUT, GLOWTONE_FRAG_COLOR_OUT).replace(MAIN, FRAGMENT_HEADER + GLOWTONE_MAIN) + FRAGMENT_FOOTER;
	}

	private static String patchTerrainLines(String source) {
		final float shade = AmbientOcclusionOption.glowtoneActive()
			&& AmbientOcclusionOption.SHADER_CONTACT_SHADING ? OcclusionStrengthOption.strength() : 0F;
		final float highlight = EdgeHighlightOption.strength();
		final float liquid = highlight * LIQUID_STRENGTH_SCALE;
		final boolean occlusionView = aoDebug();
		if (shade <= 0F && highlight <= 0F && !occlusionView) return source;

		String patched = source.replace(GLOWTONE_MAIN, EDGE_DATA_HEADER + GLOWTONE_MAIN);

		final boolean lines = highlight > 0F && (!occlusionView || edgeDebugColour());

		if (occlusionView) patched = patchAmbientOcclusion(patched, shade);
		if (lines) patched = patchEdgeHighlight(patched, highlight, liquid);
		if (shade > 0F && !occlusionView) patched = patchAmbientOcclusion(patched, shade);

		return deferFog(patched);
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
		if (AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING) return OcclusionStrengthOption.strength();
		if (AmbientOcclusionOption.vanillaActive()) return OcclusionStrengthOption.VANILLA_DEPTH * OcclusionStrengthOption.COVERAGE_REFERENCE;
		return 0F;
	}

	private static String patchAmbientOcclusion(String source, float strength) {
		final String radius = Float.toString(AO_RADIUS_UNITS);
		final boolean view = aoDebug();
		final String depth = Float.toString(view ? occlusionDepth() : strength);
		final String shaded = "dot(glowtone_Shade.rgb, vec3(0.2126, 0.7152, 0.0722))";

		final String occlusion = view
			? " fragColor.rgb = vec3(%s%s);".formatted(
				shaded, AmbientOcclusionOption.vanillaActive()
				? ""
				: " * max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0)"
			)
			: "	fragColor.rgb *= max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0);";
		final String body = occlusion + System.lineSeparator() + "	fragColor.rgb += vec3(glowtone_keepVaryings());";

		return source
			.replace(GLOWTONE_MAIN,
				AO_HEADER.formatted(radius,
					hex(GlowtoneContactRects.OCCUPIED_FLAG), hex(GlowtoneContactRects.GRID_FLAG))
					+ GLOWTONE_MAIN)
			.replace(EMISSIVE_WRITE, guarded(SHADED_TERRAIN_DEFINE, body) + EMISSIVE_WRITE);
	}

	private static String patchEdgeHighlight(String source, float edgeStrength, float liquidStrength) {
		final String edgeReturn = edgeDebugColour() ? EDGE_RETURN_DEBUG
			: EDGE_RETURN_NORMAL.formatted(
				Float.toString(EDGE_ANCHOR),
				Float.toString(EDGE_FLATTEN),
				Float.toString(EDGE_WHITEN)
			);
		final String header = EDGE_HEADER.formatted(
			Float.toString(LIQUID_FACING_FADE),
			Float.toString(EDGE_WIDTH_UNITS),
			"0.5",
			edgeReturn,
			Float.toString(LIQUID_FLOOR),
			Float.toString(LIQUID_WHITEN),
			Float.toString(LIQUID_LIFT),
			Float.toString(LIQUID_OPACITY)
		);
		final String newline = System.lineSeparator();
		final String isLiquid = "(glowtone_contactBits(glowtone_Contact3) & " + hex(GlowtoneContactRects.LIQUID_FLAG) + ") != 0u";
		final String foam = edgeDebugColour()
			? "fragColor.rgb = glowtone_edgeHighlight(fragColor.rgb, 1.0);"
			: "fragColor = glowtone_liquidHighlight(fragColor, " + liquidStrength + ");";

		final String opaque = "	if (" + isLiquid + ") {" + newline
			+ "		" + foam + newline
			+ "	} else {" + newline
			+ "		fragColor.rgb = glowtone_edgeHighlight(fragColor.rgb, "
			+ edgeStrength + ");" + newline
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
