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

package net.frozenblock.glowtone.mixin.client.particle;

import net.frozenblock.glowtone.particle.GlowtoneParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DustParticleBase.class)
public class DustParticleBaseMixin implements GlowtoneParticle {
	@Unique
	private int glowtone$lightEmission;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$copyEmissionFromOptions(
		ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
		ScalableParticleOptionsBase options, SpriteSet sprites, CallbackInfo info
	) {
		if (options instanceof GlowtoneParticle emissiveOptions) {
			this.glowtone$lightEmission = emissiveOptions.glowtone$getLightEmission();
		}
	}

	@Override
	public void glowtone$setLightEmission(int lightEmission) {
		this.glowtone$lightEmission = lightEmission;
	}

	@Override
	public int glowtone$getLightEmission() {
		return this.glowtone$lightEmission;
	}
}
