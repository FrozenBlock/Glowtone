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

package net.frozenblock.glowtone.light;

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;

@ClientOnly
public final class BlockLightPropertiesRenderer {
	private static final ThreadLocal<BlockLightProperties[]> RENDERED = ThreadLocal.withInitial(() -> new BlockLightProperties[]{BlockLightProperties.NONE});
	private static volatile boolean anyOcclusionScales;
	private static volatile boolean anyEmissive;
	private static volatile boolean anyFilterColors;
	private static volatile boolean anyLightColors;

	public static boolean anyOcclusionScales() {
		return anyOcclusionScales;
	}

	public static boolean anyEmissive() {
		return anyEmissive;
	}

	public static void beginBlock(BlockState state) {
		if (!anyEmissive) return;
		RENDERED.get()[0] = BlockLightProperties.forBlockState(state);
	}

	public static void endBlock() {
		if (!anyEmissive) return;
		RENDERED.get()[0] = BlockLightProperties.NONE;
	}

	public static int renderBrightness(int baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().brightness().orElse(baked) : baked;
	}

	public static boolean bloom(boolean baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().bloom().orElse(baked) : baked;
	}

	public static boolean anyFilterColors() {
		return anyFilterColors;
	}

	public static boolean anyLightColors() {
		return anyLightColors;
	}

	public static void setLoadedFeatures(boolean occlusionScales, boolean emissive, boolean filterColors, boolean lightColors) {
		anyOcclusionScales = occlusionScales;
		anyEmissive = emissive;
		anyFilterColors = filterColors;
		anyLightColors = lightColors;
	}

	private BlockLightPropertiesRenderer() {}
}
