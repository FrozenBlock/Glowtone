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

package net.frozenblock.glowtone.light.color.render;

import java.util.Arrays;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.light.GlowtoneRegionFlood;
import net.frozenblock.glowtone.light.color.FilterColorHelper;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.light.edge.FluidEdges;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.GlowtoneSectionColorStore;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@ClientOnly
public final class ChromaBaker {
	public static final int NEUTRAL_ARGB = ChromaBlender.NEUTRAL_TERRAIN_ARGB;
	public static final int NEUTRAL_SKY_ARGB = 0xFFFFFFFF;
	private static final int NO_PIN = 0;
	private static final int OPAQUE_DAMPENING = LightEngine.MAX_LEVEL;

	private static final ThreadLocal<SectionState> STATE = ThreadLocal.withInitial(SectionState::new);
	private static volatile boolean smoothLightingEnabled = true;
	private static volatile boolean vanillaOcclusion;
	private static volatile float occlusionScale = 1F;

	public static void beginSection(SectionPos sectionPos, @Nullable RenderSectionRegion region) {
		final SectionState state = STATE.get();
		if (!GlowtoneConfig.coloredLighting().enabled()) {
			state.begin(sectionPos, null);
			return;
		}
		state.begin(sectionPos, region);
	}

	public static void beginSodiumSection(
		SectionPos origin, PalettedContainerRO<BlockState>[] grid
	) {
		final SectionState state = STATE.get();
		if (!GlowtoneConfig.coloredLighting().enabled()) {
			state.begin(origin, null);
			GlowtoneSectionColorStore.publish(origin.asLong(), null, null);
			return;
		}
		state.beginSodium(origin, grid);
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

	public static float occlusionShade(float brightness) {
		if (!vanillaOcclusion) return 1F;

		final float scale = occlusionScale;
		return scale == 1F ? brightness : 1F - (1F - brightness) * scale;
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
		private final QuadEdges pendingEdges = new QuadEdges();
		private final EdgeNeighbours edgeNeighbours = new EdgeNeighbours();
		private float @Nullable [] modelFaces;
		private final FluidEdges fluidEdges = new FluidEdges();
		private @Nullable BlockAndTintGetter fluidLevel;
		private final BlockPos.MutableBlockPos fluidPos = new BlockPos.MutableBlockPos();
		private boolean fluidQuad;
		private boolean building;
		private int edgeVertex = 4;
		private int usedSkyChroma = NO_PIN;
		private boolean bound;
		private boolean lit;
		private boolean highlightEnabled;
		private boolean contactShading;
		private boolean emissiveQuad;
		private long scratch;
		private final float[] quadPositions = new float[12];
		private int originX;
		private int originY;
		private int originZ;

		boolean building() {
			return this.building;
		}

		private void reset() {
			this.building = true;
			this.pendingColors = null;
			this.pendingSkyColors = null;
			this.fluidLevel = null;
			this.fluidQuad = false;
			this.bound = false;
			this.lit = false;
			this.flatVerticesLeft = 0;
			this.flatSkyVerticesLeft = 0;
			this.latchedChroma = NO_PIN;
			this.latchedSkyChroma = NO_PIN;
			this.usedChroma = NO_PIN;
			this.usedSkyChroma = NO_PIN;
			this.smoothLighting = Minecraft.getInstance().options.ambientOcclusion().get();
			if (smoothLightingEnabled != this.smoothLighting) smoothLightingEnabled = this.smoothLighting;

			this.highlightEnabled = EdgeHighlightOption.enabled();
			this.contactShading =
				(AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING)
					|| GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.AMBIENT_OCCLUSION);
			this.emissiveQuad = false;

			final boolean vanilla = AmbientOcclusionOption.vanillaActive();
			final float scale = OcclusionStrengthOption.scale();
			if (vanillaOcclusion != vanilla) vanillaOcclusion = vanilla;
			if (occlusionScale != scale) occlusionScale = scale;
		}

		void begin(SectionPos sectionPos, @Nullable RenderSectionRegion region) {
			this.reset();
			if (region == null) return;

			final GlowtoneRegionFlood flood = this.bind(sectionPos);
			flood.begin(region, sectionPos.x(), sectionPos.y(), sectionPos.z());
			this.latch(flood);
		}

		void beginSodium(SectionPos sectionPos, PalettedContainerRO<BlockState>[] grid) {
			this.reset();

			final GlowtoneRegionFlood flood = this.bind(sectionPos);
			flood.begin(grid, sectionPos.x() - 1, sectionPos.y() - 1, sectionPos.z() - 1);
			this.latch(flood);

			GlowtoneSectionColorStore.publish(
				sectionPos.asLong(),
				this.lit ? flood.downsampleCentre() : null,
				flood.downsampleCentreSky()
			);
		}

		private GlowtoneRegionFlood bind(SectionPos sectionPos) {
			this.originX = sectionPos.minBlockX();
			this.originY = sectionPos.minBlockY();
			this.originZ = sectionPos.minBlockZ();
			this.bound = true;

			GlowtoneRegionFlood flood = this.flood;
			if (flood == null) flood = this.flood = new GlowtoneRegionFlood();
			return flood;
		}

		private void latch(GlowtoneRegionFlood flood) {
			this.lit = flood.isLit();
			if (this.smoothLighting) {
				if (this.lit) Arrays.fill(this.cornerCache, 0);
				if (flood.hasSkyTint()) Arrays.fill(this.skyCornerCache, 0);
			}
		}

		void end() {
			this.building = false;
			final GlowtoneRegionFlood flood = this.flood;
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
			final short[] colors = this.pendingColors;
			this.pendingColors = null;
			return colors;
		}

		short @Nullable [] takePendingSkyColors() {
			final short[] colors = this.pendingSkyColors;
			this.pendingSkyColors = null;
			return colors;
		}

		public boolean highlightEnabled() {
			return this.highlightEnabled;
		}

		public boolean contactShading() {
			return this.contactShading;
		}

		public boolean emissiveQuad() {
			return this.emissiveQuad;
		}

		public void setEmissiveQuad(boolean emissive) {
			this.emissiveQuad = emissive;
		}

		public float[] quadPositions() {
			return this.quadPositions;
		}

		public long scratch(int bytes) {
			if (this.scratch == 0L) this.scratch = MemoryUtil.nmemAlloc(bytes);
			return this.scratch;
		}

		public boolean smoothLighting() {
			return this.smoothLighting;
		}

		public EdgeNeighbours edgeNeighbours() {
			return this.edgeNeighbours;
		}

		public float @Nullable [] modelFaces() {
			return this.modelFaces;
		}

		public void setModelFaces(float @Nullable [] faces) {
			this.modelFaces = faces;
		}

		public QuadEdges pendingEdges() {
			return this.pendingEdges;
		}

		public void beginQuadEdges() {
			this.edgeVertex = 0;
			this.fluidQuad = false;
		}

		public void beginFluid(BlockAndTintGetter level, BlockPos pos) {
			this.fluidLevel = level;
			this.fluidPos.set(pos);
		}

		public void endFluid() {
			this.fluidLevel = null;
			this.fluidQuad = false;
		}

		@Nullable
		public BlockAndTintGetter fluidLevel() {
			return this.fluidLevel;
		}

		public BlockPos fluidPos() {
			return this.fluidPos;
		}

		public FluidEdges fluidEdges() {
			return this.fluidEdges;
		}

		public void beginFluidQuadEdges() {
			this.edgeVertex = 4;
			this.fluidQuad = true;
		}

		public void endFluidQuad() {
			this.fluidQuad = false;
		}

		public boolean fluidQuad() {
			return this.fluidQuad;
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
				final int levels = flood.cellLevelsAt(worldX, worldY, worldZ);

				if (levels != 0) {
					this.flatChroma = ChromaBlender.toArgb(ChromaBlender.add(ChromaBlender.EMPTY, levels));
					this.flatVerticesLeft = 4;
				}
			}

			if (flood.hasSkyTint()) {
				this.flatSkyChroma = ChromaBlender.skyTintArgb(flood.skyHueAt(worldX, worldY, worldZ));
				this.flatSkyVerticesLeft = 4;
			}
		}

