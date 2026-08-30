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

package net.frozenblock.glowtone.light.occlusion;

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;

@ClientOnly
public final class OcclusionOverrideHelper {
	public static final float FULL_OCCLUDER = 0.2F;

	public static boolean any() {
		return BlockLightProperties.anyOcclusionScales();
	}

	public static boolean receives(BlockState state, boolean automatic) {
		if (!any()) return automatic;
		return BlockLightProperties.forBlockState(state).ambientOcclusion().self().orElse(automatic);
	}

	public static boolean casts(BlockState state, boolean automatic) {
		if (!any()) return automatic;
		return BlockLightProperties.forBlockState(state).ambientOcclusion().cast().orElse(automatic);
	}

	private OcclusionOverrideHelper() {}
}
