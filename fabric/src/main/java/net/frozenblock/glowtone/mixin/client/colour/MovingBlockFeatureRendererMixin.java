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

package net.frozenblock.glowtone.mixin.client.colour;

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.render.light.color.ChromaFold;
import net.frozenblock.glowtone.render.light.entity.SmoothEntityLightingHelper;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MovingBlockFeatureRenderer.class)
public class MovingBlockFeatureRendererMixin {

	@Inject(
		method = "buildGroup",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/MovingBlockFeatureRenderer$Submit;movingBlockRenderState()Lnet/minecraft/client/renderer/block/MovingBlockRenderState;",
			shift = At.Shift.AFTER
		)
	)
	private void glowtone$beginMovingBlockQuads(
		FeatureFrameContext context,
		List<MovingBlockFeatureRenderer.Submit> submits,
		CallbackInfo info,
		@Local MovingBlockFeatureRenderer.Submit submit
	) {
		final MovingBlockRenderState renderState = submit.movingBlockRenderState();
		final BlockPos pos = renderState.blockPos;
		final int lightCoords = SmoothEntityLightingHelper.worldLightAt(
			pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, LightCoordsUtil.FULL_BRIGHT);

		ChromaFold.beginMovingBlockQuads(
			ChromaFold.resolveBlockEntity(pos, lightCoords));
	}

	@Inject(method = "buildGroup", at = @At("RETURN"))
	private void glowtone$endMovingBlockQuads(
		FeatureFrameContext context,
		List<MovingBlockFeatureRenderer.Submit> submits,
		CallbackInfo info
	) {
		ChromaFold.endMovingBlockQuads();
	}

}
