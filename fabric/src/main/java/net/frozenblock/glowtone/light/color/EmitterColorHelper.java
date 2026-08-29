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

import net.frozenblock.glowtone.light.color.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;

@ClientOnly
public final class EmitterColorHelper {
	public static final int NO_COLOUR = -1;
	public static final int WHITE = 0xFFFFFF;

	public static int rgbFor(BlockState state) {
		return BlockLightProperties.forBlockState(state).lightColor().orElse(NO_COLOUR);
	}

	public static int rgbForOrWhite(BlockState state) {
		return BlockLightProperties.forBlockState(state).lightColor().orElse(WHITE);
	}
}
