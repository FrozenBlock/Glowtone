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

package net.frozenblock.glowtone.render.light.edge;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.render.light.color.ChromaBaker;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class FluidEdges {
	private static final float WIDTH = 1F / QuadEdges.UNITS_PER_BLOCK;
	private static final float CLEARANCE = 0.001F;
	private static final float WRAP_CLEARANCE = 1F / 512F;
	private static final float MATCH = 1F / 10000F;
	private static final float FLOOR = 1F / 20F;
	private static final float TOUCH = 1F / 4096F;

	private final float[] corners = new float[12];
	private final float[] held = new float[12];
	private final float[] rimUvs = new float[8];
	private final int[] grid = new int[4];
	private final float[] reach = new float[2];
	private int faceU;
	private int faceV;
	private float uvMinU;
	private float uvMinV;
	private float uvSpanU;
	private float uvSpanV;
	private final float[] uvs = new float[8];
	private final float[] rim = new float[12];
	private int colour;
	private int light;
	// TODO: why cant we use Direction.Axis?
	private int faceAxis;
	private boolean facePositive;

	public void quad(
		float x0, float y0, float z0, float u0, float v0,
		float x1, float y1, float z1, float u1, float v1,
		float x2, float y2, float z2, float u2, float v2,
		float x3, float y3, float z3, float u3, float v3,
		int colour,
		int light
	) {
		this.corners[0] = x0;
		this.corners[1] = y0;
		this.corners[2] = z0;
		this.corners[3] = x1;
		this.corners[4] = y1;
		this.corners[5] = z1;
		this.corners[6] = x2;
		this.corners[7] = y2;
		this.corners[8] = z2;
		this.corners[9] = x3;
		this.corners[10] = y3;
		this.corners[11] = z3;
		this.uvs[0] = u0;
		this.uvs[1] = v0;
		this.uvs[2] = u1;
		this.uvs[3] = v1;
		this.uvs[4] = u2;
		this.uvs[5] = v2;
		this.uvs[6] = u3;
		this.uvs[7] = v3;
		this.colour = colour;
		this.light = light;
	}

	// FIXME: more descriptive method name or docs
	public boolean locate(int originX, int originY, int originZ) {
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

		for (int corner = 0; corner < 4; corner++) {
			final float x = this.corners[corner * 3];
			final float y = this.corners[corner * 3 + 1];
			final float z = this.corners[corner * 3 + 2];
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
			if (z < minZ) minZ = z;
			if (z > maxZ) maxZ = z;
		}

		final float spanX = maxX - minX;
		final float spanY = maxY - minY;
		final float spanZ = maxZ - minZ;

		final int axis;
		final float middle;
		if (spanX <= spanY && spanX <= spanZ) {
			axis = 0;
			middle = (minX + maxX) * 0.5F - originX;
			if (spanY <= MATCH || spanZ <= MATCH) return false;
		} else if (spanY <= spanZ) {
			axis = 1;
			middle = (minY + maxY) * 0.5F - originY;
			if (spanX <= MATCH || spanZ <= MATCH) return false;
		} else {
			axis = 2;
			middle = (minZ + maxZ) * 0.5F - originZ;
			if (spanX <= MATCH || spanY <= MATCH) return false;
		}

		this.faceAxis = axis;
		this.facePositive = axis == 1 ? middle > FLOOR : middle > 0.5F;
		return true;
	}

	public void emit(
		ChromaBaker.SectionState state,
		VertexConsumer consumer,
		EdgeNeighbours neighbours,
		int originX, int originY, int originZ
	) {
		frame();

		for (int axis = 0; axis < 3; axis++) {
			if (axis == this.faceAxis) continue;
			strip(state, consumer, neighbours, originX, originY, originZ, axis, false, false);
			strip(state, consumer, neighbours, originX, originY, originZ, axis, true, false);
		}

		if (this.faceAxis == 1 && this.facePositive) {
			pierced(state, consumer, neighbours, originX, originY, originZ);
		}
	}

	// FIXME: more descriptive method name or docs
	private void pierced(
		ChromaBaker.SectionState state,
		VertexConsumer consumer,
		EdgeNeighbours neighbours,
		int originX, int originY, int originZ
	) {
		final AABB[] boxes = neighbours.boxesAt(0, 0, 0);
		if (boxes == null || boxes.length == 0) return;

		final int face = this.faceAxis;
		final int axisU = face == 0 ? 1 : 0;
		final int axisV = face == 2 ? 1 : 2;
		final int originU = axisU == 0 ? originX : axisU == 1 ? originY : originZ;
		final int originV = axisV == 0 ? originX : axisV == 1 ? originY : originZ;

		float surface = 0F;
		float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
		float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
		for (int corner = 0; corner < 4; corner++) {
			surface += this.corners[corner * 3 + face] * 0.25F;
			final float u = this.corners[corner * 3 + axisU];
			final float v = this.corners[corner * 3 + axisV];
			if (u < minU) minU = u;
			if (u > maxU) maxU = u;
			if (v < minV) minV = v;
			if (v > maxV) maxV = v;
		}

		final float local = surface - (face == 0 ? originX : face == 1 ? originY : originZ);
		System.arraycopy(this.corners, 0, this.held, 0, 12);

		for (final AABB box : boxes) {
			if (low(box, face) - TOUCH > local || high(box, face) + TOUCH < local) continue;

			final float lowU = Math.max(minU, originU + low(box, axisU));
			final float highU = Math.min(maxU, originU + high(box, axisU));
			final float lowV = Math.max(minV, originV + low(box, axisV));
			final float highV = Math.min(maxV, originV + high(box, axisV));
			if (highU - lowU <= MATCH || highV - lowV <= MATCH) continue;

			if (lowU - minU <= MATCH && maxU - highU <= MATCH
				&& lowV - minV <= MATCH && maxV - highV <= MATCH) continue;

			footprint(face, surface, axisU, lowU, highU, axisV, lowV, highV);
			for (int axis = 0; axis < 3; axis++) {
				if (axis == face) continue;
				strip(state, consumer, neighbours, originX, originY, originZ, axis, false, true);
				strip(state, consumer, neighbours, originX, originY, originZ, axis, true, true);
			}
		}

		System.arraycopy(this.held, 0, this.corners, 0, 12);
	}

	private float blend(float s, float t, int part) {
		final float low = this.uvs[this.grid[0] * 2 + part] * (1F - t)
			+ this.uvs[this.grid[1] * 2 + part] * t;
		final float high = this.uvs[this.grid[2] * 2 + part] * (1F - t)
			+ this.uvs[this.grid[3] * 2 + part] * t;

		return low * (1F - s) + high * s;
	}

	// FIXME: more descriptive method name or docs
	private void footprint(int face, float plane, int axisU, float lowU, float highU, int axisV, float lowV, float highV) {
		for (int corner = 0; corner < 4; corner++) {
			this.corners[corner * 3 + face] = plane;
			this.corners[corner * 3 + axisU] = corner == 0 || corner == 3 ? lowU : highU;
			this.corners[corner * 3 + axisV] = corner < 2 ? lowV : highV;
		}
	}

	private static float low(AABB box, int axis) {
		return (float) (axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ);
	}

	private static float high(AABB box, int axis) {
		return (float) (axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ);
	}

	// FIXME: more descriptive method name or docs
	private void strip(
		ChromaBaker.SectionState state,
		VertexConsumer consumer,
		EdgeNeighbours neighbours,
		int originX, int originY, int originZ,
		int axis,
		boolean positive,
		boolean around
	) {
		final int face = this.faceAxis;
		final int along = 3 - axis - face;

		float bound = positive ? -Float.MAX_VALUE : Float.MAX_VALUE;
		for (int corner = 0; corner < 4; corner++) {
			final float value = this.corners[corner * 3 + axis];
			bound = positive ? Math.max(bound, value) : Math.min(bound, value);
		}

		int low = -1;
		int high = -1;
		for (int corner = 0; corner < 4; corner++) {
			if (Math.abs(this.corners[corner * 3 + axis] - bound) > MATCH) continue;
			if (low < 0) low = corner;
			else if (high < 0) high = corner;
		}
		if (high < 0) return;
		if (this.corners[low * 3 + along] > this.corners[high * 3 + along]) {
			final int swap = low;
			low = high;
			high = swap;
		}

		final float clearance = around ? WRAP_CLEARANCE : CLEARANCE;
		final float plane = bound + (positive != around ? -clearance : clearance);
		float alongLow = this.corners[low * 3 + along];
		float alongHigh = this.corners[high * 3 + along];
		final float outerLow = this.corners[low * 3 + face];
		final float outerHigh = this.corners[high * 3 + face];
		final int origin = face == 0 ? originX : face == 1 ? originY : originZ;
		final float innerLow = inwards(outerLow, origin);
		final float innerHigh = inwards(outerHigh, origin);
		if (Math.abs(outerLow - innerLow) <= MATCH && Math.abs(outerHigh - innerHigh) <= MATCH) return;

		boolean lit = around;
		if (!around && this.faceAxis == 1 && this.facePositive) {
			final int originAlong = along == 0 ? originX : along == 1 ? originY : originZ;
			if (!backing(neighbours, axis, positive, outerLow - origin, along, originAlong)) return;

			alongLow = Math.max(alongLow, this.reach[0]);
			alongHigh = Math.min(alongHigh, this.reach[1]);
			if (alongHigh - alongLow <= MATCH) return;

			lit = true;
		}

		put(0, axis, plane, face, innerLow, along, alongLow);
		put(1, axis, plane, face, innerHigh, along, alongHigh);
		put(2, axis, plane, face, outerHigh, along, alongHigh);
		put(3, axis, plane, face, outerLow, along, alongLow);

		uv(0, axis, plane, along, alongLow, innerLow - outerLow);
		uv(1, axis, plane, along, alongHigh, innerHigh - outerHigh);
		uv(2, axis, plane, along, alongHigh, 0F);
		uv(3, axis, plane, along, alongLow, 0F);

		if (normalOn(axis) > 0F == positive != around) swap(1, 3);

		state.pendingEdges().setFluidRim(
			neighbours, originX, originY, originZ, axis, positive, face, lit,
			this.rim[0], this.rim[1], this.rim[2],
			this.rim[3], this.rim[4], this.rim[5],
			this.rim[6], this.rim[7], this.rim[8],
			this.rim[9], this.rim[10], this.rim[11]
		);
		if (!state.pendingEdges().anyLit()) return;

		state.beginFluidQuadEdges();
		for (int corner = 0; corner < 4; corner++) {
			vertex(consumer, corner, axis, positive);
		}
		state.endFluidQuad();

		if (!around) return;

		state.beginFluidQuadEdges();
		for (int corner = 3; corner >= 0; corner--) {
			vertex(consumer, corner, axis, !positive);
		}
		state.endFluidQuad();
	}

	private boolean backing(EdgeNeighbours neighbours, int axis, boolean positive, float local, int along, int originAlong) {
		this.reach[0] = Float.MAX_VALUE;
		this.reach[1] = -Float.MAX_VALUE;

		final int step = positive ? 1 : -1;
		gather(neighbours.boxesAt(
			axis == 0 ? step : 0, axis == 1 ? step : 0, axis == 2 ? step : 0),
			axis, positive ? 0F : 1F, local, along, originAlong);
		gather(neighbours.boxesAt(0, 0, 0), axis, positive ? 1F : 0F, local, along, originAlong);

		return this.reach[0] <= this.reach[1];
	}

	private void gather(AABB @Nullable [] boxes, int axis, float side, float local, int along, int originAlong) {
		if (boxes == null) return;

		for (final AABB box : boxes) {
			if (low(box, this.faceAxis) - TOUCH > local) continue;
			if (high(box, this.faceAxis) + TOUCH < local) continue;
			if (low(box, axis) - TOUCH > side || high(box, axis) + TOUCH < side) continue;

			this.reach[0] = Math.min(this.reach[0], originAlong + low(box, along));
			this.reach[1] = Math.max(this.reach[1], originAlong + high(box, along));
		}
	}

	private float inwards(float outer, int origin) {
		return this.facePositive
			? Math.max(outer - WIDTH, origin)
			: Math.min(outer + WIDTH, origin + 1F);
	}

	private void frame() {
		final int axisU = this.faceAxis == 0 ? 1 : 0;
		final int axisV = this.faceAxis == 2 ? 1 : 2;
		this.faceU = axisU;
		this.faceV = axisV;

		float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
		float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
		for (int corner = 0; corner < 4; corner++) {
			final float u = this.corners[corner * 3 + axisU];
			final float v = this.corners[corner * 3 + axisV];
			if (u < minU) minU = u;
			if (u > maxU) maxU = u;
			if (v < minV) minV = v;
			if (v > maxV) maxV = v;
		}

		this.uvMinU = minU;
		this.uvMinV = minV;
		this.uvSpanU = maxU - minU;
		this.uvSpanV = maxV - minV;

		for (int corner = 0; corner < 4; corner++) {
			final int atU = this.corners[corner * 3 + axisU] - minU > this.uvSpanU * 0.5F ? 2 : 0;
			final int atV = this.corners[corner * 3 + axisV] - minV > this.uvSpanV * 0.5F ? 1 : 0;
			this.grid[atU + atV] = corner;
		}
	}

	private void uv(int corner, int axis, float axisCoord, int along, float alongCoord, float depth) {
		final float atU = axis == this.faceU ? axisCoord : alongCoord;
		final float atV = axis == this.faceV ? axisCoord : alongCoord;
		final float s = this.uvSpanU <= MATCH ? 0F : (atU - this.uvMinU) / this.uvSpanU;
		final float t = this.uvSpanV <= MATCH ? 0F : (atV - this.uvMinV) / this.uvSpanV;

		for (int part = 0; part < 2; part++) {
			this.rimUvs[corner * 2 + part] = blend(s, t, part) + rate(axis, part) * depth;
		}
	}

	private float rate(int axis, int part) {
		final boolean alongU = axis == this.faceU;
		final float span = alongU ? this.uvSpanU : this.uvSpanV;
		if (span <= MATCH) return 0F;

		final int from = this.grid[0];
		final int to = alongU ? this.grid[2] : this.grid[1];

		return (this.uvs[to * 2 + part] - this.uvs[from * 2 + part]) / span;
	}

	private void put(
		int corner, int axis, float plane, int face, float depth, int along, float distance
	) {
		this.rim[corner * 3 + axis] = plane;
		this.rim[corner * 3 + face] = depth;
		this.rim[corner * 3 + along] = distance;
	}

	private void swap(int a, int b) {
		for (int part = 0; part < 3; part++) {
			final float held = this.rim[a * 3 + part];
			this.rim[a * 3 + part] = this.rim[b * 3 + part];
			this.rim[b * 3 + part] = held;
		}

		for (int part = 0; part < 2; part++) {
			final float uv = this.rimUvs[a * 2 + part];
			this.rimUvs[a * 2 + part] = this.rimUvs[b * 2 + part];
			this.rimUvs[b * 2 + part] = uv;
		}
	}

	private float normalOn(int axis) {
		final float ax = this.rim[6] - this.rim[0];
		final float ay = this.rim[7] - this.rim[1];
		final float az = this.rim[8] - this.rim[2];
		final float bx = this.rim[9] - this.rim[3];
		final float by = this.rim[10] - this.rim[4];
		final float bz = this.rim[11] - this.rim[5];

		return switch (axis) {
			case 0 -> ay * bz - az * by;
			case 1 -> az * bx - ax * bz;
			default -> ax * by - ay * bx;
		};
	}

	private void vertex(VertexConsumer consumer, int corner, int axis, boolean positive) {
		final float sign = positive ? -1F : 1F;

		consumer.addVertex(
			this.rim[corner * 3], this.rim[corner * 3 + 1], this.rim[corner * 3 + 2],
			this.colour, this.rimUvs[corner * 2], this.rimUvs[corner * 2 + 1],
			OverlayTexture.NO_OVERLAY, this.light,
			axis == 0 ? sign : 0F, axis == 1 ? sign : 0F, axis == 2 ? sign : 0F
		);
	}
}
