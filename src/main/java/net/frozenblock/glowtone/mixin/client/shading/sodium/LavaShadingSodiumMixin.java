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

package net.frozenblock.glowtone.mixin.client.shading.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(DefaultFluidRenderer.class)
public class LavaShadingSodiumMixin {

	@Inject(method = "render", at = @At("HEAD"))
	private void glowtone$setUnshadeIfApplicable(
		CallbackInfo callback,
		@Local(argsOnly = true) FluidState fluidState,
		@Share("glowtone$shouldUnshade") LocalBooleanRef shouldUnshade
	) {
		shouldUnshade.set(GlowtoneConstants.GLOWSTONE_SHADING && fluidState.is(FluidTags.LAVA));
	}

	@ModifyExpressionValue(
		method = "render",
		at = {
			@At(value = "CONSTANT", args = "floatValue=0.8", ordinal = 0),
			@At(value = "CONSTANT", args = "floatValue=0.6", ordinal = 0),
		},
		remap = false
	)
	private float glowtone$unshade(
		float original,
		@Share("glowtone$shouldUnshade") LocalBooleanRef shouldUnshade
	) {
		return shouldUnshade.get() ? 1F : original;
	}
}
