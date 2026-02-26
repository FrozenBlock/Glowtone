/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.Material;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(ItemModelGenerator.ItemLayerKey.class)
public class ItemLayerKeyMixin {

	@WrapOperation(
		method = "compute(Lnet/minecraft/client/resources/model/ModelBaker;)Lnet/minecraft/client/resources/model/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/model/ItemModelGenerator;bakeExtrudedSprite(Lnet/minecraft/client/resources/model/QuadCollection$Builder;Lnet/minecraft/client/resources/model/ModelBaker$Interner;Lnet/minecraft/client/resources/model/ModelState;ILnet/minecraft/client/renderer/block/model/BakedQuad$SpriteInfo;)V"
		)
	)
	public void glowtone$computeWithGlowtone(
		QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, int tintIndex, BakedQuad.SpriteInfo spriteInfo, Operation<Void> original,
		ModelBaker modelBakery
	) {
		original.call(builder, interner, modelState, tintIndex, spriteInfo);

		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;

		final TextureAtlasSprite sprite = spriteInfo.sprite();
		final Identifier location = sprite.contents().name();
		final Identifier emissiveLocation = location.withSuffix("_glowtone_emissive");

		final Material.Baked emissiveMaterial = modelBakery.materials().get(new Material(emissiveLocation), () -> "generated item");
		if (emissiveMaterial == null || emissiveMaterial.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) return;

		final BakedQuad.SpriteInfo emissiveSpriteInfo = interner.spriteInfo(BakedQuad.SpriteInfo.of(emissiveMaterial, emissiveMaterial.sprite().transparency()));
		original.call(builder, interner, modelState, tintIndex, emissiveSpriteInfo);
	}
}
