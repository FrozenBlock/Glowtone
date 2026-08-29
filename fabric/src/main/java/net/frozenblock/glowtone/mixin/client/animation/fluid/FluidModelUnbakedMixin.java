/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client.animation.fluid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.animation.BlockAnimationType;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.sprite.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(FluidModel.Unbaked.class)
public class FluidModelUnbakedMixin {

	@ModifyExpressionValue(
		method = "bake",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;byTransparency(Lcom/mojang/blaze3d/platform/Transparency;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
		)
	)
	private static ChunkSectionLayer glowtone$applyAnimatedLayer(
		ChunkSectionLayer original,
		@Local(name = "stillMaterial") Material.Baked stillMaterial,
		@Local(name = "flowingMaterial") Material.Baked flowingMaterial
	) {
		// TODO: metadata-based selection
		if (stillMaterial.sprite().contents().name().getPath().contains("lava")) return BlockAnimationType.FOLIAGE.getLayerByVanilla(original);
		return original;
	}
}
