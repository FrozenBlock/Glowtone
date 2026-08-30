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

package net.frozenblock.glowtone.mixin.client.colour;

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.light.occlusion.OcclusionOverrideHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@ClientOnly
@Mixin(BlockModelLighter.class)
public class BlockModelLighterAmbientMixin {

	@ModifyArg(
		method = "prepareQuadAmbientOcclusion",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/ARGB;gray(F)I"
		),
		index = 0,
		require = 0
	)
	private static float glowtone$clampCorner(float occlusion, @Local(argsOnly = true) BlockState state) {
		final float clamped = Mth.clamp(occlusion, 0F, 1F);
		if (!AmbientOcclusionOption.vanillaActive()) return clamped;

		return OcclusionOverrideHelper.receives(state, true) ? clamped : 1F;
	}
}
