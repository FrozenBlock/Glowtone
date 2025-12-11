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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(SimpleUnbakedGeometry.class)
public abstract class SimpleUnbakedGeometryMixin {

	@ModifyExpressionValue(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/renderer/block/model/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/SpriteGetter;resolveSlot(Lnet/minecraft/client/renderer/block/model/TextureSlots;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
		)
	)
	private static TextureAtlasSprite glowtone$findEmissiveTexture(
		TextureAtlasSprite original,
		@Local(argsOnly = true) ModelBaker modelBaker,
		@Local(argsOnly = true) ModelDebugName modelDebugName,
		@Share("glowtone$emissiveSprite") LocalRef<TextureAtlasSprite> emissiveSpriteRef
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return original;

		final Identifier location = original.contents().name();
		final Identifier emissiveLocation = Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath() + "_glowtone_emissive");

		final TextureAtlasSprite emissiveSprite = modelBaker.sprites().get(new Material(original.atlasLocation(), emissiveLocation), modelDebugName);
		if (emissiveSprite != null && !emissiveSprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
			emissiveSpriteRef.set(emissiveSprite);
		}

		return original;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/renderer/block/model/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/model/FaceBakery;bakeQuad(Lnet/minecraft/client/resources/model/ModelBaker$PartCache;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lnet/minecraft/client/renderer/block/model/BlockElementFace;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/core/Direction;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/renderer/block/model/BlockElementRotation;ZI)Lnet/minecraft/client/renderer/block/model/BakedQuad;"
		)
	)
	private static BakedQuad glowtone$bakeEmissiveQuad(
		ModelBaker.PartCache parts,
		Vector3fc from,
		Vector3fc to,
		BlockElementFace face,
		TextureAtlasSprite sprite,
		Direction direction,
		ModelState modelState,
		BlockElementRotation rotation,
		boolean shade,
		int lightEmission,
		Operation<BakedQuad> original,
		@Share("glowtone$emissiveSprite") LocalRef<TextureAtlasSprite> emissiveSpriteRef,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final BakedQuad originalQuad = original.call(parts, from, to, face, sprite, direction, modelState, rotation, shade, lightEmission);

		final TextureAtlasSprite emissiveSprite = emissiveSpriteRef.get();
		if (emissiveSprite == null) return originalQuad;

		final BakedQuad emissiveQuad = original.call(parts, from, to, face, emissiveSprite, direction, modelState, rotation, shade, lightEmission);
		emissiveQuadRef.set(emissiveQuad);

		return originalQuad;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/renderer/block/model/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/QuadCollection$Builder;addUnculledFace(Lnet/minecraft/client/renderer/block/model/BakedQuad;)Lnet/minecraft/client/resources/model/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveUnculledFace(
		QuadCollection.Builder instance, BakedQuad bakedQuad, Operation<QuadCollection.Builder> original,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final QuadCollection.Builder builder = original.call(instance, bakedQuad);

		final BakedQuad emissiveQuad = emissiveQuadRef.get();
		if (emissiveQuad != null) original.call(instance, emissiveQuad);

		return builder;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/renderer/block/model/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/QuadCollection$Builder;addCulledFace(Lnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/block/model/BakedQuad;)Lnet/minecraft/client/resources/model/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveCulledFace(
		QuadCollection.Builder instance, Direction rotatedDirection, BakedQuad bakedQuad, Operation<QuadCollection.Builder> original,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final QuadCollection.Builder builder = original.call(instance, rotatedDirection, bakedQuad);

		final BakedQuad emissiveQuad = emissiveQuadRef.get();
		if (emissiveQuad != null) original.call(instance, rotatedDirection, emissiveQuad);

		return builder;
	}

}
