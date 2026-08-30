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

package net.frozenblock.glowtone.material;

import net.frozenblock.glowtone.material.data.MaterialShader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map;

@ClientOnly
// TODO: make patch code not unreadable
public final class MaterialShaderPatcher {
	public static final String ATLAS = "glowtone_Atlas";
	public static final String COLOR = "glowtone_Color";
	public static final String UV = "glowtone_Uv";
	public static final String WORLD_POS = "glowtone_WorldPos";
	public static final String BLOCK_POS = "glowtone_BlockPos";
	public static final String LOCAL_POS = "glowtone_LocalPos";
	public static final String NORMAL = "glowtone_Normal";
	public static final String SCREEN_PROJ = "glowtone_ScreenProj";
	public static final String LIGHT = "glowtone_Light";
	public static final String GAME_TIME = "glowtone_GameTime";
	public static final String CONTEXT = "glowtone_Context";
	public static final String POSITION = "glowtone_Position";
	public static final String DISPATCH = "glowtone_materialShader";
	public static final String VERTEX_DISPATCH = "glowtone_materialVertex";

	private static final String FRAGMENT_PARAMS = "vec4 %s, vec2 %s, vec3 %s, vec3 %s, vec3 %s, vec3 %s, vec4 %s, vec2 %s, float %s, int %s"
		.formatted(COLOR, UV, WORLD_POS, BLOCK_POS, LOCAL_POS, NORMAL, SCREEN_PROJ, LIGHT, GAME_TIME, CONTEXT);

	private static final String FRAGMENT_ARGS = "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s"
		.formatted(COLOR, UV, WORLD_POS, BLOCK_POS, LOCAL_POS, NORMAL, SCREEN_PROJ, LIGHT, GAME_TIME, CONTEXT);

	// No normal: terrain carries no normal attribute.
	private static final String VERTEX_PARAMS = "vec3 %s, vec3 %s, vec3 %s, vec2 %s, float %s, int %s"
		.formatted(POSITION, BLOCK_POS, LOCAL_POS, LIGHT, GAME_TIME, CONTEXT);

	private static final String VERTEX_ARGS = "%s, %s, %s, %s, %s, %s"
		.formatted(POSITION, BLOCK_POS, LOCAL_POS, LIGHT, GAME_TIME, CONTEXT);

	private static volatile List<Loaded> loaded = List.of();
	private static volatile boolean anyFragment;
	private static volatile boolean anyVertex;

	public static void apply(List<Loaded> shaders) {
		loaded = List.copyOf(shaders);
		anyFragment = loaded.stream().anyMatch(entry -> entry.fragmentSource() != null);
		anyVertex = loaded.stream().anyMatch(entry -> entry.vertexSource() != null);
	}

	public static List<Loaded> loaded() {
		return loaded;
	}

	public static boolean any() {
		return !loaded.isEmpty();
	}

	public static boolean anyFragment() {
		return anyFragment;
	}

	public static boolean anyVertex() {
		return anyVertex;
	}

	private static void appendConstants(StringBuilder builder, Loaded entry, boolean define) {
		entry.shader().constants().forEach((name, value) -> {
			if (define) {
				builder.append("#define ").append(name).append(" (").append(value).append(")\n");
			} else {
				builder.append("#undef ").append(name).append("\n");
			}
		});
	}

	public static String generateFunctions(boolean withSamplers) {
		if (!anyFragment) return "";

		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < loaded.size(); i++) {
			final Loaded entry = loaded.get(i);
			if (entry.fragmentSource() == null) continue;

			appendConstants(builder, entry, true);
			builder.append("\nvec4 ").append(DISPATCH).append('_').append(i + 1).append("(sampler2D ").append(ATLAS);
			entry.slots().keySet().forEach(name -> builder.append(", sampler2D ").append(name));
			builder.append(", ").append(FRAGMENT_PARAMS).append(") {\n");
			builder.append(entry.fragmentSource()).append("\n}\n");
			appendConstants(builder, entry, false);
		}

		builder.append("\nvec4 ").append(DISPATCH)
			.append("(sampler2D ").append(ATLAS).append(", int glowtone_index, ").append(FRAGMENT_PARAMS).append(") {\n")
			.append("\tswitch (glowtone_index) {\n");

		for (int i = 0; i < loaded.size(); i++) {
			if (loaded.get(i).fragmentSource() == null) continue;

			builder.append("\t\tcase ").append(i + 1).append(": return ").append(DISPATCH).append('_').append(i + 1).append('(').append(ATLAS);
			loaded.get(i).slots().values().forEach(slot -> builder.append(", ").append(withSamplers ? MaterialSamplers.name(slot) : ATLAS));
			builder.append(", ").append(FRAGMENT_ARGS).append(");\n");
		}

		builder.append("\t}\n\treturn ").append(COLOR).append(";\n}\n\n");
		return builder.toString();
	}

	public static String generateVertexFunctions() {
		if (!anyVertex) return "";

		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < loaded.size(); i++) {
			final Loaded entry = loaded.get(i);
			if (entry.vertexSource() == null) continue;

			appendConstants(builder, entry, true);
			builder.append("\nvec3 ").append(VERTEX_DISPATCH).append('_').append(i + 1).append('(').append(VERTEX_PARAMS).append(") {\n")
				.append(entry.vertexSource()).append("\n}\n");
			appendConstants(builder, entry, false);
		}

		builder.append("\nvec3 ").append(VERTEX_DISPATCH).append("(int glowtone_index, ").append(VERTEX_PARAMS).append(") {\n")
			.append("\tswitch (glowtone_index) {\n");

		for (int i = 0; i < loaded.size(); i++) {
			if (loaded.get(i).vertexSource() == null) continue;

			builder.append("\t\tcase ").append(i + 1).append(": return ").append(VERTEX_DISPATCH).append('_').append(i + 1).append('(').append(VERTEX_ARGS).append(");\n");
		}

		builder.append("\t}\n\treturn vec3(0.0);\n}\n\n");
		return builder.toString();
	}

	public record Loaded(
		Identifier materialId,
		MaterialShader shader,
		@Nullable String fragmentSource,
		@Nullable String vertexSource,
		Map<String, Integer> slots
	) {}

	private MaterialShaderPatcher() {}
}
