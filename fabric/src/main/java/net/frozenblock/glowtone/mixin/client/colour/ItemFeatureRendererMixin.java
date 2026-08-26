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

import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

	@Inject(method = "prepareMainSubmit", at = @At("HEAD"))
	private void glowtone$beginItemQuads(ItemFeatureRenderer.Submit submit, CallbackInfo info) {
		GlowtoneChromaFold.beginItemQuads(submit.glowtone$chromaTint(), submit.lightCoords());
	}

	@Inject(method = "prepareMainSubmit", at = @At("RETURN"))
	private void glowtone$endItemQuads(ItemFeatureRenderer.Submit submit, CallbackInfo info) {
		GlowtoneChromaFold.endItemQuads();
	}

	@ModifyArg(
		method = "prepareMainSubmit",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"
		),
		index = 0
	)
	private int glowtone$tintItemQuad(int quadColor) {
		// TODO: i swear this can be done via localref
		return GlowtoneChromaFold.tintItemColor(quadColor);
	}
}
