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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ItemFeatureRenderer.Submit.class)
public class ItemFeatureRendererMaterialMixin implements GlowtoneMaterialHolder {
	@Unique
	private int glowtone$materialIndex;

	@Unique
	@Override
	public int glowtone$materialIndex() {
		return this.glowtone$materialIndex;
	}

	@Unique
	@Override
	public void glowtone$setMaterialIndex(int index) {
		this.glowtone$materialIndex = index;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowtone$captureItemMaterial(CallbackInfo info) {
		this.glowtone$materialIndex = BlockMaterialRenderer.renderedShaderIndex();
	}
}
