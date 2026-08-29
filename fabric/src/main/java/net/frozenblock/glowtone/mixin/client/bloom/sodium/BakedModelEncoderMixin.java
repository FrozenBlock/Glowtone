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

package net.frozenblock.glowtone.mixin.client.bloom.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@ClientOnly
@Mixin(BakedModelEncoder.class)
public class BakedModelEncoderMixin {

	@ModifyExpressionValue(
		method = "writeQuadVertices",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;getLightCoordsWithEmission(II)I"
		)
	)
	private static int glowtone$markEmissiveQuad(
		int lightCoords,
		@Local(argsOnly = true) BakedQuadView quad
	) {
		return GlowtoneBloom.isEmissiveLevel(quad.getLightEmission())
			? GlowtoneBloom.mark(lightCoords)
			: lightCoords;
	}
}
