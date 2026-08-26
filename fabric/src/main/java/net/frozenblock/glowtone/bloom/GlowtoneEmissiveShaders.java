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
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.EdgeHighlightOption;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.OcclusionStrengthOption;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
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
		return GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.EDGE_HIGHLIGHTS);
	}

	public static boolean toggleEdgeDebugColour() {
		return GlowtoneDebugEntries.toggle(GlowtoneDebugEntries.EDGE_HIGHLIGHTS);
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

		float glowtone_kernelSpan(float glowtone_low, float glowtone_high) {
			return max(0.0, glowtone_kernelBelow(glowtone_high) - glowtone_kernelBelow(glowtone_low));
		}

		float glowtone_fromRects(uint glowtone_w[4], vec2 glowtone_here) {
			vec4 glowtone_rect[4];
			int glowtone_held = 0;

			for (uint glowtone_i = 0u; glowtone_i < 4u; glowtone_i++) {
				uint glowtone_at = glowtone_i * 24u;
				float glowtone_u0 = float(glowtone_field(glowtone_w, glowtone_at, 6u)) - 16.0;
				float glowtone_u1 = float(glowtone_field(glowtone_w, glowtone_at + 6u, 6u)) - 16.0;
				if (glowtone_u1 < glowtone_u0) continue;

				float glowtone_v0 = float(glowtone_field(glowtone_w, glowtone_at + 12u, 6u)) - 16.0;
				float glowtone_v1 = float(glowtone_field(glowtone_w, glowtone_at + 18u, 6u)) - 16.0;

				glowtone_rect[glowtone_held++] = vec4(glowtone_u0, glowtone_u1, glowtone_v0, glowtone_v1)
					- vec4(glowtone_here.x, glowtone_here.x, glowtone_here.y, glowtone_here.y);
			}

			float glowtone_covered = 0.0;

			for (int glowtone_subset = 1; glowtone_subset < (1 << glowtone_held); glowtone_subset++) {
				vec4 glowtone_box = vec4(-1e9, 1e9, -1e9, 1e9);
				int glowtone_members = 0;

				for (int glowtone_i = 0; glowtone_i < glowtone_held; glowtone_i++) {
					if ((glowtone_subset & (1 << glowtone_i)) == 0) continue;
					glowtone_box = vec4(
						max(glowtone_box.x, glowtone_rect[glowtone_i].x),
						min(glowtone_box.y, glowtone_rect[glowtone_i].y),
						max(glowtone_box.z, glowtone_rect[glowtone_i].z),
						min(glowtone_box.w, glowtone_rect[glowtone_i].w));
					glowtone_members++;
				}
				if (glowtone_box.y <= glowtone_box.x || glowtone_box.w <= glowtone_box.z) continue;

				float glowtone_term = glowtone_kernelSpan(glowtone_box.x, glowtone_box.y)
					* glowtone_kernelSpan(glowtone_box.z, glowtone_box.w);
				glowtone_covered += (glowtone_members & 1) == 1 ? glowtone_term : -glowtone_term;
			}

			return glowtone_covered * %s;
		}

		float glowtone_node(uint glowtone_w[4], int glowtone_i, int glowtone_j) {
			return float(glowtone_field(glowtone_w, uint(glowtone_i * 5 + glowtone_j) * 5u, 5u))
				/ 31.0 * %s;
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
			float glowtone_rim = min(1.0,
				glowtone_edgeRim(glowtone_edgeLit()) * glowtone_strength * glowtone_liquidFacing());
			float glowtone_level = dot(glowtone_colour.rgb, vec3(0.2126, 0.7152, 0.0722));
			vec3 glowtone_foam = mix(glowtone_colour.rgb, vec3(glowtone_level), %s) * %s;

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

	private static final String DIFFUSE_ACCUM = "float lightAccum = min(1.0, (lightValue.x + lightValue.y)"
		+ " * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);";
	private static final String DIFFUSE_FLAT = "float lightAccum = 1.0;";

	private static String flattenDiffuse(String source) {
		if (!GlowtoneConstants.GLOWTONE_NO_SHADING) return source;

		return source.replace(DIFFUSE_ACCUM, DIFFUSE_FLAT);
	}

	public static String patch(Identifier id, ShaderType type, String source) {
		if (!source.contains(MAIN)) return source;

		source = flattenDiffuse(source);

		if (LIT_SHADERS.contains(id)) {
			if (type == ShaderType.VERTEX) return patchVertex(source);

			final String patched = patchFragment(source);
			return id.equals(TERRAIN) ? patchTerrainLines(patched) : patched;
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

		final String apply = debugView()
			? "" : FOG_APPLY + System.lineSeparator() + "	";

		return faded
			.replace(APPLY_FOG, DEFER_FOG)
			.replace(GLOWTONE_MAIN, FOG_HEADER + GLOWTONE_MAIN)
			.replace(EMISSIVE_WRITE, apply + EMISSIVE_WRITE);
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

	private static String patchAmbientOcclusion(String source, float strength) {
		final String radius = Float.toString(AO_RADIUS_UNITS);
		final String ceiling = Float.toString(GlowtoneContactRects.COVERAGE_SCALE);
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
			.replace(GLOWTONE_MAIN,
				AO_HEADER.formatted(radius, ceiling, ceiling,
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
			Float.toString(LIQUID_WHITEN),
			Float.toString(LIQUID_LIFT),
			Float.toString(LIQUID_OPACITY)
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
