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

package net.frozenblock.glowtone.mixin.client.colour.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.frozenblock.glowtone.render.GlowtoneChromaBake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = LightDataAccess.class, remap = false)
public class LightDataAccessMixin {
	@ModifyExpressionValue(
		method = "compute",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
		)
	)
	private float glowtone$scaleOcclusion(float brightness) {
		return GlowtoneChromaBake.occlusionShade(brightness);
	}
}
