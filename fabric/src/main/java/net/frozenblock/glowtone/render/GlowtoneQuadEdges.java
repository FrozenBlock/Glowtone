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

import com.mojang.datafixers.util.Function5;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.frozenblock.glowtone.config.OcclusionStrengthOption;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class GlowtoneQuadEdges {
	public static final int UNITS_PER_BLOCK = 16;
	public static final int INTERIOR = 255;
	private static final float BOUNDARY_MARGIN = 1F / 2048F;
	private static final float PROBE = 1F / 32F;
	private static final float MATCH_EPSILON = 1F / 10000F;
	private static final float FLAT_EPSILON = 1F / 4096F;
	private static final int FULLY_LIT = 0xFFFF;
	public static final int NO_EDGES = (INTERIOR << 24) | (INTERIOR << 16) | (INTERIOR << 8) | INTERIOR;

	private final int[] vertices = {NO_EDGES, NO_EDGES, NO_EDGES, NO_EDGES};
	private final int[] masks = new int[4];
	private final int[] contact = new int[GlowtoneContactRects.WORDS];
	private final float[] positions = new float[12];
	private final GlowtoneContactRects contactRects = new GlowtoneContactRects();
	private int rimOriginX;
	private int rimOriginY;
	private int rimOriginZ;
	// TODO: why cant we use Direction.Axis?
	private int rimAxis;
	private boolean rimPositive;
	private boolean rimForceLit;
	private int rimNarrowAxis;
	// TODO: why cant we use Direction.Axis?
	private int normalAxis;
	private boolean normalPositive;

	public int get(int vertex) {
		return this.vertices[vertex];
	}

	public int mask(int vertex) {
		return this.masks[vertex];
	}

	public int contact(int word) {
		return this.contact[word];
	}

	public float positionX(int vertex) {
		return this.positions[vertex * 3];
	}

	public float positionY(int vertex) {
		return this.positions[vertex * 3 + 1];
	}

	public float positionZ(int vertex) {
		return this.positions[vertex * 3 + 2];
	}

	public int indexOf(float x, float y, float z) {
		for (int vertex = 0; vertex < 4; vertex++) {
			if (Math.abs(this.positions[vertex * 3] - x) < MATCH_EPSILON
				&& Math.abs(this.positions[vertex * 3 + 1] - y) < MATCH_EPSILON
				&& Math.abs(this.positions[vertex * 3 + 2] - z) < MATCH_EPSILON) {
				return vertex;
			}
		}
		return -1;
	}

	public int normalAxis() {
		return this.normalAxis;
	}

	public boolean normalPositive() {
		return this.normalPositive;
	}

	private void clear() {
		for (int vertex = 0; vertex < 4; vertex++) {
			this.vertices[vertex] = NO_EDGES;
			this.masks[vertex] = 0;
		}
		System.arraycopy(GlowtoneContactRects.NONE, 0, this.contact, 0, GlowtoneContactRects.WORDS);
	}

	public void set(
		MutableQuadView quad,
		GlowtoneEdgeNeighbours neighbours,
		boolean highlight,
		boolean shade,
		boolean bake
	) {
		for (int vertex = 0; vertex < 4; vertex++) {
			this.positions[vertex * 3] = quad.x(vertex);
			this.positions[vertex * 3 + 1] = quad.y(vertex);
			this.positions[vertex * 3 + 2] = quad.z(vertex);
		}

		build(quad, quad.lightFace(), neighbours, highlight, shade, bake, false);
	}

	public void setFluidRim(
		GlowtoneEdgeNeighbours neighbours,
		int originX, int originY, int originZ,
		int axis, boolean positive, int narrow,
		boolean forceLit,
		float x0, float y0, float z0,
		float x1, float y1, float z1,
		float x2, float y2, float z2,
		float x3, float y3, float z3
	) {
		this.rimOriginX = originX;
		this.rimOriginY = originY;
		this.rimOriginZ = originZ;
		this.rimAxis = axis;
		this.rimPositive = positive;
		this.rimNarrowAxis = narrow;
		this.rimForceLit = forceLit;
		this.positions[0] = x0;
		this.positions[1] = y0;
		this.positions[2] = z0;
		this.positions[3] = x1;
		this.positions[4] = y1;
		this.positions[5] = z1;
		this.positions[6] = x2;
		this.positions[7] = y2;
		this.positions[8] = z2;
		this.positions[9] = x3;
		this.positions[10] = y3;
		this.positions[11] = z3;

		build(null, null, neighbours, true, false, false, true);
	}

	public boolean anyLit() {
		for (int vertex = 0; vertex < 4; vertex++) {
			if (this.masks[vertex] != 0) return true;
		}
		return false;
	}

	private void build(
		@Nullable MutableQuadView quad,
		@Nullable Direction face,
		GlowtoneEdgeNeighbours neighbours,
		boolean highlight,
		boolean shade,
		boolean bake,
		boolean rim
	) {
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

		for (int vertex = 0; vertex < 4; vertex++) {
			final float x = this.positions[vertex * 3];
			final float y = this.positions[vertex * 3 + 1];
			final float z = this.positions[vertex * 3 + 2];
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

		final int normal;
		if (rim) {
			normal = this.rimAxis;
		} else if (spanX <= spanY && spanX <= spanZ) {
			normal = 0;
		} else if (spanY <= spanZ) {
			normal = 1;
		} else {
			normal = 2;
		}

		final float flat = normal == 0 ? spanX : normal == 1 ? spanY : spanZ;
		final int axisU = normal == 0 ? 1 : 0;
		final int axisV = normal == 2 ? 1 : 2;
		final float spanU = axisU == 0 ? spanX : axisU == 1 ? spanY : spanZ;
		final float spanV = axisV == 0 ? spanX : axisV == 1 ? spanY : spanZ;

		if (flat > FLAT_EPSILON || spanU <= FLAT_EPSILON || spanV <= FLAT_EPSILON) {
			clear();
			return;
		}

		final boolean positive;
		final float plane;
		if (rim) {
			final int origin = normal == 0
				? this.rimOriginX
				: normal == 1 ? this.rimOriginY : this.rimOriginZ;
			plane = mean(normal) - origin;
			positive = this.rimPositive;
		} else {
			positive = face != null && face.getAxis().ordinal() == normal
				? face.getAxisDirection() == Direction.AxisDirection.POSITIVE
				: facesPositive(normal);
			plane = localPlane(component(0, normal), positive);
		}
		this.normalAxis = normal;
		this.normalPositive = positive;

		final float minU = axisU == 0 ? minX : axisU == 1 ? minY : minZ;
		final float maxU = axisU == 0 ? maxX : axisU == 1 ? maxY : maxZ;
		final float minV = axisV == 0 ? minX : axisV == 1 ? minY : minZ;
		final float maxV = axisV == 0 ? maxX : axisV == 1 ? maxY : maxZ;

		final int originU = Mth.floor(minU);
		final int originV = Mth.floor(minV);

		final float lowU = minU - originU;
		final float highU = maxU - originU;
		final float lowV = minV - originV;
		final float highV = maxV - originV;

		final boolean shaded = (shade || bake) && !neighbours.selfEmissive();

		final float[] modelFaces = rim ? null : GlowtoneChromaBake.state().modelFaces();

		final int litLowU;
		final int litHighU;
		final int litLowV;
		final int litHighV;
		final Function5<Integer, Boolean, Float, Float, Float, Integer> edgePair = (edgeAxis, edgePositive, edgeCoord, alongMin, alongMax) -> {
			return edgePair(neighbours, normal, positive, plane, modelFaces, edgeAxis, edgePositive, edgeCoord, alongMin, alongMax, rim);
		};
		if (rim) {
			final boolean narrowIsU = axisU == this.rimNarrowAxis;
			final float narrowMid = narrowIsU ? (lowU + highU) * 0.5F : (lowV + highV) * 0.5F;
			final int lit = this.rimForceLit
				? FULLY_LIT
				: !highlight ? 0 : edgePair.apply(this.rimNarrowAxis, true, narrowMid, narrowIsU ? lowV : lowU, narrowIsU ? highV : highU);

			litLowU = litHighU = narrowIsU ? lit : 0;
			litLowV = litHighV = narrowIsU ? 0 : lit;
		} else {
			litLowU = !highlight ? 0 : edgePair.apply(axisU, false, lowU, lowV, highV);
			litHighU = !highlight ? 0 : edgePair.apply(axisU, true, highU, lowV, highV);
			litLowV = !highlight ? 0 : edgePair.apply(axisV, false, lowV, lowU, highU);
			litHighV = !highlight ? 0 : edgePair.apply(axisV, true, highV, lowU, highU);
		}

		final int[] built = !shaded ? GlowtoneContactRects.NONE
			: this.contactRects.build(neighbours, normal, positive, plane, axisU, lowU, highU, axisV, lowV, highV, !bake);
		final int[] contactPack = rim ? GlowtoneContactRects.FLUID
			: shade ? built : GlowtoneContactRects.NONE;
		final float bakeDepth = bake && shaded ? OcclusionStrengthOption.strength() : 0F;

		final float midU = (lowU + highU) * 0.5F;
		final float midV = (lowV + highV) * 0.5F;

		for (int vertex = 0; vertex < 4; vertex++) {
			final float u = component(vertex, axisU);
			final float v = component(vertex, axisV);
			final float localU = u - originU;
			final float localV = v - originV;

			final boolean nearLowU = localU <= midU;
			final boolean nearLowV = localV <= midV;

			if (quad != null && bakeDepth > 0F) {
				darken(quad, vertex, this.contactRects.occlusionAt((u - minU) * UNITS_PER_BLOCK, (v - minV) * UNITS_PER_BLOCK) * bakeDepth);
			}

			this.vertices[vertex] = rim
				? 0
				: (units(u - minU) << 24)
					| (units(maxU - u) << 16)
					| (units(v - minV) << 8)
					| units(maxV - v);

			this.masks[vertex] = packEdges(litLowU, litHighU, litLowV, litHighV, nearLowU, nearLowV);
		}

		System.arraycopy(contactPack, 0, this.contact, 0, GlowtoneContactRects.WORDS);
	}

	private static int packEdges(int lowU, int highU, int lowV, int highV, boolean nearLowU, boolean nearLowV) {
		return ((nearLowV ? lowU >>> 8 : lowU & 0xFF) << 24)
			| ((nearLowV ? highU >>> 8 : highU & 0xFF) << 16)
			| ((nearLowU ? lowV >>> 8 : lowV & 0xFF) << 8)
			| (nearLowU ? highV >>> 8 : highV & 0xFF);
	}

	private static void darken(MutableQuadView quad, int vertex, float amount) {
		final float factor = Math.max(0F, 1F - amount);
		final int colour = quad.color(vertex);

		quad.color(vertex, (colour & 0xFF000000)
			| (Math.round(((colour >> 16) & 0xFF) * factor) << 16)
			| (Math.round(((colour >> 8) & 0xFF) * factor) << 8)
			| Math.round((colour & 0xFF) * factor));
	}

	private boolean facesPositive(int axis) {
		final float ax = this.positions[6] - this.positions[0];
		final float ay = this.positions[7] - this.positions[1];
		final float az = this.positions[8] - this.positions[2];
		final float bx = this.positions[9] - this.positions[3];
		final float by = this.positions[10] - this.positions[4];
		final float bz = this.positions[11] - this.positions[5];

		return switch (axis) {
			case 0 -> ay * bz - az * by > 0F;
			case 1 -> az * bx - ax * bz > 0F;
			default -> ax * by - ay * bx > 0F;
		};
	}

	private static int units(float toEdge) {
		return Math.max(0, Math.min(INTERIOR, Math.round(toEdge * UNITS_PER_BLOCK)));
	}

	private static int edgePair(
		GlowtoneEdgeNeighbours neighbours,
		int normalAxis,
		boolean normalPositive,
		float plane,
		float @Nullable [] modelFaces,
		int edgeAxis,
		boolean edgePositive,
		float edgeCoord,
		float alongMin,
		float alongMax,
		boolean rim
	) {
		final Function<Float, Boolean> edgeState = along -> {
			return edgeState(neighbours, normalAxis, normalPositive, plane, modelFaces, edgeAxis, edgePositive, edgeCoord, along, alongMin, alongMax, rim);
		};
		final boolean atLow = edgeState.apply(alongMin);
		final boolean atHigh = edgeState.apply(alongMax);

		if (atLow == atHigh) return atLow ? 0xFFFF : 0x0000;
		if (alongMax - alongMin <= FLAT_EPSILON) return 0x0000;

		float lo = 0F;
		float hi = 1F;
		for (int step = 0; step < 10; step++) {
			final float mid = (lo + hi) * 0.5F;
			final float sample = alongMin + (alongMax - alongMin) * mid;
			final boolean here = edgeState.apply(sample);
			if (here == atLow) lo = mid; else hi = mid;
		}

		final float boundary = Mth.clamp((lo + hi) * 0.5F, BOUNDARY_MARGIN, 1F - BOUNDARY_MARGIN);

		final float low;
		final float high;
		if (atHigh) {
			if (boundary <= 0.5F) {
				low = (0.5F - boundary) / (1F - boundary);
				high = 1F;
			} else {
				low = 0F;
				high = 0.5F / boundary;
			}
		} else {
			if (boundary >= 0.5F) {
				low = 1F;
				high = 1F - 0.5F / boundary;
			} else {
				low = 0.5F / (1F - boundary);
				high = 0F;
			}
		}

		return (channelByte(low) << 8) | channelByte(high);
	}

	private static int channelByte(float value) {
		return Math.max(0, Math.min(255, Math.round(value * 255F)));
	}

	private static boolean edgeState(
		GlowtoneEdgeNeighbours neighbours,
		int normalAxis,
		boolean normalPositive,
		float plane,
		float @Nullable [] modelFaces,
		int edgeAxis,
		boolean edgePositive,
		float edgeCoord,
		float alongCoord,
		float alongMin,
		float alongMax,
		boolean rim
	) {
		final int alongAxis = 3 - normalAxis - edgeAxis;
		final float across = edgeCoord + (edgePositive ? PROBE : -PROBE);
		final float step = normalPositive ? PROBE : -PROBE;

		final float along = alongMax - alongMin > 2F * PROBE
			? Mth.clamp(alongCoord, alongMin + PROBE, alongMax - PROBE)
			: (alongMin + alongMax) * 0.5F;

		if (rim) return solid(neighbours, normalAxis, plane + step, edgeAxis, edgeCoord, alongAxis, along);

		final float[] faces = modelFaces;
		if (faces != null && faces.length > 0 && Mth.floor(across) == 0 && Mth.floor(plane - step) == 0) {
			return !GlowtoneModelBoxes.continuesPast(faces, normalAxis, plane, alongAxis, along, edgeAxis, across);
		}

		if (solid(neighbours, normalAxis, plane - step, edgeAxis, across, alongAxis, along)) return false;
		return !solid(neighbours, normalAxis, plane + step, edgeAxis, across, alongAxis, along);
	}

	private static boolean solid(
		GlowtoneEdgeNeighbours neighbours,
		int normalAxis,
		float normalCoord,
		int edgeAxis,
		float edgeCoord,
		int alongAxis,
		float alongCoord
	) {
		final int normalCell = Mth.clamp(Mth.floor(normalCoord), -1, 1);
		final int edgeCell = Mth.clamp(Mth.floor(edgeCoord), -1, 1);
		final int alongCell = Mth.clamp(Mth.floor(alongCoord), -1, 1);

		return neighbours.solidAt(
			normalAxis, normalCell, normalCoord - normalCell,
			edgeAxis, edgeCell, edgeCoord - edgeCell,
			alongAxis, alongCell, alongCoord - alongCell
		);
	}

	private static float localPlane(float plane, boolean positive) {
		final int origin = positive ? Mth.ceil(plane) - 1 : Mth.floor(plane);
		return plane - origin;
	}

	private float component(int vertex, int axis) {
		return this.positions[vertex * 3 + axis];
	}

	private float mean(int axis) {
		return (this.positions[axis] + this.positions[3 + axis] + this.positions[6 + axis] + this.positions[9 + axis]) * 0.25F;
	}
}
