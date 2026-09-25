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

package net.frozenblock.glowtone.light.compat.lambdynamiclights;

import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.AbstractDynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.DynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.NoOpDynamicLightsCompat;
import net.frozenblock.lib.platform.ModLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.lighting.LightEngine;

@ClientOnly
public final class GlowtoneDynamicLights {
	public static final int STRIDE = 8;

	public static final double RADIUS = 7.75D;

	private static final AbstractDynamicLightsCompat INSTANCE = ModLoader.isModLoaded("lambdynlights")
		? new DynamicLightsCompat()
		: new NoOpDynamicLightsCompat();

	public static int levelAt(double deltaX, double deltaY, double deltaZ, int luminance) {
		final double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
		if (distanceSquared > RADIUS * RADIUS) return 0;

		return (int) (luminance - Math.sqrt(distanceSquared) / RADIUS * LightEngine.MAX_LEVEL);
	}

	public static void init() {
		INSTANCE.init();
	}

	public static AbstractDynamicLightsCompat get() {
		return INSTANCE;
	}
}
