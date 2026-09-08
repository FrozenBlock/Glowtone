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

package net.frozenblock.glowtone.light.edge.impl;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface BlockStateCasterBoxCache {
	int GLOWTONE$CASTER_BOX_CACHE_LIMIT = 128;
	AABB[] GLOWTONE$NO_BOXES = {};
	AABB[] GLOWTONE$FULL_BOX = {Shapes.block().bounds()};
	Map<VoxelShape, AABB[]> SHAPE_TO_BOXES_CACHE = new HashMap<>(GLOWTONE$CASTER_BOX_CACHE_LIMIT);

	default @Nullable AABB[] glowtone$getOrCreateCasterBoxes(BlockGetter level, BlockPos pos) {
		throw new AssertionError();
	}

	default void glowtone$clearCasterBoxes() {
		throw new AssertionError();
	}

	static AABB[] glowtone$boxesFromShape(VoxelShape shape) {
		if (shape.isEmpty()) return GLOWTONE$NO_BOXES;
		if (shape == Shapes.block()) return GLOWTONE$FULL_BOX;

		AABB[] cached = SHAPE_TO_BOXES_CACHE.get(shape);
		if (cached == null) {
			if (SHAPE_TO_BOXES_CACHE.size() >= GLOWTONE$CASTER_BOX_CACHE_LIMIT) SHAPE_TO_BOXES_CACHE.clear();
			cached = shape.toAabbs().toArray(new AABB[0]);
			SHAPE_TO_BOXES_CACHE.put(shape, cached);
		}
		return cached;
	}
}
