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
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(SimpleUnbakedGeometry.class)
public abstract class SimpleUnbakedGeometryMixin {

	@Shadow
	private static BakedQuad bakeFace(BlockElement blockElement, BlockElementFace blockElementFace, TextureAtlasSprite textureAtlasSprite, Direction direction, ModelState modelState) {
		return null;
	}

	@ModifyExpressionValue(
		method = "method_67933",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/SpriteGetter;resolveSlot(Lnet/minecraft/client/renderer/block/model/TextureSlots;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
		)
	)
	private static TextureAtlasSprite glowtone$findEmissiveTexture(
		TextureAtlasSprite original,
		@Local(argsOnly = true) SpriteGetter spriteGetter,
		@Local(argsOnly = true) ModelDebugName modelDebugName,
		@Share("glowtone$emissiveSprite") LocalRef<TextureAtlasSprite> emissiveSpriteRef
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return original;

		ResourceLocation location = original.contents().name();
		ResourceLocation emissiveLocation = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath() + "_glowtone_emissive");

		TextureAtlasSprite emissiveSprite = spriteGetter.get(new Material(original.atlasLocation(), emissiveLocation), modelDebugName);
		if (emissiveSprite != null && !emissiveSprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
			emissiveSpriteRef.set(emissiveSprite);
		}

		return original;
	}

	@WrapOperation(
		method = "method_67933",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/QuadCollection$Builder;addUnculledFace(Lnet/minecraft/client/renderer/block/model/BakedQuad;)Lnet/minecraft/client/resources/model/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveUnculledFace(
		QuadCollection.Builder instance, BakedQuad bakedQuad, Operation<QuadCollection.Builder> original,
		@Local(argsOnly = true) BlockElement blockElement,
		@Local(argsOnly = true) Direction direction,
		@Local(argsOnly = true) BlockElementFace blockElementFace,
		@Local(argsOnly = true) ModelState modelState,
		@Share("glowtone$emissiveSprite") LocalRef<TextureAtlasSprite> emissiveSpriteRef
	) {
		QuadCollection.Builder builder = original.call(instance, bakedQuad);

		final TextureAtlasSprite emissiveSprite = emissiveSpriteRef.get();
		if (emissiveSprite != null) builder.addUnculledFace(bakeFace(blockElement, blockElementFace, emissiveSprite, direction, modelState));

		return builder;
	}

	@WrapOperation(
		method = "method_67933",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/QuadCollection$Builder;addCulledFace(Lnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/block/model/BakedQuad;)Lnet/minecraft/client/resources/model/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveCulledFace(
		QuadCollection.Builder instance, Direction rotatedDirection, BakedQuad bakedQuad, Operation<QuadCollection.Builder> original,
		@Local(argsOnly = true) BlockElement blockElement,
		@Local(argsOnly = true) Direction direction,
		@Local(argsOnly = true) BlockElementFace blockElementFace,
		@Local(argsOnly = true) ModelState modelState,
		@Share("glowtone$emissiveSprite") LocalRef<TextureAtlasSprite> emissiveSpriteRef
	) {
		QuadCollection.Builder builder = original.call(instance, rotatedDirection, bakedQuad);

		final TextureAtlasSprite emissiveSprite = emissiveSpriteRef.get();
		if (emissiveSprite != null) builder.addCulledFace(rotatedDirection, bakeFace(blockElement, blockElementFace, emissiveSprite, direction, modelState));

		return builder;
	}

}
