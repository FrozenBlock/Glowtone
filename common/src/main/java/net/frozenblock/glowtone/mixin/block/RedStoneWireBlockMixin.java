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

package net.frozenblock.glowtone.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.particle.GlowtoneParticle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {
	@Shadow
	@Final
	private static Vec3[] COLORS;

	@ModifyExpressionValue(
		method = "spawnParticlesAlongLine",
		at = @At(value = "NEW", target = "Lnet/minecraft/core/particles/DustParticleOptions;")
	)
	private DustParticleOptions glowtone$makeDustGlow(DustParticleOptions original, @Local(argsOnly = true) Vec3 particleColor) {
		if (original instanceof GlowtoneParticle emissive) {
			for (int power = 1; power < COLORS.length; power++) {
				if (COLORS[power] == particleColor) {
					emissive.glowtone$setLightEmission(power);
					break;
				}
			}
		}
		return original;
	}
}
