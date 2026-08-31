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

package net.frozenblock.glowtone.mixin.client.emissive.particle;

import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.particle.GlowtoneParticleEmissives;
import net.frozenblock.glowtone.particle.impl.GlowtoneEmissiveParticle;
import net.frozenblock.glowtone.particle.impl.GlowtoneParticle;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(TerrainParticle.class)
public class TerrainParticleEmissiveMixin implements GlowtoneEmissiveParticle, GlowtoneParticle {
	@Unique
	private static final float GLOWTONE_BASE_DIM = 0.6F;

	@Shadow
	@Final
	private float uo;

	@Shadow
	@Final
	private float vo;

	@Unique
	private TextureAtlasSprite glowtone$emissiveSprite;

	@Unique
	private SingleQuadParticle.Layer glowtone$emissiveLayer;

	@Unique
	private int glowtone$lightEmission;

	@Unique
	private float glowtone$emissiveRCol;

	@Unique
	private float glowtone$emissiveGCol;

	@Unique
	private float glowtone$emissiveBCol;

	@Unique
	private int glowtone$emissiveLightEmission;

	@Inject(
		method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
		at = @At("TAIL")
	)
	private void glowtone$findEmissiveSprite(
		ClientLevel level,
		double x,
		double y,
		double z,
		double xa,
		double ya,
		double za,
		BlockState blockState,
		BlockPos pos,
		CallbackInfo info
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;

		final SingleQuadParticleAccessor accessor = (SingleQuadParticleAccessor) this;
		final TextureAtlasSprite sprite = accessor.glowtone$getSprite();
		if (sprite == null) return;

		final GlowtoneParticleEmissives.Resolved resolved = GlowtoneParticleEmissives.forSprite(sprite);
		this.glowtone$lightEmission = resolved.baseEmission();

		final float tintR = accessor.glowtone$getRCol() / GLOWTONE_BASE_DIM;
		final float tintG = accessor.glowtone$getGCol() / GLOWTONE_BASE_DIM;
		final float tintB = accessor.glowtone$getBCol() / GLOWTONE_BASE_DIM;

		if (resolved.present()) {
			this.glowtone$emissiveSprite = resolved.sprite();
			this.glowtone$emissiveLayer = resolved.layer();
			this.glowtone$emissiveLightEmission = resolved.emissiveEmission();

			final float scale = resolved.emissiveShade() ? GLOWTONE_BASE_DIM : 1F;
			this.glowtone$emissiveRCol = tintR / scale;
			this.glowtone$emissiveGCol = tintG / scale;
			this.glowtone$emissiveBCol = tintB / scale;
		}

		if (!resolved.baseShade()) {
			accessor.glowtone$setRCol(tintR);
			accessor.glowtone$setGCol(tintG);
			accessor.glowtone$setBCol(tintB);
		}
	}

	@Override
	public boolean glowtone$hasEmissiveOverlay() {
		return this.glowtone$emissiveSprite != null;
	}

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

	@Override
	public SingleQuadParticle.Layer glowtone$emissiveLayer() {
		return this.glowtone$emissiveLayer;
	}

	@Override
	public float glowtone$emissiveU0() {
		return this.glowtone$emissiveSprite.getU((this.uo + 1F) / 4F);
	}

	@Override
	public float glowtone$emissiveU1() {
		return this.glowtone$emissiveSprite.getU(this.uo / 4F);
	}

	@Override
	public float glowtone$emissiveV0() {
		return this.glowtone$emissiveSprite.getV(this.vo / 4F);
	}

	@Override
	public float glowtone$emissiveV1() {
		return this.glowtone$emissiveSprite.getV((this.vo + 1F) / 4F);
	}

	@Override
	public float glowtone$emissiveRCol() {
		return this.glowtone$emissiveRCol;
	}

	@Override
	public float glowtone$emissiveGCol() {
		return this.glowtone$emissiveGCol;
	}

	@Override
	public float glowtone$emissiveBCol() {
		return this.glowtone$emissiveBCol;
	}

	@Override
	public int glowtone$emissiveLightEmission() {
		return this.glowtone$emissiveLightEmission;
	}
}
