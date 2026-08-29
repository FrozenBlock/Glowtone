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
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

// TODO: sodium
@ClientOnly
public final class GlowtoneAnimationShaders {
	private static final Identifier TERRAIN_SHADER = Identifier.withDefaultNamespace("core/terrain");

	private static final String MAIN = "void main()";
	private static final String GLOWTONE_MAIN = "void glowtone_main()";
	private static final String POSITION = "Position";
	private static final String GAME_TIME = "GameTime";
	private static final String NEW_POSITION = "(%s + glowtone_animation(%s, %s))".formatted(POSITION, POSITION, GAME_TIME);

	private static final String PI = "3.14159265359";
	private static final String GLOWTONE_ANIMATION_START = """
		vec3 glowtone_animation(vec3 pos, float gameTime) {
			float xOffset = 0.0;
		    float yOffset = 0.0;
		    float zOffset = 0.0;

			vec3 animPos = pos / %f * %s;
		    float animTime = gameTime * %f;

		""";
	private static final String GLOWTONE_ANIMATION_END = """

		    return vec3(xOffset, yOffset, zOffset);
		}

		""";

	@Nullable
	public static Map<Identifier, String> createNewTerrainShaders(Identifier id, ShaderType type, String source) {
		final String main = EmissiveShaderPatcher.containesEmissivePatchedNotation(source) ? GLOWTONE_MAIN : MAIN;
		if (type != ShaderType.VERTEX || !source.contains(main) || !source.contains(POSITION) || !id.equals(TERRAIN_SHADER)) return null;

		final Map<Identifier, String> map = new HashMap<>();
		map.put(
			createTerrainAnimationShaderId("foliage"),
			createAnimationShader(
				main,
				source,
				2D,
				2000D,
				"""
					xOffset = sin(animPos.x + (animPos.y / 2.0) + animTime) / 64.0;
					yOffset = (sin(animPos.y + ((animPos.x + animPos.z) / 4.0) + animTime) / 128.0) + (cos(((animPos.x + animPos.z) / 2.0) + (animPos.y / 4.0) + animTime * 2.0) / 128.0);
					zOffset = cos(animPos.z + (animPos.y / 2.0) + animTime) / 64.0;
				"""
			)
		);
		map.put(
			createTerrainAnimationShaderId("fire"),
			createAnimationShader(
				main,
				source,
				2D,
				20000D,
				"""
					float additionalCosA = (cos(animPos.z + (animPos.y / 2.0) + animTime * 2.5) + 0.5);
					yOffset = (sin(((animPos.x + animPos.z) / 2.0) + animTime) / 182.0) + ((sin(((animPos.x - animPos.z) / 4.0) + (animPos.y / 4.0) + animTime * 1.65) / 182.0) * additionalCosA);
				"""
			)
		);

		return map;
	}

	private static String createAnimation(
		double animationPosDividend,
		double animationProgressScale,
		String animation
	) {
		final String start = GLOWTONE_ANIMATION_START.formatted(animationPosDividend, PI, animationProgressScale);
		return start + animation + GLOWTONE_ANIMATION_END;
	}

	private static String patchVertex(String main, String source, String animation) {
		String preMain = source.substring(0, source.indexOf(main));
		preMain = preMain + animation;

		String postMain = source.substring(source.indexOf(main));
		postMain = postMain.replaceFirst(POSITION, GlowtoneAnimationShaders.NEW_POSITION);

		return preMain + postMain;
	}

	private static String createAnimationShader(
		String main,
		String source,
		double animationPosDividend,
		double animationProgressScale,
		String animation
	) {
		return patchVertex(main, source, createAnimation(animationPosDividend, animationProgressScale, animation));
	}

	public static Identifier createTerrainAnimationShaderId(String name) {
		return GlowtoneConstants.id("core/terrain_" + name + "_animation");
	}

	private GlowtoneAnimationShaders() {}
}
