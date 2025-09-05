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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.impl.GlowingDustParticleInterface;
import net.minecraft.core.particles.DustColorTransitionOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DustColorTransitionOptions.class)
public class DustColorTransitionOptionsMixin implements GlowingDustParticleInterface {

	@Shadow
	@Final
	public static DustColorTransitionOptions SCULK_TO_REDSTONE;

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

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void glowtone$makeSculkToRedstoneRedstoneParticlesEmissive(CallbackInfo info) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;
		if (SCULK_TO_REDSTONE instanceof GlowingDustParticleInterface glowingInterface) glowingInterface.glowtone$setLightEmission(15);
	}

}
