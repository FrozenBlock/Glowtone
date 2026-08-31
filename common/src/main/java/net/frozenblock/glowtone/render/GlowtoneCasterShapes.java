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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@ClientOnly
public final class GlowtoneCasterShapes {

	public static VoxelShape of(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.isAir()) return Shapes.empty();

		final @Nullable VoxelShape automatic = automatic(level, pos, state);
		if (!OcclusionOverrideHelper.any()) return automatic != null ? automatic : Shapes.empty();

		if (!OcclusionOverrideHelper.casts(state, automatic != null)) return Shapes.empty();
		return automatic == null ? state.getShape(level, pos) : Shapes.empty();
	}

	@Nullable
	private static VoxelShape automatic(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.canOcclude()) {
			final VoxelShape occlusionShape = state.getOcclusionShape();
			if (!occlusionShape.isEmpty()) return occlusionShape;
		}

		// TODO: block tag & attachment
		if (state.is(BlockTags.LEAVES) || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)) return state.getShape(level, pos);

		// Unfortunately we cannot safely simplify this using .hasCollision, as someone could extend getCollisionShape and ignore the property.
		final VoxelShape collision = state.getCollisionShape(level, pos);
		if (collision.isEmpty() || fullBlock(collision)) return null;

		return collision;
	}

	private static boolean fullBlock(VoxelShape shape) {
		if (shape == Shapes.block()) return true;

		final AABB bounds = shape.bounds();
		if (bounds.minX > 0D || bounds.minY > 0D || bounds.minZ > 0D
			|| bounds.maxX < 1D || bounds.maxY < 1D || bounds.maxZ < 1D) {
			return false;
		}

		return Block.isShapeFullBlock(shape);
	}

	private GlowtoneCasterShapes() {}
}
