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

package net.frozenblock.glowtone.light.compat.lambdynamiclights.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

@ClientOnly
public interface AbstractDynamicLightsCompat {

	void init();

	int dynamicLightLevelAt(BlockPos pos);

	int luminanceOf(Object entity);

	int[] snapshot();

	boolean any();

	boolean anyWithin(int minBlockX, int minBlockY, int minBlockZ, int span);

	boolean matches(int[] published, int[] candidate, int count);

	int colorOf(Object source);

	int colorOfItemStack(ItemStack stack);
}
