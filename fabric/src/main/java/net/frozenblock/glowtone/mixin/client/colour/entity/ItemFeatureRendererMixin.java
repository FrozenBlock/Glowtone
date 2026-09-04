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

package net.frozenblock.glowtone.mixin.client.colour.entity;

import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

	@Inject(method = "prepareMainSubmit", at = @At("HEAD"))
	private void glowtone$beginItemQuads(ItemFeatureRenderer.Submit submit, CallbackInfo info) {
		ChromaFold.beginItemQuads(submit.glowtone$blockLightTint(), submit.lightCoords());
	}

	@Inject(method = "prepareMainSubmit", at = @At("RETURN"))
	private void glowtone$endItemQuads(CallbackInfo info) {
		ChromaFold.endItemQuads();
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
		// FIXME
		return quadColor;
		// TODO: i swear this can be done via localref
		//return ChromaFold.tintItemColor(quadColor);
	}
}
