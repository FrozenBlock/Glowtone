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

package net.frozenblock.glowtone.mixin.client.bloom;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {

	@ModifyExpressionValue(
		method = "putBlockBakedQuad",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;getLightCoordsWithEmission(II)I"
		)
	)
	private int glowtone$markEmissiveBlockQuad(int lightCoords, @Local(argsOnly = true) BakedQuad quad) {
		return GlowtoneBloom.isEmissiveQuad(quad) ? GlowtoneBloom.mark(lightCoords) : lightCoords;
	}

	@ModifyExpressionValue(
		method = "putBakedQuad",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;getLightCoordsWithEmission(II)I"
		)
	)
	private int glowtone$markEmissiveQuad(int lightCoords, @Local(argsOnly = true) BakedQuad quad) {
		return GlowtoneBloom.isEmissiveQuad(quad) ? GlowtoneBloom.mark(lightCoords) : lightCoords;
	}
}
