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

package net.frozenblock.glowtone.mixin.client.lighting;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.frozenblock.glowtone.lighting.WorldLightCurves;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(BlockModelLighter.class)
public class BlockModelLighterWorldCurveMixin {

	@Inject(method = "prepareQuadAmbientOcclusion", at = @At("TAIL"))
	private void glowtone$bendSmoothLight(
		BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad, QuadInstance outputInstance, CallbackInfo info
	) {
		glowtone$bend(level, pos, outputInstance);
	}

	@Inject(method = "prepareQuadFlat", at = @At("TAIL"))
	private void glowtone$bendFlatLight(
		BlockAndTintGetter level, BlockState state, BlockPos pos, int lightCoords, BakedQuad quad, QuadInstance outputInstance, CallbackInfo info
	) {
		glowtone$bend(level, pos, outputInstance);
	}

	@Unique
	private static void glowtone$bend(BlockAndTintGetter level, BlockPos pos, QuadInstance outputInstance) {
		if (!WorldLightCurves.any()) return;

		final long blockCurve = WorldLightCurves.blockCurveAt(level, pos);
		final long skyCurve = WorldLightCurves.skyCurveAt(level, pos);
		if (blockCurve == WorldLightCurves.IDENTITY && skyCurve == WorldLightCurves.IDENTITY) return;

		for (int vertex = 0; vertex < 4; vertex++) {
			outputInstance.setLightCoords(vertex, WorldLightCurves.remap(outputInstance.getLightCoords(vertex), blockCurve, skyCurve));
		}
	}
}
