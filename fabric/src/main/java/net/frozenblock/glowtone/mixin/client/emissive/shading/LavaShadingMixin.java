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

package net.frozenblock.glowtone.mixin.client.emissive.shading;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(FluidRenderer.class)
public class LavaShadingMixin {
	@Unique
	private static final CardinalLighting GLOWTONE$NO_CARDINAL_LIGHTING = new CardinalLighting(1F, 1F, 1F, 1F, 1F, 1F);

	@ModifyExpressionValue(
		method = "tesselate",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;cardinalLighting()Lnet/minecraft/world/level/CardinalLighting;"
		)
	)
	public CardinalLighting glowtone$unshadeIfApplicable(
		CardinalLighting original,
		@Local(argsOnly = true) FluidState fluidState
	) {
		if (GlowtoneConstants.GLOWTONE_NO_SHADING
			|| (GlowtoneConstants.GLOWTONE_SHADING && fluidState.is(FluidTags.LAVA))) {
			return GLOWTONE$NO_CARDINAL_LIGHTING;
		}
		return original;
	}
}
