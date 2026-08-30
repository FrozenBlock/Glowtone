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

package net.frozenblock.glowtone.mixin.client.material;

import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.impl.GlowtoneMaterialHolder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererSubmitMaterialMixin {

	@Inject(method = "prepareMainSubmit", at = @At("HEAD"))
	private void glowtone$beginItemMaterial(ItemFeatureRenderer.Submit submit, CallbackInfo info) {
		// TODO: injected interface
		if (!((Object) submit instanceof GlowtoneMaterialHolder materialHolder)) return;

		final int index = materialHolder.glowtone$materialIndex();
		BlockMaterialRenderer.beginShaderIndex(index);
		BlockMaterialRenderer.beginGui(submit.displayContext() == ItemDisplayContext.GUI);
	}

	@ModifyArg(
		method = "prepareMainSubmit",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setLightCoords(I)V"
		),
		index = 0,
		require = 1
	)
	private int glowtone$markItemLightCoords(int lightCoords) {
		final int marked = BlockMaterialRenderer.markGui(BlockMaterialRenderer.markShaderIndex(lightCoords));
		return marked;
	}

	@Inject(method = "prepareMainSubmit", at = @At("RETURN"))
	private void glowtone$endItemMaterial(CallbackInfo info) {
		BlockMaterialRenderer.beginGui(false);
		BlockMaterialRenderer.endBlock();
	}
}
