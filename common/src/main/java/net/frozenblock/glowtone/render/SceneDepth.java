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

package net.frozenblock.glowtone.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;

@ClientOnly
public final class SceneDepth {
	public static final float NEAR = 0.05F;
	private static final float MIN_FAR = 64F;

	public static float far() {
		return Math.max(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16F, MIN_FAR);
	}

	public static boolean reversed() {
		final CompareOp test = DepthStencilState.DEFAULT.depthTest();
		return test == CompareOp.GREATER_THAN || test == CompareOp.GREATER_THAN_OR_EQUAL;
	}

	private SceneDepth() {}
}
