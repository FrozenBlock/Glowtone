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

import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.minecraft.client.Minecraft;
import net.frozenblock.glowtone.light.GlowtoneRegionFlood;
import net.frozenblock.glowtone.light.color.GlowtoneTransmittance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class GlowtoneChromaBake {
	public static final int NEUTRAL_ARGB = GlowtoneChromaBlend.NEUTRAL_TERRAIN_ARGB;
	public static final int NEUTRAL_SKY_ARGB = 0xFFFFFFFF;
	private static final int NO_PIN = 0;

	private static final int OPAQUE_DAMPENING = 15;

	private static volatile boolean smoothLightingEnabled = true;

	private static final ThreadLocal<SectionState> STATE = ThreadLocal.withInitial(SectionState::new);

	private GlowtoneChromaBake() {
		throw new UnsupportedOperationException("GlowtoneChromaBake is a static holder.");
	}

	public static void beginSection(SectionPos sectionPos, @Nullable RenderSectionRegion region) {
		final SectionState state = STATE.get();
		if (!GlowtoneConfig.colouredLighting().isEnabled()) {
			state.begin(sectionPos, null);
			return;
		}
		state.begin(sectionPos, region);
	}

	public static void endSection() {
		STATE.get().end();
	}

	public static boolean buildingSection() {
		return STATE.get().building();
	}

	public static void rotateFlatPins() {
		STATE.get().rotateFlatPins();
	}

	public static int sampleArgb(float x, float y, float z) {
		return STATE.get().sample(x, y, z);
	}

	public static int sampleSkyArgb(float x, float y, float z) {
		return STATE.get().sampleSky(x, y, z);
	}

	public static boolean smoothLightingEnabled() {
		return smoothLightingEnabled;
	}

	public static SectionState state() {
		return STATE.get();
	}

	public static void beginFlatQuad(int worldX, int worldY, int worldZ) {
		STATE.get().beginFlatQuad(worldX, worldY, worldZ);
	}

	public static void beginFlatQuadLocal(float localX, float localY, float localZ) {
		STATE.get().beginFlatQuadLocal(localX, localY, localZ);
	}

	public static short @Nullable [] takeSectionColors() {
		return STATE.get().takePendingColors();
	}

	public static short @Nullable [] takeSectionSkyColors() {
		return STATE.get().takePendingSkyColors();
	}

	public static final class SectionState {
		private static final int CORNER_MIN = -1;
		private static final int CORNER_SPAN = 19;

		private final int[] cornerCache = new int[CORNER_SPAN * CORNER_SPAN * CORNER_SPAN];
		private final int[] skyCornerCache = new int[CORNER_SPAN * CORNER_SPAN * CORNER_SPAN];

		private @Nullable GlowtoneRegionFlood flood;

		private short @Nullable [] pendingColors;
		private short @Nullable [] pendingSkyColors;

		private boolean smoothLighting;
		private int flatChroma;
		private int flatVerticesLeft;
		private int flatSkyChroma;
		private int flatSkyVerticesLeft;
		private int latchedChroma = NO_PIN;
		private int latchedSkyChroma = NO_PIN;
		private int usedChroma = NO_PIN;
		private final GlowtoneQuadEdges pendingEdges = new GlowtoneQuadEdges();
		private final GlowtoneEdgeNeighbours edgeNeighbours = new GlowtoneEdgeNeighbours();
		private float @org.jspecify.annotations.Nullable [] modelFaces;
		private final GlowtoneLiquidRims liquidRims = new GlowtoneLiquidRims();
		private @Nullable BlockAndTintGetter liquidLevel;
		private final BlockPos.MutableBlockPos liquidPos = new BlockPos.MutableBlockPos();
		private boolean liquidQuad;
		private boolean building;
		private int edgeVertex = 4;
		private int usedSkyChroma = NO_PIN;
		private boolean bound;
		private boolean lit;
		private int originX;
		private int originY;
		private int originZ;

		boolean building() {
			return this.building;
		}

		void begin(SectionPos sectionPos, @Nullable RenderSectionRegion region) {
			this.building = true;
			this.pendingColors = null;
			this.pendingSkyColors = null;
			this.liquidLevel = null;
			this.liquidQuad = false;
			this.bound = false;
			this.lit = false;
			this.flatVerticesLeft = 0;
			this.flatSkyVerticesLeft = 0;
			this.latchedChroma = NO_PIN;
			this.latchedSkyChroma = NO_PIN;
			this.usedChroma = NO_PIN;
			this.usedSkyChroma = NO_PIN;
			this.smoothLighting = Minecraft.getInstance().options.ambientOcclusion().get();
			smoothLightingEnabled = this.smoothLighting;
			if (region == null) return;

			this.originX = sectionPos.minBlockX();
			this.originY = sectionPos.minBlockY();
			this.originZ = sectionPos.minBlockZ();
			this.bound = true;

			GlowtoneRegionFlood flood = this.flood;
			if (flood == null) {
				flood = this.flood = new GlowtoneRegionFlood();
			}

			flood.begin(region, sectionPos.x(), sectionPos.y(), sectionPos.z());
			this.lit = flood.isLit();
			if (this.smoothLighting) {
				if (this.lit) Arrays.fill(this.cornerCache, 0);
				if (this.flood.hasSkyTint()) Arrays.fill(this.skyCornerCache, 0);
			}
		}

		void end() {
			this.building = false;
			GlowtoneRegionFlood flood = this.flood;
			if (flood != null) {
				if (this.bound) {
					if (this.lit) this.pendingColors = flood.downsampleCentre();
					this.pendingSkyColors = flood.downsampleCentreSky();
				}
				flood.release();
			}
			this.bound = false;
			this.lit = false;
		}

		short @Nullable [] takePendingColors() {
			short[] colors = this.pendingColors;
			this.pendingColors = null;
			return colors;
		}

		short @Nullable [] takePendingSkyColors() {
			short[] colors = this.pendingSkyColors;
			this.pendingSkyColors = null;
			return colors;
		}

		public boolean smoothLighting() {
			return this.smoothLighting;
		}

		public GlowtoneEdgeNeighbours edgeNeighbours() {
			return this.edgeNeighbours;
		}

		public float @org.jspecify.annotations.Nullable [] modelFaces() {
			return this.modelFaces;
		}

		public void setModelFaces(float @org.jspecify.annotations.Nullable [] faces) {
			this.modelFaces = faces;
		}

		public GlowtoneQuadEdges pendingEdges() {
			return this.pendingEdges;
		}

		public void beginQuadEdges() {
			this.edgeVertex = 0;
			this.liquidQuad = false;
		}

		public void beginLiquid(BlockAndTintGetter level, BlockPos pos) {
			this.liquidLevel = level;
			this.liquidPos.set(pos);
		}

		public void endLiquid() {
			this.liquidLevel = null;
			this.liquidQuad = false;
		}

		public @Nullable BlockAndTintGetter liquidLevel() {
			return this.liquidLevel;
		}

		public BlockPos liquidPos() {
			return this.liquidPos;
		}

		public GlowtoneLiquidRims liquidRims() {
			return this.liquidRims;
		}

		public void beginLiquidQuadEdges() {
			this.edgeVertex = 4;
			this.liquidQuad = true;
		}

		public void endLiquidQuad() {
			this.liquidQuad = false;
		}

		public boolean liquidQuad() {
			return this.liquidQuad;
		}

		public int nextEdgeVertex() {
			return this.edgeVertex < 4 ? this.edgeVertex++ : -1;
		}

		public void rotateFlatPins() {
			this.usedChroma = this.latchedChroma;
			this.usedSkyChroma = this.latchedSkyChroma;

			if (this.flatVerticesLeft > 0) {
				this.latchedChroma = this.flatChroma;
				this.flatVerticesLeft--;
			} else {
				this.latchedChroma = NO_PIN;
			}

			if (this.flatSkyVerticesLeft > 0) {
				this.latchedSkyChroma = this.flatSkyChroma;
				this.flatSkyVerticesLeft--;
			} else {
				this.latchedSkyChroma = NO_PIN;
			}
		}

		public void beginFlatQuadLocal(float localX, float localY, float localZ) {
			this.beginFlatQuad(
					this.originX + Mth.floor(localX),
					this.originY + Mth.floor(localY),
					this.originZ + Mth.floor(localZ)
			);
		}

		void beginFlatQuad(int worldX, int worldY, int worldZ) {
			this.flatVerticesLeft = 0;
			this.flatSkyVerticesLeft = 0;

			GlowtoneRegionFlood flood = this.flood;
			if (!this.bound || flood == null) return;

			if (this.lit) {
				this.flatChroma = GlowtoneChromaBlend.toArgb(
						GlowtoneChromaBlend.add(GlowtoneChromaBlend.EMPTY, flood.cellLevelsAt(worldX, worldY, worldZ))
				);
				this.flatVerticesLeft = 4;
			}

			if (flood.hasSkyTint()) {
				this.flatSkyChroma = GlowtoneChromaBlend.skyTintArgb(flood.skyHueAt(worldX, worldY, worldZ));
				this.flatSkyVerticesLeft = 4;
			}
		}

		public int sampleSky(float x, float y, float z) {
			if (this.usedSkyChroma != NO_PIN) {
				return this.usedSkyChroma;
			}
			if (!this.smoothLighting) return NEUTRAL_SKY_ARGB;

			GlowtoneRegionFlood flood = this.flood;
			if (!this.bound || flood == null || !flood.hasSkyTint()) return NEUTRAL_SKY_ARGB;

			int localX = Math.round(x);
			int localY = Math.round(y);
			int localZ = Math.round(z);
			int slot = cacheSlot(localX, localY, localZ);

			if (slot < 0) {
				return this.blendSkyCorner(
						this.originX + localX, this.originY + localY, this.originZ + localZ
				);
			}

			int cached = this.skyCornerCache[slot];
			if (cached != 0) return cached;

			int argb = this.blendSkyCorner(
					this.originX + localX, this.originY + localY, this.originZ + localZ
			);
			this.skyCornerCache[slot] = argb;
			return argb;
		}

		private int blendSkyCorner(int cornerX, int cornerY, int cornerZ) {
			GlowtoneRegionFlood flood = this.flood;
			if (flood == null) return NEUTRAL_SKY_ARGB;

			int red = 0;
			int green = 0;
			int blue = 0;
			int count = 0;
			int strongest = 0xFFFFFF;
			int strongestDeparture = 0;

			for (int dx = -1; dx <= 0; dx++) {
				for (int dy = -1; dy <= 0; dy++) {
					for (int dz = -1; dz <= 0; dz++) {
						int x = cornerX + dx;
						int y = cornerY + dy;
						int z = cornerZ + dz;
						if (isOpaque(flood, x, y, z)) continue;

						int hue = flood.skyHueAt(x, y, z);
						red += (hue >> 16) & 0xFF;
						green += (hue >> 8) & 0xFF;
						blue += hue & 0xFF;
						count++;

						int departure = (255 - ((hue >> 16) & 0xFF))
								+ (255 - ((hue >> 8) & 0xFF))
								+ (255 - (hue & 0xFF));
						if (departure > strongestDeparture) {
							strongestDeparture = departure;
							strongest = hue;
						}
					}
				}
			}

			if (count == 0) return NEUTRAL_SKY_ARGB;
			if (!this.smoothLighting) return GlowtoneChromaBlend.skyTintArgb(strongest);
			return GlowtoneChromaBlend.skyTintArgb(((red / count) << 16) | ((green / count) << 8) | (blue / count));
		}

		public int sample(float x, float y, float z) {
			if (!this.smoothLighting && this.usedChroma == NO_PIN) return NEUTRAL_ARGB;

			if (this.usedChroma != NO_PIN) {
				return this.usedChroma;
			}

			if (!this.bound || !this.lit) {
				return NEUTRAL_ARGB;
			}

			int localX = Math.round(x);
			int localY = Math.round(y);
			int localZ = Math.round(z);

			int slot = cacheSlot(localX, localY, localZ);
			if (slot < 0) {
				return this.blendCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);
			}

			int cached = this.cornerCache[slot];
			if (cached != 0) {
				return cached;
			}

			int argb = this.blendCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);
			this.cornerCache[slot] = argb;
			return argb;
		}

		private int blendCorner(int cornerX, int cornerY, int cornerZ) {
			GlowtoneRegionFlood flood = this.flood;
			if (flood == null) return NEUTRAL_ARGB;

			long accumulator = GlowtoneChromaBlend.EMPTY;
			int brightest = 0;

			for (int dx = -1; dx <= 0; dx++) {
				for (int dy = -1; dy <= 0; dy++) {
					for (int dz = -1; dz <= 0; dz++) {
						int levels = flood.levelsAt(cornerX + dx, cornerY + dy, cornerZ + dz);
						if (levels == 0) continue;

						if (tintsLight(flood, cornerX + dx, cornerY + dy, cornerZ + dz)) continue;

						if (this.smoothLighting) {
							accumulator = GlowtoneChromaBlend.add(accumulator, levels);
						} else if ((levels & 0xF) > (brightest & 0xF)) {
							brightest = levels;
						}
					}
				}
			}

			if (!this.smoothLighting) {
				accumulator = GlowtoneChromaBlend.add(GlowtoneChromaBlend.EMPTY, brightest);
			}
			return GlowtoneChromaBlend.toArgb(accumulator);
		}

		private static boolean isOpaque(GlowtoneRegionFlood flood, int worldX, int worldY, int worldZ) {
			return flood.stateAt(worldX, worldY, worldZ).getLightDampening() >= OPAQUE_DAMPENING;
		}

		private static boolean tintsLight(GlowtoneRegionFlood flood, int worldX, int worldY, int worldZ) {
			var state = flood.stateAt(worldX, worldY, worldZ);
			return GlowtoneTransmittance.filterFor(state) != GlowtoneTransmittance.FULLY_TRANSMISSIVE;
		}

		private static int cacheSlot(int localX, int localY, int localZ) {
			int ix = localX - CORNER_MIN;
			int iy = localY - CORNER_MIN;
			int iz = localZ - CORNER_MIN;
			if ((ix | iy | iz) < 0 || ix >= CORNER_SPAN || iy >= CORNER_SPAN || iz >= CORNER_SPAN) {
				return -1;
			}
			return (iz * CORNER_SPAN + iy) * CORNER_SPAN + ix;
		}
	}
}
