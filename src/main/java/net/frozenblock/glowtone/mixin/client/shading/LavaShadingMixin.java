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

package net.frozenblock.glowtone.mixin.client.shading;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LiquidBlockRenderer.class)
public class LavaShadingMixin {

	@Inject(method = "tesselate", at = @At("HEAD"))
	public void glowtone$setUnshadeIfApplicable(
		CallbackInfo info,
		@Local(argsOnly = true) FluidState fluidState,
		@Share("glowtone$shouldUnshade") LocalBooleanRef shouldUnshade
	) {
		shouldUnshade.set(GlowtoneConstants.GLOWTONE_SHADING && fluidState.is(FluidTags.LAVA));
	}

	@ModifyExpressionValue(
		method = "tesselate",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/BlockAndTintGetter;getShade(Lnet/minecraft/core/Direction;Z)F"
		)
	)
	private float glowtone$unshade(
		float original,
		@Share("glowtone$shouldUnshade") LocalBooleanRef shouldUnshade
	) {
		return shouldUnshade.get() ? 1F : original;
	}
}
