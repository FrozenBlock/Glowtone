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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public final class GlowtoneModelBoxes {
	public static final float EPSILON = 1.0E-4F;
	private static final int STRIDE = 6;
	private static final int MAX_FACES = 512;
	private static final float[] NONE = new float[0];
	private static final Map<BlockState, float[]> CACHE = new ConcurrentHashMap<>();

	public static float[] forState(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, long seed) {
		final float[] cached = CACHE.get(state);
		if (cached != null) return cached;

		final float[] built = build(model, level, pos, state, seed);
		CACHE.put(state, built);
		return built;
	}

	private static float[] build(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, long seed) {
		final List<float[]> faces = new ArrayList<>();

		try {
			final RandomSource random = RandomSource.create(seed);
			final QuadEmitter collector = Renderer.get().quadEmitter(quad -> {
				if (faces.size() >= MAX_FACES) return;

				float minX = Float.MAX_VALUE;
				float maxX = -Float.MAX_VALUE;
				float minY = Float.MAX_VALUE;
				float maxY = -Float.MAX_VALUE;
				float minZ = Float.MAX_VALUE;
				float maxZ = -Float.MAX_VALUE;

				for (int vertex = 0; vertex < 4; vertex++) {
					final float x = quad.x(vertex);
					final float y = quad.y(vertex);
					final float z = quad.z(vertex);
					if (x < minX) minX = x;
					if (x > maxX) maxX = x;
					if (y < minY) minY = y;
					if (y > maxY) maxY = y;
					if (z < minZ) minZ = z;
					if (z > maxZ) maxZ = z;
				}

				faces.add(new float[]{minX, minY, minZ, maxX, maxY, maxZ});
			});

			model.emitQuads(collector, level, pos, state, random, direction -> false);
		} catch (Exception ignored) {
			return NONE;
		}

		if (faces.isEmpty()) return NONE;

		final float[] packed = new float[faces.size() * STRIDE];
		for (int i = 0; i < faces.size(); i++) {
			System.arraycopy(faces.get(i), 0, packed, i * STRIDE, STRIDE);
		}
		return packed;
	}

	// TODO: why cant we use Direction.Axis?
	public static boolean continuesPast(float[] packed, int normalAxis, float plane, int alongAxis, float along, int edgeAxis, float across) {
		if (packed.length == 0) return false;

		for (int base = 0; base < packed.length; base += STRIDE) {
			final float minN = packed[base + normalAxis];
			final float maxN = packed[base + normalAxis + 3];
			if (Math.abs(minN - maxN) > EPSILON) continue;
			if (Math.abs(minN - plane) > EPSILON) continue;

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
