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
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.frozenblock.glowtone.material.MaterialBlockTextures;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFeatures;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public final class SodiumEmissiveShaderPatcher {
	static final Identifier TERRAIN_FRAGMENT = Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque");
	private static final String QUAD_OFFSET_DECODE = "vec3(a_GlowtoneChroma.a - 0.5, a_GlowtonePivot.r - 0.5, a_GlowtoneSkyChroma.a - 0.5)";
	private static final String MODEL_VIEW_MATRIX = "u_ModelViewMatrix";

	static final String VERTEX_MARKER = "_vert_init();";
	private static final String COLOR_OUT = "out vec4 v_Color;";
	private static final String VERTEX_TAIL = "v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;";
	private static final String FRAG_OUT = "out vec4 fragColor;";
	static final String FOG_CALL = "fragColor = _linearFog(color,";
	private static final String FOG_STATEMENT =
		"fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);";
	private static final Set<String> FOG_SYMBOLS = Set.of(
		"total_fog_value", "fadeFactor", "v_FragDistance", "u_EnvironmentFog", "u_RenderFog", "u_FogColor"
	);
	private static final String TARGET = "color";

	private static final String EMISSIVE_ATTRIBUTES = """
		in vec4 a_GlowtoneFlags;

		flat out float glowtone_Emissive;

		""";

	private static final String EMISSIVE_WRITE_VERTEX = System.lineSeparator() + "    glowtone_Emissive = a_GlowtoneFlags.r;";
	private static final String EMISSIVE_FRAGMENT = """
		flat in float glowtone_Emissive;
		layout(location = 1) out vec4 glowtone_EmissiveColor;

		""";

	private static final String FRAG_OUT_RELOCATED = "layout(location = 0) out vec4 fragColor;";
	private static final String FOG_SURVIVAL = """
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

	private static final String ATTRIBUTES = """
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

	private static final String WRITES = """

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

	private static final String MATERIAL_VERTEX = """
		flat out int glowtone_Material;
		out vec3 glowtone_WorldPos;
		out vec3 glowtone_AbsPos;
		out vec2 glowtone_Light;
		out vec4 glowtone_ScreenProj;
		out float glowtone_GameTime;

		""";

	private static final String MATERIAL_WRITES =
		System.lineSeparator()
			+ "    glowtone_Material = " + BlockMaterialRenderer.GLSL_INDEX_DECODE.formatted("a_GlowtoneFlags") + ";" + System.lineSeparator()
			+ "    glowtone_WorldPos = position;" + System.lineSeparator()
			+ "    glowtone_AbsPos = position + vec3(CameraBlockPos) - CameraOffset;" + System.lineSeparator()
			+ "    glowtone_Light = vec2(0.0);" + System.lineSeparator()
			+ "    vec4 glowtone_halfClip = gl_Position * 0.5;" + System.lineSeparator()
			+ "    glowtone_ScreenProj = vec4(glowtone_halfClip.x + glowtone_halfClip.w,"
			+ " glowtone_halfClip.y + glowtone_halfClip.w, gl_Position.z, gl_Position.w);" + System.lineSeparator()
			+ "    glowtone_GameTime = float(u_CurrentTime % 1200000) / 1200000.0;";

	private static final String MATERIAL_FRAGMENT = """
		#define Sampler0 u_BlockTex

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

	private static String materialCall() {
		return "if (glowtone_Material != 0) color = " + MaterialShaderPatcher.DISPATCH
			+ "(u_BlockTex, glowtone_Material, color, v_TexCoord, glowtone_WorldPos, " + EmissiveShaderPatcher.blockPosArgument() + ","
			+ " " + EmissiveShaderPatcher.localPosArgument() + ", glowtone_faceNormal(), " + EmissiveShaderPatcher.screenProjArgument() + ", glowtone_Light, GameTime, 0);"
			+ System.lineSeparator() + "    ";
	}

	private static final String LIGHT_COORD = "_vert_tex_light_coord";

	private static final String POSITION_ANCHOR = "vec3 position =";

	static String patch(ShaderType type, String source) {
		final float shade = AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING ? OcclusionStrengthOption.strength() : 0F;
		final float option = EdgeHighlightOption.strength();
		final float highlight = EmissiveShaderPatcher.blockHighlight(option);
		final float liquid = option * GlowtonePackSettings.waterStrength();
		final boolean occlusionView = EmissiveShaderPatcher.aoDebug();

		final String emissive = type == ShaderType.VERTEX
			? patchEmissiveVertex(source)
			: patchEmissiveFragment(source);

		final String material = type == ShaderType.VERTEX
			? patchMaterialVertex(emissive)
			: patchMaterialFragment(emissive);

		if (!GlowtoneVertexFeatures.shaders().edges()) return material;
		if (shade <= 0F && highlight <= 0F && liquid <= 0F && !occlusionView) return material;

		return type == ShaderType.VERTEX
			? patchVertex(material)
			: patchFragment(material, shade, highlight, liquid, occlusionView);
	}

	// Sodium's vertex stage has no Globals block of its own, and the block position needs the camera.
	private static String displace(String source) {
		final int declaration = source.indexOf(POSITION_ANCHOR);
		final int end = declaration < 0 ? -1 : source.indexOf(';', declaration);
		if (end < 0) {
			EmissiveShaderPatcher.MATERIAL_LOGGER.error("Glowtone could not apply material vertex shaders: Sodium's position anchor is missing");
			return source;
		}

		final String displace = System.lineSeparator()
			+ "    int glowtone_vIndex = " + BlockMaterialRenderer.GLSL_INDEX_DECODE.formatted("a_GlowtoneFlags") + ";" + System.lineSeparator()
			+ "    if (glowtone_vIndex != 0) {" + System.lineSeparator()
			+ "        vec3 glowtone_vAbs = position + vec3(CameraBlockPos) - CameraOffset;" + System.lineSeparator()
			+ "        vec3 glowtone_vBlock = floor(glowtone_vAbs);" + System.lineSeparator()
			+ "        position += " + MaterialShaderPatcher.VERTEX_DISPATCH
			+ "(glowtone_vIndex, position, glowtone_vBlock, glowtone_vAbs - glowtone_vBlock, "
			+ EmissiveShaderPatcher.CAMERA_WORLD_POS + ", _vert_tex_diffuse_coord, vec2(0.0), GameTime, 0, "
			+ EmissiveShaderPatcher.quadOffsetArgument(true, QUAD_OFFSET_DECODE) + ", "
			+ EmissiveShaderPatcher.cameraAxisArgument(MODEL_VIEW_MATRIX, 0) + ", "
			+ EmissiveShaderPatcher.cameraAxisArgument(MODEL_VIEW_MATRIX, 1) + ");"
			+ System.lineSeparator() + "    }";

		return source.substring(0, end + 1) + displace + source.substring(end + 1);
	}

	private static String patchMaterialVertex(String source) {
		if (!MaterialShaderPatcher.anyVertex()
			|| !source.contains(COLOR_OUT)
			|| !source.contains(VERTEX_TAIL)
			|| source.contains("glowtone_Material")
		) {
			return source;
		}

		final boolean hasLight = source.contains(LIGHT_COORD);

		if ((MaterialShaderPatcher.anyVertex() || EmissiveShaderPatcher.usesBlockPos()) && !source.contains(EmissiveShaderPatcher.GAME_TIME_UNIFORM)) {
			source = source.replace(COLOR_OUT, EmissiveShaderPatcher.GLOBALS_BLOCK + COLOR_OUT);
		}

		if (MaterialShaderPatcher.anyVertex()) {
			final String rectangles = MaterialBlockTextures.declarations();
			source = displace(
				source.replace(COLOR_OUT, rectangles + MaterialShaderPatcher.generateVertexFunctions() + COLOR_OUT)
			);
		}

		final String patched = source
			.replace(COLOR_OUT, MATERIAL_VERTEX + COLOR_OUT)
			.replace(VERTEX_TAIL, VERTEX_TAIL + EmissiveShaderPatcher.withoutUnusedWrites(MATERIAL_WRITES));

		return hasLight
			? patched.replace("glowtone_Light = vec2(0.0);", "glowtone_Light = " + LIGHT_COORD + ";")
			: patched;
	}

	private static String patchMaterialFragment(String source) {
		if (!source.contains(FOG_CALL) || source.contains("GlowtoneMaterialTex0") || source.contains(MaterialBlockTextures.TABLE)) return source;

		final String keep = MaterialShaderPatcher.anySamplers()
			? "color.r += glowtone_keepSamplers();" + System.lineSeparator() + "    "
			: "";
		final String declared = source
			.replace(EmissiveShaderPatcher.MAIN, EmissiveShaderPatcher.samplerDeclarations() + EmissiveShaderPatcher.MAIN)
			.replace(FOG_CALL, keep + FOG_CALL);

		if (!MaterialShaderPatcher.anyFragment()) return declared;

		EmissiveShaderPatcher.MATERIAL_LOGGER.info("Glowtone injected {} material shaders into Sodium's terrain fragment shader", MaterialShaderPatcher.loaded().size());
		return declared
			.replace(EmissiveShaderPatcher.MAIN, EmissiveShaderPatcher.GLOBALS_BLOCK + MATERIAL_FRAGMENT + MaterialShaderPatcher.generateFunctions(true) + EmissiveShaderPatcher.MAIN)
			.replace(FOG_CALL, materialCall() + FOG_CALL);
	}

	private static String patchEmissiveVertex(String source) {
		if (!GlowtoneVertexFeatures.shaders().flags()
			|| !source.contains(COLOR_OUT)
			|| !source.contains(VERTEX_TAIL)
			|| source.contains("a_GlowtoneFlags")
		) {
			return source;
		}

		return source
			.replace(COLOR_OUT, EMISSIVE_ATTRIBUTES + COLOR_OUT)
			.replace(VERTEX_TAIL, VERTEX_TAIL + EMISSIVE_WRITE_VERTEX);
	}

	private static boolean reportedFog;

	private static String patchEmissiveFragment(String source) {
		if (!GlowtoneVertexFeatures.shaders().flags()
			|| !source.contains(FRAG_OUT)
			|| !source.contains(FOG_CALL)
			|| source.contains("glowtone_EmissiveColor")
		) {
			return source;
		}

		if (!hasFogSymbols(source)) {
			if (!reportedFog) {
				reportedFog = true;
				EmissiveShaderPatcher.MATERIAL_LOGGER.error("Glowtone could not write emissives from Sodium's terrain fragment shader: its fog statement no longer matches");
			}

			return source;
		}

		final String newline = System.lineSeparator();
		return source
			.replace(FRAG_OUT, FRAG_OUT_RELOCATED + newline + newline + EMISSIVE_FRAGMENT)
			.replace(EmissiveShaderPatcher.MAIN, FOG_SURVIVAL + EmissiveShaderPatcher.MAIN)
			.replace(
				FOG_STATEMENT,
				FOG_STATEMENT + newline
					+ "    glowtone_EmissiveColor = vec4("
					+ "color.rgb * glowtone_Emissive * glowtone_fogSurvival(), fragColor.a);"
			);
	}

	private static boolean hasFogSymbols(String source) {
		if (!source.contains(FOG_STATEMENT)) return false;
		for (String symbol : FOG_SYMBOLS) {
			if (!source.contains(symbol)) return false;
		}

		return true;
	}

	private static String patchVertex(String source) {
		if (!source.contains(COLOR_OUT)
			|| !source.contains(VERTEX_TAIL)
			|| source.contains("a_GlowtoneEdge")
		) {
			return source;
		}

		return source
			.replace(COLOR_OUT, ATTRIBUTES + COLOR_OUT)
			.replace(VERTEX_TAIL, VERTEX_TAIL + WRITES);
	}

	private static String patchFragment(
		String source, float shade, float highlight, float liquid, boolean occlusionView
	) {
		final String fragOut = source.contains(FRAG_OUT_RELOCATED)
			? FRAG_OUT_RELOCATED : FRAG_OUT;
		if (!source.contains(fragOut) || !source.contains(FOG_CALL)) return source;

		final String newline = System.lineSeparator();
		final boolean lines = (highlight > 0F || liquid > 0F) && (!occlusionView || EmissiveShaderPatcher.edgeDebugColour());

		final StringBuilder header = new StringBuilder(EmissiveShaderPatcher.EDGE_DATA_HEADER);
		final StringBuilder body = new StringBuilder();

		if (occlusionView || shade > 0F) header.append(EmissiveShaderPatcher.aoHeader());
		if (lines) header.append(edgeHeader());

		if (occlusionView) body.append(occlusionBody(shade, true)).append(newline);
		if (lines) body.append(highlightBody(highlight, liquid)).append(newline);
		if (shade > 0F && !occlusionView) body.append(occlusionBody(shade, false)).append(newline);

		body.append("    ").append(TARGET).append(".rgb += vec3(glowtone_keepVaryings());")
			.append(newline).append("    ");

		return source
			.replace(fragOut, fragOut + newline + newline + header)
			.replace(FOG_CALL, body + FOG_CALL);
	}

	private static String edgeHeader() {
		final String edgeReturn = EmissiveShaderPatcher.edgeDebugColour() ? EmissiveShaderPatcher.EDGE_RETURN_DEBUG
			: EmissiveShaderPatcher.EDGE_RETURN_NORMAL.formatted(
				Float.toString(EmissiveShaderPatcher.EDGE_ANCHOR),
				Float.toString(EmissiveShaderPatcher.EDGE_FLATTEN),
				Float.toString(EmissiveShaderPatcher.EDGE_WHITEN)
			).replace("vertexColor", "v_Color");

		return EmissiveShaderPatcher.EDGE_HEADER.formatted(
			Float.toString(GlowtonePackSettings.waterSize()),
			EmissiveShaderPatcher.edgeStyle(GlowtonePackSettings.waterStyle(), GlowtonePackSettings.waterSize()),
			Float.toString(GlowtonePackSettings.highlightSize()),
			EmissiveShaderPatcher.edgeStyle(GlowtonePackSettings.highlightStyle(), GlowtonePackSettings.highlightSize()),
			"0.5",
			EmissiveShaderPatcher.edgeCorners(GlowtonePackSettings.highlightCorners()),
			"glowtone_ViewDist",
			edgeReturn,
			Float.toString(GlowtonePackSettings.waterDistance()),
			Float.toString(GlowtonePackSettings.waterFloor()),
			Float.toString(GlowtonePackSettings.waterWhiten()),
			Float.toString(GlowtonePackSettings.waterLift()),
			Float.toString(GlowtonePackSettings.waterOpacity())
		);
	}

	private static String occlusionBody(float strength, boolean view) {
		final String depth = Float.toString(view ? EmissiveShaderPatcher.occlusionDepth() : strength);
		final String shaded = "dot(glowtone_Shade.rgb, vec3(0.2126, 0.7152, 0.0722))";
		if (!view) {
			return "    " + TARGET + ".rgb *= max(1.0 - glowtone_ambientOcclusion() * "
				+ depth + ", 0.0);";
		}
		return "    " + TARGET + ".rgb = vec3(" + shaded
			+ (AmbientOcclusionOption.vanillaActive() ? ""
				: " * max(1.0 - glowtone_ambientOcclusion() * " + depth + ", 0.0)")
			+ ");";
	}

	private static String highlightBody(float edgeStrength, float liquidStrength) {
		final String newline = System.lineSeparator();
		final String isLiquid = "(glowtone_contactBits(glowtone_Contact3) & "
			+ EmissiveShaderPatcher.hex(GlowtoneContactRects.LIQUID_FLAG) + ") != 0u";
		final String liquid = EmissiveShaderPatcher.edgeDebugColour()
			? TARGET + ".rgb = glowtone_edgeHighlight(" + TARGET + ".rgb, 1.0);"
			: TARGET + " = glowtone_liquidHighlight(" + TARGET + ", " + liquidStrength + ");";

		return "    if (" + isLiquid + ") {" + newline
			+ "        " + liquid + newline
			+ "    } else {" + newline
			+ "        " + TARGET + ".rgb = glowtone_edgeHighlight(" + TARGET + ".rgb, " + edgeStrength + ");" + newline
			+ "    }";
	}

	private SodiumEmissiveShaderPatcher() {}
}
