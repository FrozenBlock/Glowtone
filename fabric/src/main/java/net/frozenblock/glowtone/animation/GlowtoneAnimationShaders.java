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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class GlowtoneAnimationShaders {
	private static final Identifier TERRAIN_SHADER = Identifier.withDefaultNamespace("core/terrain");

	private static final String MAIN = "void main()";
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

	private GlowtoneAnimationShaders() {}

	@Nullable
	public static Map<Identifier, String> createNewTerrainShaders(Identifier id, ShaderType type, String source) {
		if (type != ShaderType.VERTEX || !source.contains(MAIN) || !source.contains(POSITION) || !id.equals(TERRAIN_SHADER)) return null;

		final Map<Identifier, String> map = new HashMap<>();
		map.put(
			createTerrainAnimationShaderId("foliage"),
			createAnimationShader(
				source,
				2D,
				4000D,
				"""
					xOffset = sin(animPos.x + (animPos.y / 2.0) + animTime) / 32.0;
					yOffset = cos(animPos.z + (animPos.y / 2.0) + animTime) / 32.0;
				"""
			)
		);
		map.put(
			createTerrainAnimationShaderId("fire"),
			createAnimationShader(
				source,
				2D,
				10000D,
				"""
					yOffset = (sin(animPos.x + (animPos.y / 4.0) + animTime) / 64.0) + (cos(animPos.z + (animPos.y / 2.0) + animTime) / 64.0);
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

	private static String patchVertex(String source, String animation) {
		String preMain = source.substring(0, source.indexOf(MAIN));
		preMain = preMain + animation;

		String postMain = source.substring(source.indexOf(MAIN));
		postMain = postMain.replaceFirst(POSITION, GlowtoneAnimationShaders.NEW_POSITION);

		return preMain + postMain;
	}

	private static String createAnimationShader(
		String source,
		double animationPosDividend,
		double animationProgressScale,
		String animation
	) {
		final String yes = patchVertex(source, createAnimation(animationPosDividend, animationProgressScale, animation));
		System.out.println(yes);
		return yes;
	}

	public static Identifier createTerrainAnimationShaderId(String name) {
		return GlowtoneConstants.id("core/terrain_" + name + "_animation");
	}
}
