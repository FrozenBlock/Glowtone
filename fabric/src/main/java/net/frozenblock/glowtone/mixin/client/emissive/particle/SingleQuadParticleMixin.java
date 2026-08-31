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

package net.frozenblock.glowtone.mixin.client.emissive.particle;

import net.frozenblock.glowtone.particle.impl.GlowtoneEmissiveParticle;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.lighting.LightEngine;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {

	@Shadow
	protected float alpha;

	@Shadow
	public abstract float getQuadSize(float partialTickTime);

	@Inject(
		method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
		at = @At("TAIL")
	)
	private void glowtone$extractEmissiveOverlay(
		QuadParticleRenderState particleTypeRenderState,
		Quaternionf rotation,
		float x,
		float y,
		float z,
		float partialTickTime,
		CallbackInfo info
	) {
		if (!(this instanceof GlowtoneEmissiveParticle emissiveParticle) || !emissiveParticle.glowtone$hasEmissiveOverlay()) return;

		final int color = ARGB.colorFromFloat(
			this.alpha,
			emissiveParticle.glowtone$emissiveRCol(),
			emissiveParticle.glowtone$emissiveGCol(),
			emissiveParticle.glowtone$emissiveBCol()
		);

		final int baseLightCoords = ((ParticleInvokerMixin) this).glowtone$getLightCoords(partialTickTime);
		final int emissiveLightEmission = emissiveParticle.glowtone$emissiveLightEmission();
		final int emissiveLightCoords = emissiveLightEmission == 0
			? baseLightCoords
			: LightCoordsUtil.addSmoothBlockEmission(baseLightCoords, (float) emissiveLightEmission / LightEngine.MAX_LEVEL);

		particleTypeRenderState.add(
			emissiveParticle.glowtone$emissiveLayer(),
			x,
			y,
			z,
			rotation.x,
			rotation.y,
			rotation.z,
			rotation.w,
			this.getQuadSize(partialTickTime),
			emissiveParticle.glowtone$emissiveU0(),
			emissiveParticle.glowtone$emissiveU1(),
			emissiveParticle.glowtone$emissiveV0(),
			emissiveParticle.glowtone$emissiveV1(),
			color,
			emissiveLightCoords
		);
	}
}
