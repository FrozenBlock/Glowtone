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
import net.frozenblock.glowtone.material.BlockMaterials;
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
		@Local(argsOnly = true) ItemStackRenderState state,
		@Local(argsOnly = true) ItemStack stack
	) {
		if (!BlockMaterials.anyShaders() || !(state instanceof GlowtoneMaterialHolder holder)) return;

		final int index = stack.getItem() instanceof BlockItem blockItem
			? BlockMaterials.shaderIndexFor(blockItem.getBlock().defaultBlockState())
			: BlockMaterials.NO_SHADER;

		holder.glowtone$setMaterialIndex(index);

	}
}
