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
