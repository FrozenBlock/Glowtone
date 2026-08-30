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

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.impl.GlowtoneMaterialHolder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

	@Inject(method = "appendItemLayers", at = @At("RETURN"))
	private void glowtone$resolveItemMaterial(
		CallbackInfo info,
		@Local(argsOnly = true) ItemStackRenderState output,
		@Local(argsOnly = true) ItemStack item
	) {
		if (!BlockMaterialRenderer.anyShaders() || !(output instanceof GlowtoneMaterialHolder holder)) return;

		final int index = item.getItem() instanceof BlockItem blockItem
			? BlockMaterialRenderer.shaderIndexFor(blockItem.getBlock().defaultBlockState())
			: BlockMaterialRenderer.NO_SHADER;

		holder.glowtone$setMaterialIndex(index);
	}
}
