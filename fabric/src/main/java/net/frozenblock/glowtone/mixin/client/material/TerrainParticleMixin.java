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

package net.frozenblock.glowtone.mixin.client.material;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.frozenblock.glowtone.material.impl.GlowtoneMaterialHolder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(TerrainParticle.class)
public class TerrainParticleMixin implements GlowtoneMaterialHolder {
	@Unique
	private int glowtone$materialIndex;

	@Unique
	@Override
	public int glowtone$materialIndex() {
		return this.glowtone$materialIndex;
	}

	@Unique
	@Override
	public void glowtone$setMaterialIndex(int index) {
		this.glowtone$materialIndex = index;
	}

	@Inject(
		method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
		at = @At("RETURN")
	)
	private void glowtone$captureDirectMaterial(
		CallbackInfo info,
		@Local(argsOnly = true) BlockState blockState
	) {
		this.glowtone$materialIndex = BlockMaterials.shaderIndexFor(blockState);
	}

	@ModifyReturnValue(method = "createTerrainParticle", at = @At("RETURN"))
	private static TerrainParticle glowtone$captureMaterial(
		TerrainParticle original,
		@Local(argsOnly = true) BlockParticleOption options
	) {
		// TODO: injected interface
		if (original == null || !(original instanceof GlowtoneMaterialHolder materialHolder)) return original;

		final int index = BlockMaterials.shaderIndexFor(options.getState());
		materialHolder.glowtone$setMaterialIndex(index);
		return original;
	}
}
