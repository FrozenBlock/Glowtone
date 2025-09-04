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

package net.frozenblock.glowtone.mixin.client.redstone_dust;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.impl.GlowingDustParticleInterface;
import net.minecraft.core.particles.DustParticleOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DustParticleOptions.class)
public class DustParticleOptionsMixin implements GlowingDustParticleInterface {

	@Shadow
	@Final
	public static int REDSTONE_PARTICLE_COLOR;

	@Unique
	private int glowtone$lightEmission;

	@Unique
	@Override
	public void glowtone$setLightEmission(int lightEmission) {
		this.glowtone$lightEmission = lightEmission;
	}

	@Unique
	@Override
	public int glowtone$getLightEmission() {
		return this.glowtone$lightEmission;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	public void glowtone$makeBaseRedstoneParticlesEmissive(int color, float scale, CallbackInfo info) {
		if (GlowtoneConstants.GLOWTONE_EMISSIVES && color == REDSTONE_PARTICLE_COLOR) this.glowtone$setLightEmission(15);
	}

}
