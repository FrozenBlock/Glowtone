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

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Set;

@ClientOnly
public final class MaterialShaderNames {
	private static final List<String> RESERVED_PREFIXES = List.of("gl_", "glowtone_", "u_", "v_", "a_", "Glowtone");

	private static final Set<String> RESERVED = Set.of(
		"Sampler0", "Sampler1", "Sampler2", "Sampler3",
		"Position", "Color", "Normal", "UV0", "UV1", "UV2",
		"texCoord0", "texCoord1", "texCoord2", "fragColor", "vertexColor",
		"ModelViewMat", "ProjMat", "ModelOffset", "ChunkPosition", "ChunkVisibility",
		"CameraBlockPos", "CameraOffset", "ScreenSize", "GlintAlpha", "GameTime",
		"MenuBlurRadius", "UseRgss", "FogColor", "EMISSIVE",
		"Light0_Direction", "Light1_Direction",
		"attribute", "varying", "uniform", "sampler2D", "sampler2DArray",
		"void", "bool", "int", "uint", "float", "double",
		"vec2", "vec3", "vec4", "ivec2", "ivec3", "ivec4", "bvec2", "bvec3", "bvec4",
		"mat2", "mat3", "mat4", "struct", "layout", "flat", "smooth", "in", "out", "inout",
		"if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
		"return", "discard", "const", "true", "false", "precision", "highp", "mediump", "lowp"
	);

	@Nullable
	public static String snippetRejection(String source) {
		final String stripped = withoutCommentsOrStrings(source);

		int braces = 0;
		int parens = 0;
		int brackets = 0;
		for (int index = 0; index < stripped.length(); index++) {
			switch (stripped.charAt(index)) {
				case '{' -> braces++;
				case '}' -> braces--;
				case '(' -> parens++;
				case ')' -> parens--;
				case '[' -> brackets++;
				case ']' -> brackets--;
				default -> { }
			}

			if (braces < 0) return "it closes a brace it never opened, which would end the generated function early";
			if (parens < 0) return "it closes a parenthesis it never opened";
			if (brackets < 0) return "it closes a bracket it never opened";
		}

		if (braces != 0) return "it leaves " + braces + " brace(s) unclosed";
		if (parens != 0) return "it leaves " + parens + " parenthesis/es unclosed";
		if (brackets != 0) return "it leaves " + brackets + " bracket(s) unclosed";
		if (!stripped.contains("return")) return "it never returns a value";

		for (String directive : List.of("#version", "#extension", "void main")) {
			if (stripped.contains(directive)) {
				return "'" + directive + "' belongs to a whole shader, and a snippet is only a function body";
			}
		}

		return null;
	}

	private static String withoutCommentsOrStrings(String source) {
		final StringBuilder clean = new StringBuilder(source.length());
		boolean lineComment = false;
		boolean blockComment = false;

		for (int index = 0; index < source.length(); index++) {
			final char character = source.charAt(index);
			final char next = index + 1 < source.length() ? source.charAt(index + 1) : ' ';

			if (lineComment) {
				if (character == '\n') lineComment = false;
				continue;
			}
			if (blockComment) {
				if (character == '*' && next == '/') {
					blockComment = false;
					index++;
				}

				continue;
			}
			if (character == '/' && next == '/') {
				lineComment = true;
				index++;
				continue;
			}
			if (character == '/' && next == '*') {
				blockComment = true;
				index++;
				continue;
			}

			clean.append(character);
		}

		return clean.toString();
	}

	@Nullable
	public static String rejection(String name) {
		if (name.isEmpty()) return "it is empty";
		if (name.length() > 64) return "it is longer than 64 characters";

		final char first = name.charAt(0);
		if (first != '_' && !isLetter(first)) return "it must start with a letter or underscore";

		for (int index = 0; index < name.length(); index++) {
			final char character = name.charAt(index);
			if (character != '_' && !isLetter(character) && (character < '0' || character > '9')) {
				return "'" + character + "' is not allowed in a GLSL identifier";
			}
		}

		if (name.contains("__")) return "GLSL reserves identifiers containing a double underscore";
		if (RESERVED.contains(name)) return "it is a GLSL keyword or a name the patched shaders already use";

		for (String prefix : RESERVED_PREFIXES) {
			if (name.startsWith(prefix)) return "names starting with '" + prefix + "' are reserved";
		}

		return null;
	}

	private static boolean isLetter(char character) {
		return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
	}

	private MaterialShaderNames() {}
}
