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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	private static volatile boolean anySamplers;
	private static volatile String sampledFunctions = "";
	private static volatile String atlasFunctions = "";
	private static volatile String vertexFunctions = "";

	public static void apply(List<Loaded> shaders) {
		loaded = List.copyOf(shaders);
		anyFragment = loaded.stream().anyMatch(entry -> entry.fragmentSource() != null);
		anyVertex = loaded.stream().anyMatch(entry -> entry.vertexSource() != null);
		anySamplers = loaded.stream().anyMatch(entry -> !entry.slots().isEmpty());
		sampledFunctions = buildFunctions(true);
		atlasFunctions = buildFunctions(false);
		vertexFunctions = buildVertexFunctions();
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

	public static boolean anySamplers() {
		return anySamplers;
	}

	public static boolean usesInput(String name) {
		for (Loaded entry : loaded) {
			if (entry.fragmentSource() != null && entry.fragmentSource().contains(name)) return true;
			if (entry.vertexSource() != null && entry.vertexSource().contains(name)) return true;
		}

		return false;
	}

	private static String parameterDeclarations(Loaded entry) {
		final StringBuilder text = new StringBuilder();
		entry.shader().parameters().keySet().stream().sorted()
			.forEach(name -> text.append(", float ").append(name));
		entry.blockTextures().stream().sorted()
			.forEach(name -> text.append(", vec4 ").append(name));

		return text.toString();
	}

	private static String parameterArguments(Loaded entry) {
		final StringBuilder text = new StringBuilder();
		entry.shader().parameters().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(parameter -> text.append(", float(").append(parameter.getValue()).append(')'));
		entry.blockTextures().stream().sorted().forEach(name -> {
			final int index = MaterialBlockTextures.indexOf(name);
			if (index < 0) {
				text.append(", vec4(0.0)");
			} else {
				text.append(", ").append(MaterialBlockTextures.ARRAY).append('[').append(index).append(']');
			}
		});

		return text.toString();
	}

	private static void appendConstants(StringBuilder builder, Loaded entry, boolean define) {
		entry.shader().constants().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(constant -> {
				if (define) {
					builder.append("#define ").append(constant.getKey()).append(" (").append(constant.getValue()).append(")\n");
				} else {
					builder.append("#undef ").append(constant.getKey()).append("\n");
				}
			});
	}

	private static String bodyKey(String source, Loaded entry) {
		final StringBuilder key = new StringBuilder(source);
		key.append(' ').append(String.join(",", entry.slots().keySet())).append(' ');
		key.append(String.join(",", entry.shader().parameters().keySet().stream().sorted().toList())).append(' ');
		key.append(String.join(",", entry.blockTextures().stream().sorted().toList())).append(' ');
		entry.shader().constants().entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(constant -> key.append(constant.getKey()).append('=').append(constant.getValue()).append(';'));

		return key.toString();
	}

	public static List<String> describe() {
		final List<String> lines = new ArrayList<>(loaded.size());
		final Map<String, Integer> fragmentOwners = new HashMap<>();
		final Map<String, Integer> vertexOwners = new HashMap<>();

		for (int i = 0; i < loaded.size(); i++) {
			final Loaded entry = loaded.get(i);
			final int index = i + 1;
			final StringBuilder line = new StringBuilder("  [")
				.append(index).append("] ").append(entry.materialId());

			final List<String> stages = new ArrayList<>(2);
			if (entry.fragmentSource() != null) {
				final int owner = fragmentOwners.computeIfAbsent(bodyKey(entry.fragmentSource(), entry), key -> index);
				stages.add(owner == index ? "fragment" : "fragment (shares program " + owner + ")");
			}
			if (entry.vertexSource() != null) {
				final int owner = vertexOwners.computeIfAbsent(bodyKey(entry.vertexSource(), entry), key -> index);
				stages.add(owner == index ? "vertex" : "vertex (shares program " + owner + ")");
			}

			line.append(" - ").append(String.join(", ", stages));
			if (!entry.slots().isEmpty()) {
				line.append("; textures ").append(entry.slots().keySet())
					.append(" in slots ").append(entry.slots().values());
			}

			lines.add(line.toString());
		}

		return lines;
	}

	public static String generateFunctions(boolean withSamplers) {
		return withSamplers ? sampledFunctions : atlasFunctions;
	}

	public static String generateVertexFunctions() {
		return vertexFunctions;
	}

	private static String buildFunctions(boolean withSamplers) {
		if (!anyFragment) return "";

		final StringBuilder builder = new StringBuilder();
		final Map<String, Integer> shared = new HashMap<>();
		final int[] body = new int[loaded.size() + 1];
		final Map<Integer, List<Integer>> bodyUsers = new LinkedHashMap<>();

		for (int i = 0; i < loaded.size(); i++) {
			final Loaded entry = loaded.get(i);
			if (entry.fragmentSource() == null) continue;

			final Integer alreadyEmitted = shared.putIfAbsent(bodyKey(entry.fragmentSource(), entry), i + 1);
			body[i + 1] = alreadyEmitted != null ? alreadyEmitted : i + 1;
			bodyUsers.computeIfAbsent(body[i + 1], owner -> new ArrayList<>()).add(i + 1);
		}

		for (Map.Entry<Integer, List<Integer>> owned : bodyUsers.entrySet()) {
			final int owner = owned.getKey();
			final Loaded entry = loaded.get(owner - 1);

			appendConstants(builder, entry, true);
			builder.append("\nvec4 ").append(DISPATCH).append('_').append(owner).append("(sampler2D ").append(ATLAS);
			entry.slots().keySet().forEach(name -> builder.append(", sampler2D ").append(name));
			builder.append(", ").append(FRAGMENT_PARAMS).append(parameterDeclarations(entry)).append(") {\n");
			builder.append(entry.fragmentSource()).append("\n}\n");
			appendConstants(builder, entry, false);
		}

		builder.append("\nvec4 ").append(DISPATCH)
			.append("(sampler2D ").append(ATLAS).append(", int glowtone_index, ").append(FRAGMENT_PARAMS).append(") {\n")
			.append("\tswitch (glowtone_index) {\n");

		for (int i = 0; i < loaded.size(); i++) {
			if (loaded.get(i).fragmentSource() == null) continue;

			builder.append("\t\tcase ").append(i + 1).append(": return ").append(DISPATCH).append('_').append(body[i + 1]).append('(').append(ATLAS);
			loaded.get(i).slots().values().forEach(slot -> builder.append(", ").append(withSamplers ? MaterialSamplers.name(slot) : ATLAS));
			builder.append(", ").append(FRAGMENT_ARGS).append(parameterArguments(loaded.get(i))).append(");\n");
		}

		builder.append("\t}\n\treturn ").append(COLOR).append(";\n}\n\n");
		return builder.toString();
	}

	private static String buildVertexFunctions() {
		if (!anyVertex) return "";

		final StringBuilder builder = new StringBuilder();
		final Map<String, Integer> shared = new HashMap<>();
		final int[] body = new int[loaded.size() + 1];
		final Map<Integer, List<Integer>> bodyUsers = new LinkedHashMap<>();

		for (int i = 0; i < loaded.size(); i++) {
			final Loaded entry = loaded.get(i);
			if (entry.vertexSource() == null) continue;

			final Integer alreadyEmitted = shared.putIfAbsent(bodyKey(entry.vertexSource(), entry), i + 1);
			body[i + 1] = alreadyEmitted != null ? alreadyEmitted : i + 1;
			bodyUsers.computeIfAbsent(body[i + 1], owner -> new ArrayList<>()).add(i + 1);
		}

		for (Map.Entry<Integer, List<Integer>> owned : bodyUsers.entrySet()) {
			final int owner = owned.getKey();
			final Loaded entry = loaded.get(owner - 1);

			appendConstants(builder, entry, true);
			builder.append("\nvec3 ").append(VERTEX_DISPATCH).append('_').append(owner).append('(').append(VERTEX_PARAMS)
				.append(parameterDeclarations(entry)).append(") {\n")
				.append(entry.vertexSource()).append("\n}\n");
			appendConstants(builder, entry, false);
		}

		builder.append("\nvec3 ").append(VERTEX_DISPATCH).append("(int glowtone_index, ").append(VERTEX_PARAMS).append(") {\n")
			.append("\tswitch (glowtone_index) {\n");

		for (int i = 0; i < loaded.size(); i++) {
			if (loaded.get(i).vertexSource() == null) continue;

			builder.append("\t\tcase ").append(i + 1).append(": return ").append(VERTEX_DISPATCH).append('_').append(body[i + 1]).append('(').append(VERTEX_ARGS)
				.append(parameterArguments(loaded.get(i))).append(");\n");
		}

		builder.append("\t}\n\treturn vec3(0.0);\n}\n\n");
		return builder.toString();
	}

	public record Loaded(
		Identifier materialId,
		MaterialShader shader,
		@Nullable String fragmentSource,
		@Nullable String vertexSource,
		Map<String, Integer> slots,
		List<String> blockTextures
	) {
		public Loaded(
			Identifier materialId, MaterialShader shader,
			@Nullable String fragmentSource, @Nullable String vertexSource, Map<String, Integer> slots
		) {
			this(materialId, shader, fragmentSource, vertexSource, slots, List.of());
		}
	}

	private MaterialShaderPatcher() {}
}
