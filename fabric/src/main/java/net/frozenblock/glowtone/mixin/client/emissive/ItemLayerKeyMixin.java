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

package net.frozenblock.glowtone.mixin.client.emissive;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(ItemModelGenerator.ItemLayerKey.class)
public class ItemLayerKeyMixin {

	@WrapOperation(
		method = "compute(Lnet/minecraft/client/resources/model/ModelBaker;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/cuboid/ItemModelGenerator;bakeExtrudedSprite(Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;Lnet/minecraft/client/resources/model/ModelBaker$Interner;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;)V"
		)
	)
	public void glowtone$computeWithGlowtone(
		QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo, Operation<Void> original,
		ModelBaker modelBakery
	) {
		original.call(builder, interner, modelState, materialInfo);

		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;

		final TextureAtlasSprite sprite = materialInfo.sprite();
		final Identifier location = sprite.contents().name();
		final Identifier emissiveLocation = location.withSuffix(GlowtoneConstants.EMISSIVE_SUFFIX);

		final Material.Baked emissiveMaterial = modelBakery.materials().get(new Material(emissiveLocation), () -> "generated item");
		if (emissiveMaterial == null || emissiveMaterial.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) return;

		final BakedQuad.MaterialInfo emissiveMaterialInfo = interner.materialInfo(
			BakedQuad.MaterialInfo.of(
				emissiveMaterial,
				emissiveMaterial.sprite().transparency(),
				materialInfo.tintIndex(),
				materialInfo.shade(),
				materialInfo.lightEmission()
			)
		);
		original.call(builder, interner, modelState, emissiveMaterialInfo);
	}
}
