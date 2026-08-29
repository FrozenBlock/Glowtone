package net.frozenblock.glowtone.light.compat.lambdynamiclights.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

@ClientOnly
public final class NoOpDynamicLightsCompat implements AbstractDynamicLightsCompat {
	private static final int[] NONE = new int[0];

	@Override
	public void init() {}

	@Override
	public int dynamicLightLevelAt(BlockPos pos) {
		return 0;
	}

	@Override
	public int luminanceOf(Object entity) {
		return 0;
	}

	@Override
	public int[] snapshot() {
		return NONE;
	}

	@Override
	public boolean any() {
		return false;
	}

	@Override
	public boolean anyWithin(int minBlockX, int minBlockY, int minBlockZ, int span) {
		return false;
	}

	@Override
	public boolean matches(int[] published, int[] candidate, int count) {
		return false;
	}

	@Override
	public int colorOf(Object source) {
		return 0;
	}

	@Override
	public int colorOfItemStack(ItemStack stack) {
		return 0;
	}
}