		public int sampleSky(float x, float y, float z) {
			if (this.usedSkyChroma != NO_PIN) return this.usedSkyChroma;
			if (!this.smoothLighting) return NEUTRAL_SKY_ARGB;

			final GlowtoneRegionFlood flood = this.flood;
			if (!this.bound || flood == null || !flood.hasSkyTint()) return NEUTRAL_SKY_ARGB;

			final int localX = Math.round(x);
			final int localY = Math.round(y);
			final int localZ = Math.round(z);
			final int slot = cacheSlot(localX, localY, localZ);

			if (slot < 0) return this.blendSkyCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);

			final int cached = this.skyCornerCache[slot];
			if (cached != 0) return cached;

			final int argb = this.blendSkyCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);
			this.skyCornerCache[slot] = argb;
			return argb;
		}

		private int blendSkyCorner(int cornerX, int cornerY, int cornerZ) {
			final GlowtoneRegionFlood flood = this.flood;
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
						final int x = cornerX + dx;
						final int y = cornerY + dy;
						final int z = cornerZ + dz;
						if (isOpaque(flood, x, y, z)) continue;

						final int hue = flood.skyHueAt(x, y, z);
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
			if (!this.smoothLighting) return ChromaBlender.skyTintArgb(strongest);
			return ChromaBlender.skyTintArgb(((red / count) << 16) | ((green / count) << 8) | (blue / count));
		}

		public int sample(float x, float y, float z) {
			if (!this.smoothLighting && this.usedChroma == NO_PIN) return NEUTRAL_ARGB;
			if (this.usedChroma != NO_PIN) return this.usedChroma;
			if (!this.bound || !this.lit) return NEUTRAL_ARGB;

			final int localX = Math.round(x);
			final int localY = Math.round(y);
			final int localZ = Math.round(z);

			final int slot = cacheSlot(localX, localY, localZ);
			if (slot < 0) return this.blendCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);

			final int cached = this.cornerCache[slot];
			if (cached != 0) return cached;

			final int argb = this.blendCorner(this.originX + localX, this.originY + localY, this.originZ + localZ);
			this.cornerCache[slot] = argb;
			return argb;
		}

		private int blendCorner(int cornerX, int cornerY, int cornerZ) {
			final GlowtoneRegionFlood flood = this.flood;
			if (flood == null) return NEUTRAL_ARGB;

			long accumulator = ChromaBlender.EMPTY;
			int brightest = 0;

			for (int dx = -1; dx <= 0; dx++) {
				for (int dy = -1; dy <= 0; dy++) {
					for (int dz = -1; dz <= 0; dz++) {
						final int levels = flood.levelsAt(cornerX + dx, cornerY + dy, cornerZ + dz);
						if (levels == 0) continue;

						if (discards(flood, cornerX + dx, cornerY + dy, cornerZ + dz)) continue;

						if (this.smoothLighting) {
							accumulator = ChromaBlender.add(accumulator, levels);
						} else if ((levels & 0xF) > (brightest & 0xF)) {
							brightest = levels;
						}
					}
				}
			}

			if (!this.smoothLighting) accumulator = ChromaBlender.add(ChromaBlender.EMPTY, brightest);
			return ChromaBlender.toArgb(accumulator);
		}

		private static boolean isOpaque(GlowtoneRegionFlood flood, int worldX, int worldY, int worldZ) {
			return flood.stateAt(worldX, worldY, worldZ).getLightDampening() >= OPAQUE_DAMPENING;
		}

		private static boolean discards(GlowtoneRegionFlood flood, int worldX, int worldY, int worldZ) {
			final BlockState state = flood.stateAt(worldX, worldY, worldZ);
			if (state.getLightEmission() > 0) return false;
			return FilterColorHelper.filterFor(state) != FilterColorHelper.FULLY_TRANSMISSIVE;
		}

		private static int cacheSlot(int localX, int localY, int localZ) {
			final int ix = localX - CORNER_MIN;
			final int iy = localY - CORNER_MIN;
			final int iz = localZ - CORNER_MIN;
			if ((ix | iy | iz) < 0 || ix >= CORNER_SPAN || iy >= CORNER_SPAN || iz >= CORNER_SPAN) return -1;
			return (iz * CORNER_SPAN + iy) * CORNER_SPAN + ix;
		}
	}

	private ChromaBaker() {}
}
