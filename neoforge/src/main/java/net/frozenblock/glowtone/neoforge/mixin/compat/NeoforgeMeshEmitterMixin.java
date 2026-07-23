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

package net.frozenblock.glowtone.neoforge.mixin.compat;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.emissive.EmissiveResolver;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.NeoforgeMeshEmitter", remap = false)
public abstract class NeoforgeMeshEmitterMixin {
	@Shadow @Final private RenderType renderType;
	@Shadow private boolean defaultAo;

	@Shadow public abstract BufferBuilder getBuffer(Material material);

	@Unique
	private static final Map<Material, Material> glowtone$fullbrightMaterials = new ConcurrentHashMap<>();

	@Inject(
		method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void glowtone$emissiveContraptionQuad(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, CallbackInfo ci) {
		if (!glowtone$isEmissive(quad)) return;
		final Material fullbright = glowtone$fullbrightFor(quad);
		if (fullbright == null) return;
		final BufferBuilder buffer = this.getBuffer(fullbright);
		if (buffer != null) {
			buffer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
		}
		ci.cancel();
	}

	@Inject(
		method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void glowtone$emissiveContraptionQuad(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readAlpha, CallbackInfo ci) {
		if (!glowtone$isEmissive(quad)) return;
		final Material fullbright = glowtone$fullbrightFor(quad);
		if (fullbright == null) return;
		final BufferBuilder buffer = this.getBuffer(fullbright);
		if (buffer != null) {
			buffer.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readAlpha);
		}
		ci.cancel();
	}

	@Unique
	private static boolean glowtone$isEmissive(BakedQuad quad) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return false;
		final TextureAtlasSprite sprite = quad.getSprite();
		return sprite != null && EmissiveResolver.lightEmissionFor(sprite) == 15;
	}

	@Unique
	@Nullable
	private Material glowtone$fullbrightFor(BakedQuad quad) {
		final Material base = ModelUtil.getMaterial(this.renderType, quad.isShade(), quad.hasAmbientOcclusion() && this.defaultAo);
		if (base == null) return null;
		return glowtone$fullbrightMaterials.computeIfAbsent(base, original -> SimpleMaterial.builderOf(original)
			.useLight(false)
			.cardinalLightingMode(CardinalLightingMode.OFF)
			.build());
	}
}
