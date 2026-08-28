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

import net.frozenblock.glowtone.render.light.color.ChromaBaker;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.QuadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.block.BlockModelLighter;

@Mixin(BlockModelLighter.class)
public class BlockModelLighterMixin {

	@Inject(method = "prepareQuadFlat", at = @At("TAIL"))
	private void glowtone$pinFlatQuadColour(
		BlockAndTintGetter level,
		BlockState state,
		BlockPos pos,
		int lightCoords,
		BakedQuad quad,
		QuadInstance outputInstance,
		CallbackInfo info
	) {
		final Direction direction = quad.direction();
		final BlockPos lit = direction == null ? pos : pos.relative(direction);
		ChromaBaker.beginFlatQuad(lit.getX(), lit.getY(), lit.getZ());
	}
}
