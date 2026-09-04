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

package net.frozenblock.glowtone.mixin.client.colour.feature;

import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.color.render.impl.BlockLightTinted;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

	@Inject(
		method = "prepareModel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"
		)
	)
	private <S> void glowtone$beginModelQuads(ModelFeatureRenderer.Submit<S> submit, CallbackInfo info) {
		if ((Object) submit instanceof BlockLightTinted blockLightTinted) ChromaFold.beginModelQuads(blockLightTinted.glowtone$blockLightTint());
	}

	@Inject(method = "prepareModel", at = @At("RETURN"))
	private void glowtone$endModelQuads(CallbackInfo info) {
		ChromaFold.endModelQuads();
	}
}
