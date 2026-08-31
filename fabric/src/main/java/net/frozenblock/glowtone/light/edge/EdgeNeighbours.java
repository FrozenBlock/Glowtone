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

package net.frozenblock.glowtone.light.edge;

import net.frozenblock.glowtone.light.BlockLightPropertiesRenderer;
import net.frozenblock.glowtone.light.occlusion.OcclusionOverrideHelper;
import net.frozenblock.glowtone.render.GlowtoneCasterShapes;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@ClientOnly
public final class EdgeNeighbours {
	private static final double SPAN_SLACK = 1D / 256D;
	private static final float CONTAINS_SLACK = 1.0E-4F;
	private static final int CACHE_LIMIT = 512;
	private static final int CENTRE = 13;
	private static final AABB[] NONE = {};
	private static final AABB[] FULL = {Shapes.block().bounds()};

	private final Map<VoxelShape, AABB[]> boxCache = new IdentityHashMap<>();
	private final Map<BlockState, AABB[]> stateCache = new IdentityHashMap<>();
	private int cacheGeneration = -1;
	private final AABB[][] cells = new AABB[27][];
	private int resolved;
	private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
	private @Nullable BlockAndTintGetter source;
	private boolean receivesOcclusion = true;
	private boolean dirty = true;
	private int x;
	private int y;
	private int z;

	public void markDirty() {
		this.dirty = true;
	}

	public void gather(BlockAndTintGetter level, BlockPos pos) {
		if (!this.dirty && this.source == level && this.x == pos.getX() && this.y == pos.getY() && this.z == pos.getZ()) return;

		final BlockState here = level.getBlockState(pos);
		this.receivesOcclusion = OcclusionOverrideHelper.receives(here, here.getLightEmission() == 0);

		this.resolved = 1 << CENTRE;
		this.cells[CENTRE] = casterBoxes(level, pos, here);

		this.source = level;
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.dirty = false;
	}

	public boolean solidAt(int axisA, int cellA, float localA, int axisB, int cellB, float localB, int axisC, int cellC, float localC) {
		if (this.dirty) return false;

		final AABB[] boxes = boxesAt(
			cellOn(0, axisA, cellA, axisB, cellB, axisC, cellC),
			cellOn(1, axisA, cellA, axisB, cellB, axisC, cellC),
			cellOn(2, axisA, cellA, axisB, cellB, axisC, cellC)
		);
		if (boxes == null || boxes.length == 0) return false;
		if (boxes == FULL) return true;

		final float x = axisA == 0 ? localA : axisB == 0 ? localB : localC;
		final float y = axisA == 1 ? localA : axisB == 1 ? localB : localC;
		final float z = axisA == 2 ? localA : axisB == 2 ? localB : localC;

		final double xLow = x - CONTAINS_SLACK;
		final double xHigh = x + CONTAINS_SLACK;
		final double yLow = y - CONTAINS_SLACK;
		final double yHigh = y + CONTAINS_SLACK;
		final double zLow = z - CONTAINS_SLACK;
		final double zHigh = z + CONTAINS_SLACK;

		for (final AABB box : boxes) {
			if (xHigh < box.minX || xLow > box.maxX) continue;
			if (yHigh < box.minY || yLow > box.maxY) continue;
			if (zHigh < box.minZ || zLow > box.maxZ) continue;
			return true;
		}
		return false;
	}

	private AABB[] casterBoxes(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.getBlock().hasDynamicShape()) return boxesOf(GlowtoneCasterShapes.of(level, pos, state));

		final int generation = BlockLightPropertiesRenderer.generation();
		if (generation != this.cacheGeneration) {
			this.stateCache.clear();
			this.cacheGeneration = generation;
		}

		AABB[] cached = this.stateCache.get(state);
		if (cached == null) {
			if (this.stateCache.size() >= CACHE_LIMIT) this.stateCache.clear();
			cached = boxesOf(GlowtoneCasterShapes.of(level, pos, state));
			this.stateCache.put(state, cached);
		}
		return cached;
	}

	private AABB[] boxesOf(VoxelShape shape) {
		if (shape.isEmpty()) return NONE;
		if (shape == Shapes.block()) return FULL;

		AABB[] cached = this.boxCache.get(shape);
		if (cached == null) {
			if (this.boxCache.size() >= CACHE_LIMIT) this.boxCache.clear();
			cached = shape.toAabbs().toArray(new AABB[0]);
			this.boxCache.put(shape, cached);
		}
		return cached;
	}

	public AABB @Nullable [] boxesAt(int cellX, int cellY, int cellZ) {
		if (this.dirty) return null;
		if (cellX < -1 || cellX > 1 || cellY < -1 || cellY > 1 || cellZ < -1 || cellZ > 1) return null;

		final int slot = (cellX + 1) + 3 * (cellY + 1) + 9 * (cellZ + 1);
		final int bit = 1 << slot;
		if ((this.resolved & bit) != 0) return this.cells[slot];

		final BlockAndTintGetter level = this.source;
		if (level == null) return null;

		this.scratchPos.set(this.x + cellX, this.y + cellY, this.z + cellZ);
		this.cells[slot] = casterBoxes(level, this.scratchPos, level.getBlockState(this.scratchPos));
		this.resolved |= bit;

		return this.cells[slot];
	}

	public boolean receivesOcclusion() {
		return !this.dirty && this.receivesOcclusion;
	}

	private static int cellOn(int axis, int axisA, int cellA, int axisB, int cellB, int axisC, int cellC) {
		return axis == axisA ? cellA
			: axis == axisB ? cellB
			: axis == axisC ? cellC
			: 0;
	}

	public static boolean isBlockLike(VoxelShape shape) {
		if (shape.isEmpty()) return false;
		if (shape == Shapes.block()) return true;

		final AABB bounds = shape.bounds();
		return bounds.minX <= SPAN_SLACK && bounds.maxX >= 1D - SPAN_SLACK
			&& bounds.minZ <= SPAN_SLACK && bounds.maxZ >= 1D - SPAN_SLACK;
	}
}
