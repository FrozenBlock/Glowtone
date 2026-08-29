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

package net.frozenblock.glowtone.animation;

import com.mojang.blaze3d.shaders.ShaderType;
import java.util.HashMap;
import java.util.Map;
import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

// TODO: sodium
@ClientOnly
public final class AnimationShaderPatcher {
	private static final Identifier TERRAIN_SHADER = Identifier.withDefaultNamespace("core/terrain");

	private static final String MAIN = "void main()";
	private static final String GLOWTONE_MAIN = "void glowtone_main()";
	private static final String UNIFORM = "uniform ";
	private static final String POSITION = "Position";
	private static final String GAME_TIME = "GameTime";
	private static final String NEW_POSITION = "(%s + glowtone_animation(%s, %s))".formatted(POSITION, POSITION, GAME_TIME);

	private static final String IN_GLOWTONE_ANIMATION_INFO = """
		in vec4 GlowtoneAnimationInfo;    // animation id, position scale, and animation time scale";

		""";
	private static final String GLOWTONE_ANIMATION_START = """
		vec3 glowtone_animation(vec3 pos, float gameTime) {
			float animationId = GlowtoneAnimationInfo.x;
			if (animationId == 0.0) return vec3(0.0, 0.0, 0.0);

			float xOffset = 0.0;
		    float yOffset = 0.0;
		    float zOffset = 0.0;

			vec3 animPos = pos * 3.14159265359 * GlowtoneAnimationInfo.y;
		    float animTime = gameTime * GlowtoneAnimationInfo.z;

		""";
	private static final String GLOWTONE_ANIMATION_BLOCK_TEMPLATE = """
			if (animationId == %s) {
		%s
				return vec3(xOffset, yOffset, zOffset);
			}
		""";
	private static final String GLOWTONE_ANIMATION_END = """
			return vec3(0.0, 0.0, 0.0);
		}

		""";

	@Nullable
	public static String patchTerrainShader(Identifier id, ShaderType type, String source) {
		final String main = EmissiveShaderPatcher.containesEmissivePatchedNotation(source) ? GLOWTONE_MAIN : MAIN;
		if (type != ShaderType.VERTEX || !source.contains(main) || !source.contains(POSITION) || !id.equals(TERRAIN_SHADER)) return null;

		injectUniforms: {
			final String preTerrainSource = source.substring(0, source.lastIndexOf("#version"));
			String terrainOnlySource = source.substring(source.lastIndexOf("#version"));

			String preUniform = terrainOnlySource.substring(0, terrainOnlySource.indexOf(UNIFORM));
			preUniform = preUniform + IN_GLOWTONE_ANIMATION_INFO;
			final String postUniform = terrainOnlySource.substring(terrainOnlySource.indexOf(UNIFORM));
			terrainOnlySource = preUniform + postUniform;

			source = preTerrainSource + terrainOnlySource;
		}

		final Map<Integer, String> animations = new HashMap<>();
		animations.put(
			1,
			"""
			xOffset = sin(animPos.x + (animPos.y / 2.0) + animTime) / 64.0;
			yOffset = (sin(animPos.y + ((animPos.x + animPos.z) / 4.0) + animTime) / 128.0) + (cos(((animPos.x + animPos.z) / 2.0) + (animPos.y / 4.0) + animTime * 2.0) / 128.0);
			zOffset = cos(animPos.z + (animPos.y / 2.0) + animTime) / 64.0;
	"""
		);
		animations.put(
			2,
			"""
			float additionalCosA = (cos(animPos.z + (animPos.y / 2.0) + animTime * 2.5) + 0.5);
			yOffset = (sin(((animPos.x + animPos.z) / 2.0) + animTime) / 182.0) + ((sin(((animPos.x - animPos.z) / 4.0) + (animPos.y / 4.0) + animTime * 1.65) / 182.0) * additionalCosA);
	"""
		);
		animations.put(
			3,
			"""
			float wigglerA = sin((((animPos.x + animPos.z) / 4.0) + animPos.y) + animTime * 0.9);
			wigglerA = (wigglerA * 8.0) - 7.0;
			if (wigglerA > 1.0) {
				wigglerA = 1.0;
			} else if (wigglerA < 0.0) {
				wigglerA = 0.0;
			}

			float wigglerB = cos((((animPos.x + animPos.z) / 2.0) + animPos.y * 2.0) + animTime * 0.8);
			wigglerB = (wigglerB * 8.0) - 7.0;
			if (wigglerB > 1.0) {
				wigglerB = 1.0;
			} else if (wigglerB < 0.0) {
				wigglerB = 0.0;
			}

			float wiggler = wigglerA * wigglerB;

			xOffset = cos(animPos.x + (animPos.y / 2.0) + animTime + (wiggler * 6.0)) / 128.0;
			yOffset = (sin(animPos.y + ((animPos.x + animPos.z) / 4.0) + animTime) / 256.0) + (cos(((animPos.x + animPos.z) / 2.0) + (animPos.y / 4.0) + animTime * 2.0) / 256.0);
			zOffset = sin(animPos.z + (animPos.y / 2.0) + animTime + (wiggler * 6.0)) / 128.0;
	"""
		);
		animations.put(
			4,
			"""
			float wigglerA = sin((animPos.x / 2.0 + animPos.z / 2.0 + animPos.y / 2.0) + animTime * 0.5);
			wigglerA = ((wigglerA * 8.0) / 8.0) + 0.1;
			if (wigglerA > 1.0) {
				wigglerA = 1.0;
			} else if (wigglerA < 0.0) {
				wigglerA = 0.0;
			}

			float wiggler = wigglerA * wigglerA;
			wiggler = wiggler * wiggler;

			xOffset = cos(animPos.x + (animPos.y / 2.0) + animTime) / 128.0;
			yOffset = wiggler / 48.0;
			zOffset = sin(animPos.z + (animPos.y / 2.0) + animTime) / 128.0;
	"""
		);

		return patchVertex(main, source, animations);
	}

	private static String patchVertex(String main, String source, Map<Integer, String> animations) {
		final StringBuilder animationMethod = new StringBuilder(GLOWTONE_ANIMATION_START);
		animations.forEach((id, animation) -> animationMethod.append(GLOWTONE_ANIMATION_BLOCK_TEMPLATE.formatted(id, animation)));
		animationMethod.append(GLOWTONE_ANIMATION_END);

		String preMain = source.substring(0, source.indexOf(main));
		preMain = preMain + animationMethod;

		String postMain = source.substring(source.indexOf(main));
		postMain = postMain.replaceFirst(POSITION, NEW_POSITION);

		return preMain + postMain;
	}

	private AnimationShaderPatcher() {}
}
