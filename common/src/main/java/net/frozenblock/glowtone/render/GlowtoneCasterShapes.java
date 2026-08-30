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

package net.frozenblock.glowtone.render;

import net.frozenblock.glowtone.light.occlusion.OcclusionOverrideHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@ClientOnly
public final class GlowtoneCasterShapes {

	public static VoxelShape of(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.isAir()) return Shapes.empty();

		final VoxelShape automatic = automatic(level, pos, state);
		if (!OcclusionOverrideHelper.any()) return automatic;

		if (!OcclusionOverrideHelper.casts(state, !automatic.isEmpty())) return Shapes.empty();
		return automatic.isEmpty() ? state.getShape(level, pos) : automatic;
	}

	private static VoxelShape automatic(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		final VoxelShape occlusionShape = state.getOcclusionShape();
		if (!occlusionShape.isEmpty()) return occlusionShape;

		// TODO: block tag
		if (state.is(BlockTags.LEAVES) || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)) return state.getShape(level, pos);

		final VoxelShape collision = state.getCollisionShape(level, pos);
		if (collision.isEmpty() || Block.isShapeFullBlock(collision)) return Shapes.empty();

		return collision;
	}

	private GlowtoneCasterShapes() {}
}
