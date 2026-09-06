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
