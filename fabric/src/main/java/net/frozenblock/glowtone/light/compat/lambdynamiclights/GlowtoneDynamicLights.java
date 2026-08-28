package net.frozenblock.glowtone.light.compat.lambdynamiclights;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.AbstractDynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.DynamicLightsCompat;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.impl.NoOpDynamicLightsCompat;

@Environment(EnvType.CLIENT)
public final class GlowtoneDynamicLights {
	public static final int STRIDE = 5;
	private static final AbstractDynamicLightsCompat INSTANCE = FabricLoader.getInstance().isModLoaded("lambdynlights")
		? new DynamicLightsCompat()
		: new NoOpDynamicLightsCompat();

	public static void init() {
		INSTANCE.init();
	}

	public static AbstractDynamicLightsCompat get() {
		return INSTANCE;
	}
}
