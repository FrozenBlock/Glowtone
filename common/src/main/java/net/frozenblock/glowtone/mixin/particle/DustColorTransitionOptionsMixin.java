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

package net.frozenblock.glowtone.mixin.particle;

import net.frozenblock.glowtone.particle.GlowtoneParticle;
import net.minecraft.core.particles.DustColorTransitionOptions;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DustColorTransitionOptions.class)
public class DustColorTransitionOptionsMixin implements GlowtoneParticle {
	@Unique
	private int glowtone$lightEmission;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$markEmissive(Vector3f fromColor, Vector3f toColor, float scale, CallbackInfo info) {
		this.glowtone$lightEmission = 15;
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
