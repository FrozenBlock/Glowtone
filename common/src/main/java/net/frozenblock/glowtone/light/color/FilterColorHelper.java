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

package net.frozenblock.glowtone.light.color;

import net.frozenblock.glowtone.light.BlockLightPropertiesRenderer;
import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

@ClientOnly
public final class FilterColorHelper {
	public static final int FULLY_TRANSMISSIVE = 0xFFF;
	public static final int MAX_CHANNEL = 0xF;
	private static final int NEUTRAL_TOLERANCE = 2;

	public static int filterFor(BlockState state) {
		// TODO: see if methods that *call* this can be optimized first-hand
		if (!BlockLightPropertiesRenderer.anyFilterColors()) return FULLY_TRANSMISSIVE;
		if (state.isAir()) return FULLY_TRANSMISSIVE;
		if (state.isSolidRender() && state.getLightDampening() >= LightEngine.MAX_LEVEL) return FULLY_TRANSMISSIVE;
		final int filter = BlockLightProperties.forBlockState(state).lightFilterColor().orElse(FULLY_TRANSMISSIVE);
		return nearNeutral(filter) ? FULLY_TRANSMISSIVE : filter;
	}

	public static boolean nearNeutral(int packed) {
		if (packed == FULLY_TRANSMISSIVE) return true;

		final int r = red(packed);
		final int g = green(packed);
		final int b = blue(packed);
		final int spread = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
		return spread <= NEUTRAL_TOLERANCE;
	}

	public static int red(int packed) {
		return (packed >> 8) & MAX_CHANNEL;
	}

	public static int green(int packed) {
		return (packed >> 4) & MAX_CHANNEL;
	}

	public static int blue(int packed) {
		return packed & MAX_CHANNEL;
	}

	public static boolean isNeutral(int packed) {
		return packed == FULLY_TRANSMISSIVE;
	}
}
