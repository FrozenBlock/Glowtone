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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(UnbakedCuboidGeometry.class)
public abstract class UnbakedCuboidGeometryMixin {

	@ModifyExpressionValue(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/sprite/MaterialBaker;resolveSlot(Lnet/minecraft/client/resources/model/sprite/TextureSlots;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/sprite/Material$Baked;"
		)
	)
	private static Material.Baked glowtone$findEmissiveTexture(
		Material.Baked original,
		@Local(argsOnly = true) ModelBaker modelBaker,
		@Local(argsOnly = true) ModelDebugName name,
		@Share("glowtone$emissiveMaterial") LocalRef<Material.Baked> emissiveMaterialRef,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		emissiveMaterialRef.set(null);
		emissiveQuadRef.set(null);
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return original;

		final TextureAtlasSprite sprite = original.sprite();
		final Identifier location = sprite.contents().name();
		final Identifier emissiveLocation = location.withSuffix(GlowtoneConstants.EMISSIVE_SUFFIX);

		final Material.Baked emissiveMaterial = modelBaker.materials().get(new Material(emissiveLocation), name);
		if (emissiveMaterial != null && !emissiveMaterial.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
			emissiveMaterialRef.set(emissiveMaterial);
		}

		return original;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/cuboid/FaceBakery;bakeQuad(Lnet/minecraft/client/resources/model/ModelBaker;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lnet/minecraft/client/resources/model/cuboid/CuboidFace;Lnet/minecraft/client/resources/model/sprite/Material$Baked;Lnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/cuboid/CuboidRotation;ZI)Lnet/minecraft/client/resources/model/geometry/BakedQuad;"
		)
	)
	private static BakedQuad glowtone$bakeEmissiveQuad(
		ModelBaker modelBaker,
		Vector3fc from,
		Vector3fc _to,
		CuboidFace face,
		Material.Baked material,
		Direction facing,
		ModelState modelState,
		@Nullable CuboidRotation elementRotation,
		boolean shade,
		int lightEmission,
		Operation<BakedQuad> original,
		@Share("glowtone$emissiveMaterial") LocalRef<Material.Baked> emissiveMaterialRef,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final BakedQuad originalQuad = original.call(modelBaker, from, _to, face, material, facing, modelState, elementRotation, shade, lightEmission);

		final Material.Baked emissiveMaterial = emissiveMaterialRef.get();
		if (emissiveMaterial == null) return originalQuad;

		final BakedQuad emissiveQuad = original.call(modelBaker, from, _to, face, emissiveMaterial, facing, modelState, elementRotation, shade, lightEmission);
		emissiveQuadRef.set(emissiveQuad);

		return originalQuad;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;addUnculledFace(Lnet/minecraft/client/resources/model/geometry/BakedQuad;)Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveUnculledFace(
		QuadCollection.Builder instance, BakedQuad quad, Operation<QuadCollection.Builder> original,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final QuadCollection.Builder builder = original.call(instance, quad);

		final BakedQuad emissiveQuad = emissiveQuadRef.get();
		if (emissiveQuad != null) original.call(instance, emissiveQuad);

		return builder;
	}

	@WrapOperation(
		method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;addCulledFace(Lnet/minecraft/core/Direction;Lnet/minecraft/client/resources/model/geometry/BakedQuad;)Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;"
		)
	)
	private static QuadCollection.Builder glowtone$bakeEmissiveCulledFace(
		QuadCollection.Builder instance, Direction direction, BakedQuad quad, Operation<QuadCollection.Builder> original,
		@Share("glowtone$emissiveQuad") LocalRef<BakedQuad> emissiveQuadRef
	) {
		final QuadCollection.Builder builder = original.call(instance, direction, quad);

		final BakedQuad emissiveQuad = emissiveQuadRef.get();
		if (emissiveQuad != null) original.call(instance, direction, emissiveQuad);

		return builder;
	}
}
