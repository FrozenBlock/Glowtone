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

package net.frozenblock.glowtone.mixin.client.emissive.redstone_dust_particle;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.impl.GlowtoneLitParticle;
import net.frozenblock.glowtone.particle.impl.GlowtoneParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Particle.class)
public class ParticleMixin implements GlowtoneLitParticle {

	@Unique
	private int glowtone$worldLightCoords;

	@Unique
	@Override
	public void glowtone$setWorldLightCoords(int lightCoords) {
		this.glowtone$worldLightCoords = lightCoords;
	}

	@Unique
	@Override
	public int glowtone$getWorldLightCoords() {
		return this.glowtone$worldLightCoords;
	}

	@ModifyReturnValue(method = "getLightCoords", at = @At("RETURN"))
	public int glowtone$renderDustWithEmission(int original) {
		this.glowtone$worldLightCoords = original;

		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !(Particle.class.cast(this) instanceof GlowtoneParticle glowingInterface)) return original;

		final int emission = glowingInterface.glowtone$getLightEmission();
		if (emission == 0) return original;
		return LightCoordsUtil.addSmoothBlockEmission(original, (float) emission / LightEngine.MAX_LEVEL);
	}
}
