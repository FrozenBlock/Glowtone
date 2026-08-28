package net.frozenblock.glowtone.light.compat.lambdynamiclights.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
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
