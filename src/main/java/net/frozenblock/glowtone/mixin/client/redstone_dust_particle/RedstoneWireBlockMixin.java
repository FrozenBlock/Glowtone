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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.impl.GlowingDustParticleInterface;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(RedStoneWireBlock.class)
public class RedstoneWireBlockMixin {

	@Shadow
	@Final
	private static int[] COLORS;

	@ModifyExpressionValue(
		method = "spawnParticlesAlongLine",
		at = @At(
			value = "NEW",
			target = "Lnet/minecraft/core/particles/DustParticleOptions;"
		)
	)
	private static DustParticleOptions glowtone$makeDustGlow(
		DustParticleOptions original,
		@Local(argsOnly = true) int color
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !(original instanceof GlowingDustParticleInterface glowingDustParticleInterface)) return original;

		for (int i = 1; i <= 15; i++) {
			if (COLORS[i] != color) continue;
			glowingDustParticleInterface.glowtone$setLightEmission(i);
			break;
		}

		return original;
	}

}
