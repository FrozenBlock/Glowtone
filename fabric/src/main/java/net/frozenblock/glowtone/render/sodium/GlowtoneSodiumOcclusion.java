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

package net.frozenblock.glowtone.render.sodium;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.light.color.OcclusionOverrides;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class GlowtoneSodiumOcclusion {
	private static final ThreadLocal<boolean[]> RECEIVES = ThreadLocal.withInitial(() -> new boolean[]{true});

	public static void beginQuad(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
		final boolean[] held = RECEIVES.get();

		if (level == null || pos == null
			|| !OcclusionOverrides.any()
			|| !ChromaBaker.vanillaOcclusionActive()
		) {
			held[0] = true;
			return;
		}

		held[0] = OcclusionOverrides.receives(level.getBlockState(pos), true);
	}

	public static float scaleSelf(float ambientOcclusion) {
		return RECEIVES.get()[0] ? ambientOcclusion : 1F;
	}

	public static void scaleSelf(float[] ambientOcclusion) {
		if (RECEIVES.get()[0]) return;

		for (int vertex = 0; vertex < ambientOcclusion.length; vertex++) ambientOcclusion[vertex] = 1F;
	}

	private GlowtoneSodiumOcclusion() {}
}
