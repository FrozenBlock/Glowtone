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
import net.frozenblock.glowtone.bloom.GlowtoneBloomRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(SingleQuadParticle.class)
public class SingleQuadParticleMixin {

	@ModifyExpressionValue(
		method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/particle/SingleQuadParticle;getLightCoords(F)I"
		)
	)
	private int glowtone$markEmissiveParticle(int lightCoords) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !GlowtoneBloomRenderer.isEnabled()) return lightCoords;

		final int worldCoords = SingleQuadParticle.class.cast(this).glowtone$getWorldLightCoords();
		if (LightCoordsUtil.smoothBlock(lightCoords) <= LightCoordsUtil.smoothBlock(worldCoords)) return lightCoords;

		return GlowtoneBloom.mark(lightCoords);
	}
}
