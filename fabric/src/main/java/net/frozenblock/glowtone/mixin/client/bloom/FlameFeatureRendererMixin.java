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
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(FlameFeatureRenderer.class)
public class FlameFeatureRendererMixin {

	@ModifyExpressionValue(
		method = "prepare",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/LightCoordsUtil;withBlock(II)I"
		),
		require = 0
	)
	private static int glowtone$markEntityFlame(int lightCoords) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return lightCoords;
		return GlowtoneBloom.mark(lightCoords);
	}
}
