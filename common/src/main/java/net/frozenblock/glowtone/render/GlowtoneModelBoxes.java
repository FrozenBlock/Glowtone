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

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneModelBoxes {
	public static final float EPSILON = 1.0E-4F;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int STRIDE = 6;
	private static final int HEADER = 3;
	private static final int MAX_FACES = 512;
	private static final float[] NONE = new float[0];
	private static final Map<BlockState, float[]> CACHE = new ConcurrentHashMap<>();
	private static volatile boolean warnedBuildFailure;

	public static void clear() {
		CACHE.clear();
	}

	public static float[] forState(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, long seed) {
		final float[] cached = CACHE.get(state);
		if (cached != null) return cached;

		final float[] built = build(model, level, pos, state, seed);
		if (built == null) return NONE;

		CACHE.put(state, built);
		return built;
	}

	private static float @Nullable [] build(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, long seed) {
		final List<float[]> faces = new ArrayList<>();

		try {
			final RandomSource random = RandomSource.create(seed);
			final List<BlockStateModelPart> parts = new ArrayList<>();
			model.collectParts(random, parts);
			for (BlockStateModelPart part : parts) {
				collect(part.getQuads(null), faces);
				for (Direction direction : Direction.values()) collect(part.getQuads(direction), faces);
			}
		} catch (Exception e) {
			if (!warnedBuildFailure) {
				warnedBuildFailure = true;
				LOGGER.warn("Failed to collect model boxes for {}, ignoring further failures", state, e);
			}
			return null;
		}

		if (faces.isEmpty()) return NONE;

		int total = 0;
		for (float[] face : faces) {
			for (int axis = 0; axis < 3; axis++) {
				if (flatOn(face, axis)) total++;
			}
		}

		final float[] packed = new float[HEADER + total * STRIDE];
		int at = HEADER;
		for (int axis = 0; axis < 3; axis++) {
			for (float[] face : faces) {
				if (!flatOn(face, axis)) continue;

				System.arraycopy(face, 0, packed, at, STRIDE);
				at += STRIDE;
			}
			packed[axis] = at;
		}
		return packed;
	}

	private static boolean flatOn(float[] face, int axis) {
		return Math.abs(face[axis] - face[axis + 3]) <= EPSILON;
	}

	private static void collect(List<BakedQuad> quads, List<float[]> faces) {
		for (BakedQuad quad : quads) {
			if (faces.size() >= MAX_FACES) return;

			float minX = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			float minZ = Float.MAX_VALUE;
			float maxZ = -Float.MAX_VALUE;

			for (int vertex = 0; vertex < 4; vertex++) {
				final float x = quad.position(vertex).x();
				final float y = quad.position(vertex).y();
				final float z = quad.position(vertex).z();
				if (x < minX) minX = x;
				if (x > maxX) maxX = x;
				if (y < minY) minY = y;
				if (y > maxY) maxY = y;
				if (z < minZ) minZ = z;
				if (z > maxZ) maxZ = z;
			}

			faces.add(new float[]{minX, minY, minZ, maxX, maxY, maxZ});
		}
	}

	// TODO: why cant we use Direction.Axis?
	public static boolean continuesPast(float[] packed, int normalAxis, float plane, int alongAxis, float along, int edgeAxis, float across) {
		if (packed.length == 0) return false;

		final int end = (int) packed[normalAxis];
		for (int base = normalAxis == 0 ? HEADER : (int) packed[normalAxis - 1]; base < end; base += STRIDE) {
			if (Math.abs(packed[base + normalAxis] - plane) > EPSILON) continue;

			if (along < packed[base + alongAxis] - EPSILON) continue;
			if (along > packed[base + alongAxis + 3] + EPSILON) continue;
			if (across < packed[base + edgeAxis] - EPSILON) continue;
			if (across > packed[base + edgeAxis + 3] + EPSILON) continue;

			return true;
		}

		return false;
	}

	private GlowtoneModelBoxes() {}
}
