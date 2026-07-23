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
import net.frozenblock.glowtone.emissive.EmissiveResolver;
import net.frozenblock.glowtone.particle.GlowtoneParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TerrainParticle.class)
public class TerrainParticleMixin implements GlowtoneParticle {
	@Unique
	private int glowtone$lightEmission;

	@Inject(
		method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
		at = @At("TAIL")
	)
	private void glowtone$detectEmissive(
		ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state, BlockPos pos, CallbackInfo info
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;
		final TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
		if (sprite == null) return;
		if (EmissiveResolver.lightEmissionFor(sprite) == 15 || EmissiveResolver.overlayFor(sprite) != null) {
			this.glowtone$lightEmission = 15;
		}
	}

	@ModifyReturnValue(method = "getLightColor", at = @At("RETURN"))
	public int glowtone$emissiveBreakParticle(int original) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || this.glowtone$lightEmission <= 0) return original;
		final int blockLight = Math.min(Math.max(original & 255, this.glowtone$lightEmission * 16), 240);
		final int skyLight = original >> 16 & 255;
		return blockLight | skyLight << 16;
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
