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

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(VertexConsumer.class)
public interface VertexConsumerMaterialTargetMixin {

	@Inject(method = "putBlockBakedQuad", at = @At("HEAD"))
	private void glowtone$targetBlockQuad(
		float red, float green, float blue, BakedQuad quad, com.mojang.blaze3d.vertex.QuadInstance instance, CallbackInfo info
	) {
		BlockMaterialRenderer.beginQuad(quad);
	}

	@Inject(method = "putBakedQuad", at = @At("HEAD"))
	private void glowtone$targetQuad(
		com.mojang.blaze3d.vertex.PoseStack.Pose pose, BakedQuad quad, com.mojang.blaze3d.vertex.QuadInstance instance, CallbackInfo info
	) {
		BlockMaterialRenderer.beginQuad(quad);
	}
}
