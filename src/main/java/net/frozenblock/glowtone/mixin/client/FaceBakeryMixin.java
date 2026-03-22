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

package net.frozenblock.glowtone.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.math.Quadrant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.resources.metadata.EmissiveMetadataSection;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

	@Inject(
		method = "bakeQuad(Lnet/minecraft/client/resources/model/ModelBaker$Interner;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lnet/minecraft/client/resources/model/cuboid/CuboidFace$UVs;Lcom/mojang/math/Quadrant;Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;Lnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/cuboid/CuboidRotation;)Lnet/minecraft/client/resources/model/geometry/BakedQuad;",
		at = @At("HEAD")
	)
	private static void glowtone$bakeWithEmission(
		CallbackInfoReturnable<BakedQuad> info,
		@Local(argsOnly = true) LocalRef<BakedQuad.MaterialInfo> materialInfoRef
	) {
		final BakedQuad.MaterialInfo materialInfo = materialInfoRef.get();
		boolean shade = materialInfo.shade();
		int lightEmission = materialInfo.lightEmission();
		boolean isModified = false;

		if (GlowtoneConstants.GLOWTONE_EMISSIVES) {
			final SpriteContents contents = materialInfo.sprite().contents();

			final Optional<EmissiveMetadataSection> optionalEmissiveMetadata = contents.getAdditionalMetadata(EmissiveMetadataSection.TYPE);
			if (optionalEmissiveMetadata.isPresent()) {
				final EmissiveMetadataSection emissiveMetadata = optionalEmissiveMetadata.get();
				shade = emissiveMetadata.shade().orElse(shade);
				lightEmission = emissiveMetadata.lightEmission();
				isModified = true;
			} else if (contents.name().getPath().endsWith("_glowtone_emissive")) {
				lightEmission = 15;
				isModified = true;
			}
		}

		if (GlowtoneConstants.GLOWTONE_SHADING) {
			final boolean wasShaded = shade;
			shade = shade && lightEmission != 15;
			isModified = isModified || wasShaded != shade;
		}

		if (!isModified) return;

		materialInfoRef.set(
			new BakedQuad.MaterialInfo(
				materialInfo.sprite(),
				materialInfo.layer(),
				materialInfo.itemRenderType(),
				materialInfo.tintIndex(),
				shade,
				lightEmission
			)
		);
	}

}
