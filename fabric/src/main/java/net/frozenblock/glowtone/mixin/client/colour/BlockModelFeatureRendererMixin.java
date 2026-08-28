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
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.color.render.impl.GlowtoneChromaTinted;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockModelFeatureRenderer.class)
public class BlockModelFeatureRendererMixin {

	@Inject(
		method = "buildGroup",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setLightCoords(I)V",
			shift = At.Shift.AFTER
		)
	)
	private void glowtone$beginBlockQuads(
		FeatureFrameContext context,
		List<BlockModelFeatureRenderer.Submit> submits,
		CallbackInfo info,
		@Local BlockModelFeatureRenderer.Submit submit
	) {
		ChromaFold.beginBlockQuads(
			((GlowtoneChromaTinted) (Object) submit).glowtone$chromaTint(),
			submit.lightCoords(),
			submit.renderType()
		);
	}

	@Inject(method = "buildGroup", at = @At("RETURN"))
	private void glowtone$endBlockQuads(
		FeatureFrameContext context,
		List<BlockModelFeatureRenderer.Submit> submits,
		CallbackInfo info
	) {
		ChromaFold.endBlockQuads();
	}

	@ModifyArg(
		method = "putQuad",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"),
		index = 0
	)
	private static int glowtone$tintBlockQuad(int quadColor) {
		return ChromaFold.tintBlockQuadColor(quadColor);
	}
}
