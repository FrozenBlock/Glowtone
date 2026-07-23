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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.GlowtoneParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Particle.class)
public class ParticleMixin {
	@ModifyReturnValue(method = "getLightColor", at = @At("RETURN"))
	public int glowtone$emissiveParticleLight(int original) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !(Particle.class.cast(this) instanceof GlowtoneParticle emissive)) return original;

		final int emission = emissive.glowtone$getLightEmission();
		if (emission <= 0) return original;

		final int blockLight = Math.min(Math.max(original & 255, emission * 16), 240);
		final int skyLight = original >> 16 & 255;
		return blockLight | skyLight << 16;
	}
}
