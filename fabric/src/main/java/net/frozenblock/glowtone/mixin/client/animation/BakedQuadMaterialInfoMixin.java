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

package net.frozenblock.glowtone.mixin.client.animation;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.Transparency;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.animation.BlockAnimationType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(BakedQuad.MaterialInfo.class)
public class BakedQuadMaterialInfoMixin {

	@Inject(
		method = "of",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/sprite/Material$Baked;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
			ordinal = 0
		)
	)
	private static void glowtone$useFoliageLayer(
		Material.Baked material, Transparency transparency, int tintIndex, boolean shade, int lightEmission, CallbackInfoReturnable<BakedQuad.MaterialInfo> info,
		@Local(name = "layer") LocalRef<ChunkSectionLayer> layer
	) {
		if (material.sprite().contents().name().getPath().endsWith("leaves")) layer.set(BlockAnimationType.FOLIAGE.getLayerByVanilla(layer.get()));
		if (material.sprite().contents().name().getPath().endsWith("fire")) layer.set(BlockAnimationType.FIRE.getLayerByVanilla(layer.get()));
	}
}
