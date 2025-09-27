/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client.redstone_dust_particle;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.impl.GlowingDustParticleInterface;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Particle.class)
public class ParticleMixin {

	@ModifyReturnValue(method = "getLightColor", at = @At(value = "RETURN"))
	public int glowtone$renderDustWithEmission(int original) {
		if (GlowtoneConstants.GLOWTONE_EMISSIVES && Particle.class.cast(this) instanceof GlowingDustParticleInterface glowingParticle) {
			final int emission = glowingParticle.glowtone$getLightEmission();
			if (emission == 0) return original;
			int j = Math.max(original & 0xFF, emission * 16);
			int k = original >> 16 & 0xFF;
			if (j > 240) j = 240;

			return j | k << 16;
		}
		return original;
	}
}
