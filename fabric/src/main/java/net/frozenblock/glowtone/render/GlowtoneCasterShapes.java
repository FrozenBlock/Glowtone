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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public final class GlowtoneCasterShapes {
	private GlowtoneCasterShapes() {}

	public static VoxelShape of(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.isAir()) return Shapes.empty();

		final VoxelShape occlusion = state.getOcclusionShape();
		if (!occlusion.isEmpty()) return occlusion;

		if (state.is(BlockTags.LEAVES) || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)) {
			return state.getShape(level, pos);
		}

		final VoxelShape collision = state.getCollisionShape(level, pos);
		if (collision.isEmpty() || Block.isShapeFullBlock(collision)) return Shapes.empty();

		return collision;
	}
}
