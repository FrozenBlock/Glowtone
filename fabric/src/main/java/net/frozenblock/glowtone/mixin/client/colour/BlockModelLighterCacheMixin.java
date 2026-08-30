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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.light.occlusion.OcclusionOverrideHelper;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(BlockModelLighter.Cache.class)
public class BlockModelLighterCacheMixin {

	@ModifyReturnValue(method = "getShadeBrightness", at = @At("RETURN"))
	private float glowtone$scaleVanillaOcclusion(float brightness, @Local(argsOnly = true) BlockState state) {
		if (AmbientOcclusionOption.vanillaActive()) {
			return OcclusionStrengthOption.brightness(glowtone$applyCast(brightness, state));
		}
		return ChromaBaker.buildingSection() ? 1F : brightness;
	}

	@Unique
	private static float glowtone$applyCast(float brightness, BlockState state) {
		if (!OcclusionOverrideHelper.any()) return brightness;

		final boolean automatic = brightness < 1F;
		if (!OcclusionOverrideHelper.casts(state, automatic)) return 1F;
		return automatic ? brightness : OcclusionOverrideHelper.FULL_OCCLUDER;
	}
}
