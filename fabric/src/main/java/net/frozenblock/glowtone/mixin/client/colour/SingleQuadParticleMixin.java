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

package net.frozenblock.glowtone.mixin.client.colour;

import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle {
	protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@ModifyArg(
			method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;add(Lnet/minecraft/client/particle/SingleQuadParticle$Layer;FFFFFFFFFFFFII)V"
			),
			index = 13
	)
	private int glowtone$tintParticle(
			SingleQuadParticle.Layer layer,
			float x,
			float y,
			float z,
			float rotationX,
			float rotationY,
			float rotationZ,
			float rotationW,
			float quadSize,
			float u0,
			float u1,
			float v0,
			float v1,
			int color,
			int lightCoords
	) {
		return GlowtoneChromaFold.tintParticleColor(color, lightCoords, this.x, this.y, this.z);
	}
}
