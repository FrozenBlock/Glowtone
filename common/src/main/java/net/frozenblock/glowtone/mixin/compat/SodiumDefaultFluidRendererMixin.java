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

package net.frozenblock.glowtone.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public class SodiumDefaultFluidRendererMixin {
	@ModifyVariable(method = "updateQuad", at = @At("HEAD"), argsOnly = true, require = 0)
	private float glowtone$unshadeLava(float brightness, @Local(argsOnly = true) FluidState fluidState) {
		if (GlowtoneConstants.GLOWTONE_SHADING && fluidState.is(FluidTags.LAVA)) return 1.0F;
		return brightness;
	}
}
