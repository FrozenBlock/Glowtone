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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class GlowtoneEntityLight {
	private static final int LEVEL_SCALE = 16;
	private static final int MAX_SMOOTH = 240;

	private static final BlockPos.MutableBlockPos SCRATCH = new BlockPos.MutableBlockPos();

	private GlowtoneEntityLight() {}

	public static int worldLightAt(double x, double y, double z, int fallback) {
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientLevel level = minecraft == null ? null : minecraft.level;
		if (level == null) return fallback;

		final BlockPos.MutableBlockPos pos = SCRATCH.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
		if (!level.hasChunkAt(pos)) return fallback;
		return LightCoordsUtil.getLightCoords(level, pos);
	}

	public static int smooth(double x, double y, double z, int lightCoords) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || !minecraft.options.ambientOcclusion().get()) return lightCoords;

		final ClientLevel level = minecraft.level;
		if (level == null) return lightCoords;

		final double gridX = x - 0.5D;
		final double gridY = y - 0.5D;
		final double gridZ = z - 0.5D;

		final int baseX = Mth.floor(gridX);
		final int baseY = Mth.floor(gridY);
		final int baseZ = Mth.floor(gridZ);

		final float fracX = (float) (gridX - baseX);
		final float fracY = (float) (gridY - baseY);
		final float fracZ = (float) (gridZ - baseZ);

		float block = 0F;
		float sky = 0F;
		float total = 0F;

		for (int corner = 0; corner < 8; corner++) {
			final int offsetX = corner & 1;
			final int offsetY = (corner >> 1) & 1;
			final int offsetZ = (corner >> 2) & 1;

			final float weight = (offsetX == 0 ? 1F - fracX : fracX)
				* (offsetY == 0 ? 1F - fracY : fracY)
				* (offsetZ == 0 ? 1F - fracZ : fracZ);
			if (weight <= 0F) continue;

			SCRATCH.set(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);

			final int sampled = level.hasChunkAt(SCRATCH) && !level.getBlockState(SCRATCH).canOcclude()
				? LightCoordsUtil.getLightCoords(level, SCRATCH)
				: lightCoords;

			block += LightCoordsUtil.block(sampled) * weight;
			sky += LightCoordsUtil.sky(sampled) * weight;
			total += weight;
		}

		if (total <= 0F) return lightCoords;

		final int smoothBlock = Math.min(MAX_SMOOTH, Math.round(block / total * LEVEL_SCALE));
		final int smoothSky = Math.min(MAX_SMOOTH, Math.round(sky / total * LEVEL_SCALE));
		final int result = LightCoordsUtil.smoothPack(
			Math.max(smoothBlock, LightCoordsUtil.block(lightCoords) == 15 ? MAX_SMOOTH : 0),
			smoothSky
		) | (lightCoords & GlowtoneBloom.EMISSIVE_MARKER);
		return result;
	}
}
