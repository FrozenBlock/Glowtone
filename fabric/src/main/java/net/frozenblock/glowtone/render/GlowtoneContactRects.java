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
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public final class GlowtoneContactRects {
	public static final int MAX_RECTS = 4;
	public static final int COORD_BITS = 6;
	private static final int COORD_MIN = -16;
	private static final int COORD_MAX = 32;
	public static final int WORDS = 4;
	public static final int GRID_NODES = 5;
	public static final int GRID_BITS = 5;
	public static final int GRID_FLAG = 1 << 31;
	private static final int EMPTY_LOW = encode(COORD_MAX);
	private static final int EMPTY_HIGH = encode(COORD_MIN);

	public static final int[] NONE = emptyPack();

	public static final int LIQUID_FLAG = 1 << 30;

	public static final int OCCUPIED_FLAG = 1 << 29;

	public static final int[] LIQUID = liquidPack();

	private static int[] liquidPack() {
		final int[] pack = emptyPack();
		pack[WORDS - 1] |= LIQUID_FLAG;
		return pack;
	}

	private static int[] emptyPack() {
		final int[] into = new int[WORDS];
		for (int slot = 0; slot < MAX_RECTS; slot++) {
			put(into, slot * 4, EMPTY_LOW);
			put(into, slot * 4 + 1, EMPTY_HIGH);
			put(into, slot * 4 + 2, EMPTY_LOW);
			put(into, slot * 4 + 3, EMPTY_HIGH);
		}
		return into;
	}

	private static void put(int[] into, int index, int value) {
		putBits(into, index * COORD_BITS, COORD_BITS, value);
	}

	private static void putBits(int[] into, int bit, int width, int value) {
		final int word = bit >>> 5;
		final int offset = bit & 31;
		into[word] |= value << offset;
		if (offset > 32 - width) into[word + 1] |= value >>> (32 - offset);
	}

	public static final float RADIUS_UNITS = 16F;

	private static final int CAPACITY = 32;
	private static final int TRIM = 16;
	private static final float PROBE = 1.0F / 32.0F;
	private static final float TOUCH_EPSILON = 1.0F / 4096.0F;
	private static final float MERGE_EPSILON = 1.0F / 256.0F;

	public static final float COVERAGE_SCALE = 2F;

	private static final int PIECES = 512;

	private final float[] rects = new float[CAPACITY * 4];
	private final int[] packed = new int[WORDS];
	private int count;

	private static final int CACHE_BITS = 12;
	private static final int CACHE_SLOTS = 1 << CACHE_BITS;
	private static final int CACHE_MASK = CACHE_SLOTS - 1;

	private final long[] cacheKeys = new long[CACHE_SLOTS];
	private final boolean[] cacheUsed = new boolean[CACHE_SLOTS];
	private final int[] cacheWords = new int[CACHE_SLOTS * WORDS];

	private final float[] pieces = new float[PIECES * 4];
	private final float[] edgesU = new float[CAPACITY * 2];
	private final float[] edgesV = new float[CAPACITY * 2];
	private int pieceCount;
	private boolean decomposed;

	public int[] build(
		GlowtoneEdgeNeighbours neighbours,
		int normalAxis, boolean normalPositive, float plane,
		int axisU, float minU, float maxU,
		int axisV, float minV, float maxV,
		boolean cacheable
	) {
		this.count = 0;
		this.decomposed = false;
		long signature = (((long) normalAxis * 31 + (normalPositive ? 1 : 0)) * 31
			+ Float.floatToRawIntBits(plane)) * 31
			+ Float.floatToRawIntBits(minU) * 7L + Float.floatToRawIntBits(maxU) * 13L
			+ Float.floatToRawIntBits(minV) * 17L + Float.floatToRawIntBits(maxV) * 19L;

		final float step = normalPositive ? PROBE : -PROBE;
		final float spanU = (maxU - minU) * GlowtoneQuadEdges.UNITS_PER_BLOCK;
		final float spanV = (maxV - minV) * GlowtoneQuadEdges.UNITS_PER_BLOCK;
		final float probe = plane + step;
		final int normalCell = Mth.clamp(Mth.floor(probe), -1, 1);
		final float probeLocal = probe - normalCell;

		for (int cellU = -1; cellU <= 1; cellU++) {
			for (int cellV = -1; cellV <= 1; cellV++) {
				final AABB[] boxes = neighbours.boxesAt(
					cellOn(0, normalAxis, normalCell, axisU, cellU, axisV, cellV),
					cellOn(1, normalAxis, normalCell, axisU, cellU, axisV, cellV),
					cellOn(2, normalAxis, normalCell, axisU, cellU, axisV, cellV)
				);
				signature = signature * 1099511628211L + System.identityHashCode(boxes);
				if (boxes == null || boxes.length == 0) continue;

				for (final AABB box : boxes) {
					if (min(box, normalAxis) - TOUCH_EPSILON > probeLocal
						|| max(box, normalAxis) + TOUCH_EPSILON < probeLocal) continue;

					add(
						texels(cellU + (float) min(box, axisU), minU),
						texels(cellU + (float) max(box, axisU), minU),
						texels(cellV + (float) min(box, axisV), minV),
						texels(cellV + (float) max(box, axisV), minV)
					);
				}
			}
		}

		final int slot = (int) (signature ^ (signature >>> 32)) & CACHE_MASK;
		if (cacheable && this.cacheUsed[slot] && this.cacheKeys[slot] == signature) {
			System.arraycopy(this.cacheWords, slot * WORDS, this.packed, 0, WORDS);
			this.count = 0;
			this.decomposed = false;
			return this.packed;
		}

		if (this.count > TRIM) keepNearest(spanU, spanV);

		merge();

		final int[] words = this.count <= MAX_RECTS ? pack() : packGrid(spanU, spanV);

		if (cacheable) {
			this.cacheKeys[slot] = signature;
			this.cacheUsed[slot] = true;
			System.arraycopy(words, 0, this.cacheWords, slot * WORDS, WORDS);
		}

		return words;
	}

	private static float texels(float coord, float origin) {
		return (coord - origin) * GlowtoneQuadEdges.UNITS_PER_BLOCK;
	}

	private void add(float u0, float u1, float v0, float v1) {
		if (u1 < COORD_MIN || u0 > COORD_MAX || v1 < COORD_MIN || v0 > COORD_MAX) return;
		if (this.count >= CAPACITY) return;

		final float lowU = Math.max(COORD_MIN, u0);
		final float highU = Math.min(COORD_MAX, u1);
		final float lowV = Math.max(COORD_MIN, v0);
		final float highV = Math.min(COORD_MAX, v1);

		for (int i = 0; i < this.count; i++) {
			if (contains(i, lowU, highU, lowV, highV)) return;
		}

		final int at = this.count++ * 4;
		this.rects[at] = lowU;
		this.rects[at + 1] = highU;
		this.rects[at + 2] = lowV;
		this.rects[at + 3] = highV;
	}

	private boolean contains(int index, float u0, float u1, float v0, float v1) {
		final int at = index * 4;
		return this.rects[at] <= u0 + TOUCH_EPSILON && this.rects[at + 1] >= u1 - TOUCH_EPSILON
			&& this.rects[at + 2] <= v0 + TOUCH_EPSILON && this.rects[at + 3] >= v1 - TOUCH_EPSILON;
	}

	private void merge() {
		for (int i = 0; i < this.count; i++) {
			int j = i + 1;
			while (j < this.count) {
				if (!joins(i, j)) {
					j++;
					continue;
				}

				final int a = i * 4;
				final int b = j * 4;
				this.rects[a] = Math.min(this.rects[a], this.rects[b]);
				this.rects[a + 1] = Math.max(this.rects[a + 1], this.rects[b + 1]);
				this.rects[a + 2] = Math.min(this.rects[a + 2], this.rects[b + 2]);
				this.rects[a + 3] = Math.max(this.rects[a + 3], this.rects[b + 3]);

				remove(j);
				j = i + 1;
			}
		}
	}

	private boolean joins(int i, int j) {
		final int a = i * 4;
		final int b = j * 4;

		final boolean sameV = near(this.rects[a + 2], this.rects[b + 2])
			&& near(this.rects[a + 3], this.rects[b + 3]);
		final boolean sameU = near(this.rects[a], this.rects[b])
			&& near(this.rects[a + 1], this.rects[b + 1]);
		final boolean touchU = this.rects[a] <= this.rects[b + 1] + MERGE_EPSILON
			&& this.rects[b] <= this.rects[a + 1] + MERGE_EPSILON;
		final boolean touchV = this.rects[a + 2] <= this.rects[b + 3] + MERGE_EPSILON
			&& this.rects[b + 2] <= this.rects[a + 3] + MERGE_EPSILON;

		return (sameV && touchU) || (sameU && touchV);
	}

	private void keepNearest(float spanU, float spanV) {
		for (int slot = 0; slot < TRIM; slot++) {
			int best = slot;
			float bestGap = gapTo(slot, spanU, spanV);
			for (int i = slot + 1; i < this.count; i++) {
				final float candidate = gapTo(i, spanU, spanV);
				if (candidate < bestGap) {
					best = i;
					bestGap = candidate;
				}
			}
			if (best != slot) swap(slot, best);
		}
		this.count = TRIM;
	}

	private float gapTo(int index, float spanU, float spanV) {
		final int at = index * 4;
		final float u = Math.max(0F, Math.max(this.rects[at] - spanU, -this.rects[at + 1]));
		final float v = Math.max(0F, Math.max(this.rects[at + 2] - spanV, -this.rects[at + 3]));
		return u * u + v * v;
	}

	private void remove(int index) {
		final int last = --this.count;
		if (index == last) return;
		System.arraycopy(this.rects, last * 4, this.rects, index * 4, 4);
	}

	private int[] packGrid(float spanU, float spanV) {
		Arrays.fill(this.packed, 0);

		for (int i = 0; i < GRID_NODES; i++) {
			final float u = i / (GRID_NODES - 1F) * spanU;

			for (int j = 0; j < GRID_NODES; j++) {
				final float occlusion = occlusionAt(u, j / (GRID_NODES - 1F) * spanV);
				final int level = Math.max(0, Math.min(31,
					Math.round(occlusion / COVERAGE_SCALE * 31F)));
				putBits(this.packed, (i * GRID_NODES + j) * GRID_BITS, GRID_BITS, level);
			}
		}

		this.packed[WORDS - 1] |= GRID_FLAG;
		this.packed[WORDS - 1] |= OCCUPIED_FLAG;
		return this.packed;
	}

	private void swap(int a, int b) {
		for (int c = 0; c < 4; c++) {
			final float held = this.rects[a * 4 + c];
			this.rects[a * 4 + c] = this.rects[b * 4 + c];
			this.rects[b * 4 + c] = held;
		}
	}

	public float occlusionAt(float u, float v) {
		ensureDecomposed();

		float covered = 0F;
		for (int i = 0; i < this.pieceCount; i++) {
			final int p = i * 4;
			covered += kernelSpan(this.pieces[p] - u, this.pieces[p + 1] - u)
				* kernelSpan(this.pieces[p + 2] - v, this.pieces[p + 3] - v);
		}
		return covered * COVERAGE_SCALE;
	}

	private static float kernelBelow(float t) {
		final float reach = Math.min(Math.abs(t) / RADIUS_UNITS, 1F);
		final float half = 0.5F * reach * (2F - reach);
		return t < 0F ? 0.5F - half : 0.5F + half;
	}

	private static float kernelSpan(float low, float high) {
		return Math.max(0F, kernelBelow(high) - kernelBelow(low));
	}

	private void ensureDecomposed() {
		if (this.decomposed) return;
		this.decomposed = true;
		decompose();
	}

	private void decompose() {
		this.pieceCount = 0;
		if (this.count == 0) return;

		int uCount = 0;
		int vCount = 0;
		for (int i = 0; i < this.count; i++) {
			final int r = i * 4;
			uCount = insert(this.edgesU, uCount, this.rects[r]);
			uCount = insert(this.edgesU, uCount, this.rects[r + 1]);
			vCount = insert(this.edgesV, vCount, this.rects[r + 2]);
			vCount = insert(this.edgesV, vCount, this.rects[r + 3]);
		}

		for (int row = 0; row + 1 < vCount; row++) {
			final float lowV = this.edgesV[row];
			final float highV = this.edgesV[row + 1];
			final float midV = (lowV + highV) * 0.5F;

			int column = 0;
			while (column + 1 < uCount) {
				if (!coveredAt((this.edgesU[column] + this.edgesU[column + 1]) * 0.5F, midV)) {
					column++;
					continue;
				}

				final float lowU = this.edgesU[column];
				while (column + 1 < uCount
					&& coveredAt((this.edgesU[column] + this.edgesU[column + 1]) * 0.5F, midV)) {
					column++;
				}
				addPiece(lowU, this.edgesU[column], lowV, highV);
			}
		}
	}

	private void addPiece(float lowU, float highU, float lowV, float highV) {
		for (int i = 0; i < this.pieceCount; i++) {
			final int p = i * 4;
			if (near(this.pieces[p], lowU) && near(this.pieces[p + 1], highU)
				&& near(this.pieces[p + 3], lowV)) {
				this.pieces[p + 3] = highV;
				return;
			}
		}
		if (this.pieceCount >= PIECES) return;

		final int at = this.pieceCount++ * 4;
		this.pieces[at] = lowU;
		this.pieces[at + 1] = highU;
		this.pieces[at + 2] = lowV;
		this.pieces[at + 3] = highV;
	}

	private boolean coveredAt(float u, float v) {
		for (int i = 0; i < this.count; i++) {
			final int r = i * 4;
			if (this.rects[r] <= u && u <= this.rects[r + 1]
				&& this.rects[r + 2] <= v && v <= this.rects[r + 3]) {
				return true;
			}
		}
		return false;
	}

	private static int insert(float[] into, int count, float value) {
		int at = 0;
		while (at < count && into[at] < value) at++;
		if (at < count && near(into[at], value)) return count;
		if (count >= into.length) return count;

		System.arraycopy(into, at, into, at + 1, count - at);
		into[at] = value;
		return count + 1;
	}

	private int[] pack() {
		Arrays.fill(this.packed, 0);

		for (int slot = 0; slot < MAX_RECTS; slot++) {
			final int at = slot * 4;
			if (slot >= this.count) {
				put(this.packed, at, EMPTY_LOW);
				put(this.packed, at + 1, EMPTY_HIGH);
				put(this.packed, at + 2, EMPTY_LOW);
				put(this.packed, at + 3, EMPTY_HIGH);
				continue;
			}
			for (int c = 0; c < 4; c++) {
				put(this.packed, at + c, encode(this.rects[slot * 4 + c]));
			}
		}

		if (this.count > 0) this.packed[WORDS - 1] |= OCCUPIED_FLAG;

		return this.packed;
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < MERGE_EPSILON;
	}

	private static int encode(float texels) {
		return Math.max(0, Math.min(COORD_MAX - COORD_MIN, Math.round(texels) - COORD_MIN));
	}

	private static final Direction.Axis[] AXES = Direction.Axis.values();

	private static double min(AABB box, int axis) {
		return box.min(AXES[axis]);
	}

	private static double max(AABB box, int axis) {
		return box.max(AXES[axis]);
	}

	private static int cellOn(int axis, int axisA, int cellA, int axisB, int cellB, int axisC, int cellC) {
		return axis == axisA ? cellA : axis == axisB ? cellB : axis == axisC ? cellC : 0;
	}
}
