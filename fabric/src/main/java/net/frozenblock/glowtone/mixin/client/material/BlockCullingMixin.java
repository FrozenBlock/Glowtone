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

package net.frozenblock.glowtone.mixin.client.material;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.frozenblock.glowtone.material.MaterialCulling;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;

@ClientOnly
@Mixin(Block.class)
public class BlockCullingMixin {

	@ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
	private static boolean glowtone$overrideFaceCulling(
		boolean automatic,
		@Local(argsOnly = true, ordinal = 0) BlockState state,
		@Local(argsOnly = true, ordinal = 1) BlockState neighborState
	) {
		return MaterialCulling.shouldRenderFace(state, neighborState, automatic);
	}
}
