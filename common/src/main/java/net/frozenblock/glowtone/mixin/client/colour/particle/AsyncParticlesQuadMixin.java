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

package net.frozenblock.glowtone.mixin.client.colour.particle;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Still needs some work, some particles look pretty bad
@ClientOnly
@Mixin(value = SingleQuadParticle.class, priority = 1500)
public class AsyncParticlesQuadMixin {

	@Dynamic
	@ModifyReturnValue(method = "asyncparticles$getColor", at = @At("RETURN"), require = 0)
	private int glowtone$tintAsyncParticle(int color) {
		if (!(this instanceof GpuParticleAddon particle)) return color;

		final int light = particle.asyncparticles$getGpuLightCoords(0F);
		if (light == LightCoordsUtil.FULL_BRIGHT) return color;

		final int tint = ChromaFold.resolveParticle(
			particle.asyncparticles$getX(), particle.asyncparticles$getY(), particle.asyncparticles$getZ(), light
		);
		if (tint == ChromaFold.NO_TINT) return color;

		// The buffer this feeds is ABGR, so the tint has to be swapped to match before it is multiplied in.
		return ARGB.multiply(color, ARGB.toABGR(tint));
	}
}
