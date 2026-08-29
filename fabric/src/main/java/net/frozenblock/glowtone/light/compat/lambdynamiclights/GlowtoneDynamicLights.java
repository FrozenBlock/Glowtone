package net.frozenblock.glowtone.light.compat.lambdynamiclights;

import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.AbstractDynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.DynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.NoOpDynamicLightsCompat;
import net.frozenblock.lib.platform.ModLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public final class GlowtoneDynamicLights {
	public static final int STRIDE = 5;
	private static final AbstractDynamicLightsCompat INSTANCE = ModLoader.isModLoaded("lambdynlights")
		? new DynamicLightsCompat()
		: new NoOpDynamicLightsCompat();

	public static void init() {
		INSTANCE.init();
	}

	public static AbstractDynamicLightsCompat get() {
		return INSTANCE;
	}
}
