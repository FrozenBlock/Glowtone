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

package net.frozenblock.glowtone.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.fabric.render.GlowtoneItemRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderContext.class)
public abstract class ItemRenderContextMixin {
	@Shadow
	private MultiBufferSource vertexConsumerProvider;

	@ModifyExpressionValue(
		method = "renderQuad",
		at = @At(
			value = "INVOKE",
			target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/ItemRenderContext;getVertexConsumer(Lnet/fabricmc/fabric/api/renderer/v1/material/BlendMode;Lnet/fabricmc/fabric/api/util/TriState;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
		)
	)
	private VertexConsumer glowtone$routeEmissiveToFullbright(VertexConsumer original, @Local(ordinal = 0) RenderMaterial material) {
		if (material.emissive() && GlowtoneConstants.GLOWTONE_SHADING) {
			return this.vertexConsumerProvider.getBuffer(GlowtoneItemRenderTypes.ITEM_EMISSIVE);
		}
		return original;
	}
}
