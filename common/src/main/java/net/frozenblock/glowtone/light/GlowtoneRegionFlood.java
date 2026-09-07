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

package net.frozenblock.glowtone.light;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.Arrays;
import java.util.function.Predicate;
import net.frozenblock.glowtone.light.color.EmitterColorHelper;
import net.frozenblock.glowtone.light.color.FilterColorHelper;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneRegionFlood {
	public static final int SPAN = 48;
	public static final int WHITE_RGB = 0xFFFFFF;
	private static final Predicate<BlockState> TINTS_DAYLIGHT = state ->
		FilterColorHelper.filterFor(state) != FilterColorHelper.FULLY_TRANSMISSIVE && state.getFluidState().isEmpty();

	public static final int ENTITY_SPAN = 8;
	public static final int ENTITY_CELL_BLOCKS = 2;
	public static final int ENTITY_CELLS = ENTITY_SPAN * ENTITY_SPAN * ENTITY_SPAN;

	public static final int WINDOW_SPAN = 20;
	private static final int DOWN_SPAN = WINDOW_SPAN / ENTITY_CELL_BLOCKS;
	private static final int DOWN_CELLS = DOWN_SPAN * DOWN_SPAN * DOWN_SPAN;
	public static final int WINDOW_CELLS = WINDOW_SPAN * WINDOW_SPAN * WINDOW_SPAN;

	private static final int WINDOW_MIN = 14;
	private static final int WINDOW_MAX = 33;

	private static final int MAX_REACH = GlowtoneChannels.MAX_LEVEL - 1;

	private static final int SPAN_SQ = SPAN * SPAN;
	private static final int CELLS = SPAN * SPAN * SPAN;

	private static final int MAX_EXPANSIONS = CELLS;
	private static final int SKY_BUCKETS = (GlowtoneChannels.MAX_LEVEL + 1) * SPAN;
	private static volatile boolean skyTintDimension = true;
	private static final byte SKY_UNKNOWN = -1;
	private static final int SECTION_GRID = RenderSectionRegion.SIZE;

	private static final Direction[] DIRECTIONS = Direction.values();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final Predicate<BlockState> EMITS_LIGHT = state -> state.getLightEmission() > 0;

	private static final int ENTRY_Y_SHIFT = 6;
	private static final int ENTRY_Z_SHIFT = 12;
	private static final int ENTRY_EMPTY_SHAPE = 1 << 18;
	private static final int ENTRY_SKIP_SHIFT = 19;
	private static final int ENTRY_COORD_MASK = 0x3F;

	static {
		if (RenderSectionRegion.RADIUS != 1 || RenderSectionRegion.SIZE != 3) {
			throw new AssertionError("Glowtone assumes a 3x3x3 RenderSectionRegion, found SIZE=" + RenderSectionRegion.SIZE);
		}
		if (WINDOW_MIN - MAX_REACH != 0 || WINDOW_MAX + MAX_REACH != SPAN - 1) {
			throw new AssertionError(
				"Colored-lightColor reach does not match the region: window [%s, %s], expanded by %s. must be [0, %s]"
					.formatted(WINDOW_MIN, WINDOW_MAX, MAX_REACH, SPAN - 1)
			);
		}
		if (SPAN != RenderSectionRegion.SIZE * 16 || SPAN > ENTRY_COORD_MASK + 1) {
			throw new AssertionError("Region span " + SPAN + " does not fit the flood's cell encoding.");
		}
	}

	private short[] levels;
	private int[] stateStamp;
	private short[] downsampled;
	private int[] downsampledStamp;
	private int stateGeneration;
	private boolean levelsBeyondWindow;
	private boolean dynamicSeeded;
	private short[] skyHues;
	private byte[] skyLevels;
	private short[] skyOut;
	private byte[] skyMarked;
	private int[] skyOrder;
	private int[] skyOrdered;
	private final int[] skyBuckets = new int[SKY_BUCKETS];
	private final IntArrayFIFOQueue skyQueue = new IntArrayFIFOQueue();
	private @Nullable BlockState[] states;

	private final int[][] buckets = new int[GlowtoneChannels.MAX_LEVEL + 1][];
	private final int[] bucketSizes = new int[GlowtoneChannels.MAX_LEVEL + 1];

	private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

	private @Nullable RenderSectionRegion region;
	private DataLayer @Nullable [] skyLightLayers;
	private SectionCopy @Nullable [] sections;
	private final PalettedContainerRO<BlockState>[] containers = newContainerGrid();
	private boolean containersBound;
	private boolean skyTinted;
	private boolean debugRegion;
	private int minBlockX;
	private int minBlockY;
	private int minBlockZ;
	private boolean lit;

	public GlowtoneRegionFlood() {}

	public static int entityCellIndex(int localX, int localY, int localZ) {
		return (((localY & 15) >> 1) * ENTITY_SPAN + ((localZ & 15) >> 1)) * ENTITY_SPAN + ((localX & 15) >> 1);
	}

	@SuppressWarnings("unchecked")
	private static PalettedContainerRO<BlockState>[] newContainerGrid() {
		return new PalettedContainerRO[SECTION_GRID * SECTION_GRID * SECTION_GRID];
	}

	public boolean begin(
		PalettedContainerRO<BlockState>[] grid, DataLayer @Nullable [] skyGrid,
		int minSectionX, int minSectionY, int minSectionZ,
		short @Nullable [] cachedWindow, short @Nullable [] cachedSky
	) {
		this.release();

		if (grid.length != SECTION_GRID * SECTION_GRID * SECTION_GRID) return false;

		System.arraycopy(grid, 0, this.containers, 0, grid.length);

		final int emitterMask = emitterMask(this.containers);
		final int tintMask = skyTintActive() && skyGrid != null ? tintMask(this.containers) : 0;
		final boolean dynamic = GlowtoneDynamicLights.get().anyWithin(minSectionX << 4, minSectionY << 4, minSectionZ << 4, SPAN);
		if (emitterMask == 0 && tintMask == 0 && !dynamic) return false;

		if (this.levels == null) {
			this.levels = new short[CELLS];
			this.states = new BlockState[CELLS];
			this.stateStamp = new int[CELLS];
			this.downsampled = new short[DOWN_CELLS];
			this.downsampledStamp = new int[DOWN_CELLS];
		}

		this.region = null;
		this.skyLightLayers = skyGrid;
		this.sections = null;
		this.debugRegion = false;
		this.containersBound = true;
		this.minBlockX = minSectionX << 4;
		this.minBlockY = minSectionY << 4;
		this.minBlockZ = minSectionZ << 4;
		this.lit = false;
		this.skyTinted = tintMask != 0;

		this.nextStateGeneration();
		if (this.skyTinted) {
			if (cachedSky != null) {
				this.restoreSkyWindow(cachedSky);
			} else {
				this.floodSkyTint(this.containers, tintMask);
			}
		}

		this.dynamicSeeded = dynamic;
		if (cachedWindow != null && !dynamic) {
			this.restoreWindow(cachedWindow);
			return this.lit || this.skyTinted;
		}

		if (emitterMask != 0 || dynamic) {
			java.util.Arrays.fill(this.levels, (short) 0);
			java.util.Arrays.fill(this.bucketSizes, 0);
			this.levelsBeyondWindow = true;

			this.seed(this.containers, emitterMask);
			this.propagate();
			this.seedDynamicLights();
		}

		return this.lit || this.skyTinted;
	}

	public boolean begin(
		RenderSectionRegion region, int centreSectionX, int centreSectionY, int centreSectionZ,
		short @Nullable [] cachedWindow, short @Nullable [] cachedSky
	) {
		this.release();

		final SectionCopy[] sections = region.sections;
		if (sections == null || sections.length != SECTION_GRID * SECTION_GRID * SECTION_GRID) {
			return false;
		}

		final int minSectionX = region.minSectionX;
		final int minSectionY = region.minSectionY;
		final int minSectionZ = region.minSectionZ;
		if (minSectionX + RenderSectionRegion.RADIUS != centreSectionX
			|| minSectionY + RenderSectionRegion.RADIUS != centreSectionY
			|| minSectionZ + RenderSectionRegion.RADIUS != centreSectionZ
		) {
			return false;
		}

		for (int slot = 0; slot < sections.length; slot++) {
			this.containers[slot] = sections[slot].debug ? null : sections[slot].section;
		}

		final int emitterMask = emitterMask(this.containers);
		final int tintMask = skyTintActive() ? tintMask(this.containers) : 0;
		if (emitterMask == 0
			&& tintMask == 0
			&& !GlowtoneDynamicLights.get().anyWithin(minSectionX << 4, minSectionY << 4, minSectionZ << 4, SPAN)
		) {
			return false;
		}

		if (this.levels == null) {
			this.levels = new short[CELLS];
			this.states = new BlockState[CELLS];
			this.stateStamp = new int[CELLS];
			this.downsampled = new short[DOWN_CELLS];
			this.downsampledStamp = new int[DOWN_CELLS];
		}

		this.region = region;
		this.sections = sections;
		this.containersBound = true;
		this.debugRegion = sections[index27(1, 1, 1)].debug;
		this.minBlockX = minSectionX << 4;
		this.minBlockY = minSectionY << 4;
		this.minBlockZ = minSectionZ << 4;
		this.lit = false;
		this.skyTinted = tintMask != 0;

		this.nextStateGeneration();
		if (this.skyTinted) {
			if (cachedSky != null) {
				this.restoreSkyWindow(cachedSky);
			} else {
				this.floodSkyTint(this.containers, tintMask);
			}
		}

		final boolean anyDynamic = GlowtoneDynamicLights.get().anyWithin(this.minBlockX, this.minBlockY, this.minBlockZ, SPAN);
		this.dynamicSeeded = anyDynamic;
		if (cachedWindow != null && !anyDynamic) {
			this.restoreWindow(cachedWindow);
			return this.lit || this.skyTinted;
		}

		if (emitterMask != 0 || anyDynamic) {
			Arrays.fill(this.levels, (short) 0);
			Arrays.fill(this.bucketSizes, 0);
			this.levelsBeyondWindow = true;

			this.seed(this.containers, emitterMask);
			this.propagate();
			if (anyDynamic) this.seedDynamicLights();
		}
		return this.lit || this.skyTinted;
	}

	public short @Nullable [] extractSkyWindow() {
		if (!this.skyTinted || this.skyHues == null) return null;

		final short[] window = new short[WINDOW_CELLS];
		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				final int row = cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z);
				for (int x = 0; x < WINDOW_SPAN; x++) {
					window[at + x] = (short) (this.skyHues[row + x] & GlowtoneChannels.WHITE_HUE);
				}
				at += WINDOW_SPAN;
			}
		}
		return window;
	}

	private void restoreSkyWindow(short[] window) {
		this.allocateSky();
		Arrays.fill(this.skyHues, (short) 0);

		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				final int row = cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z);
				for (int x = 0; x < WINDOW_SPAN; x++) {
					this.skyHues[row + x] = (short) (window[at + x] & GlowtoneChannels.WHITE_HUE);
				}
				at += WINDOW_SPAN;
			}
		}
	}

	public short @Nullable [] extractWindow() {
		if (!this.lit || this.levels == null || this.dynamicSeeded) return null;

		final short[] window = new short[WINDOW_CELLS];
		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				System.arraycopy(this.levels, cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z), window, at, WINDOW_SPAN);
				at += WINDOW_SPAN;
			}
		}
		return window;
	}

	private void nextStateGeneration() {
		if (this.stateGeneration == Integer.MAX_VALUE) {
			Arrays.fill(this.stateStamp, 0);
			this.stateGeneration = 0;
		}
		this.stateGeneration++;
	}

	private void restoreWindow(short[] window) {
		if (this.levelsBeyondWindow) {
			Arrays.fill(this.levels, (short) 0);
			this.levelsBeyondWindow = false;
		}
		this.nextStateGeneration();
		this.dynamicSeeded = false;

		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				System.arraycopy(window, at, this.levels, cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z), WINDOW_SPAN);
				at += WINDOW_SPAN;
			}
		}
		this.lit = true;
	}

	public boolean isLit() {
		return this.lit;
	}

	public static void setSkyTintDimension(boolean value) {
		skyTintDimension = value;
	}

	private static boolean skyTintActive() {
		return skyTintDimension;
	}

	public boolean hasSkyTint() {
		return this.skyTinted;
	}

	public int skyHueAt(int worldX, int worldY, int worldZ) {
		if (!this.skyTinted || !this.containersBound) return WHITE_RGB;

		final int rx = worldX - this.minBlockX;
		final int ry = worldY - this.minBlockY;
		final int rz = worldZ - this.minBlockZ;
		if (isOutside(rx, ry, rz)) return WHITE_RGB;

		final int hue = this.incidentHue(cellIndex(rx, ry, rz));
		if (hue == 0 || hue == GlowtoneChannels.WHITE_HUE) return WHITE_RGB;

		return GlowtoneChannels.toNormalisedRgb(GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, hue));
	}

	private void allocateSky() {
		if (this.skyHues != null) return;
		this.skyHues = new short[CELLS];
		this.skyOut = new short[CELLS];
		this.skyLevels = new byte[CELLS];
		this.skyMarked = new byte[CELLS];
		this.skyOrder = new int[CELLS];
		this.skyOrdered = new int[CELLS];
	}

	private void floodSkyTint(PalettedContainerRO<BlockState>[] sections, int tintMask) {
		this.allocateSky();
		Arrays.fill(this.skyHues, (short) 0);
		Arrays.fill(this.skyLevels, SKY_UNKNOWN);
		Arrays.fill(this.skyMarked, (byte) 0);
		this.skyQueue.clear();
		int marked = 0;

		for (int index = 0; index < sections.length; index++) {
			if ((tintMask & (1 << index)) == 0) continue;

			final int baseX = (index % SECTION_GRID) << 4;
			final int baseY = ((index / SECTION_GRID) % SECTION_GRID) << 4;
			final int baseZ = (index / (SECTION_GRID * SECTION_GRID)) << 4;

			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						final int rx = baseX + x;
						final int ry = baseY + y;
						final int rz = baseZ + z;
						if (!withinReach(rx, rz, GlowtoneChannels.MAX_LEVEL)) continue;
						if (!TINTS_DAYLIGHT.test(this.stateAt(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz))) continue;

						final int level = this.skyLevelAt(rx, ry, rz);
						if (level <= 0 || !withinReach(rx, rz, level)) continue;

						final int cell = cellIndex(rx, ry, rz);
						if (this.skyMarked[cell] != 0) continue;
						this.skyMarked[cell] = 1;
						this.skyOrder[marked++] = cell;
						this.skyQueue.enqueue(cell);
					}
				}
			}
		}

		while (!this.skyQueue.isEmpty()) {
			final int cell = this.skyQueue.dequeueInt();
			final int rx = cell % SPAN;
			final int rz = (cell / SPAN) % SPAN;
			final int ry = cell / SPAN_SQ;
			final int level = this.skyLevels[cell];

			for (Direction direction : DIRECTIONS) {
				final int nx = rx + direction.getStepX();
				final int ny = ry + direction.getStepY();
				final int nz = rz + direction.getStepZ();
				if (isOutside(nx, ny, nz)) continue;

				final int next = cellIndex(nx, ny, nz);
				if (this.skyMarked[next] != 0) continue;

				final int dampening = this.stateAt(this.minBlockX + nx, this.minBlockY + ny, this.minBlockZ + nz).getLightDampening();
				if (dampening >= GlowtoneChannels.MAX_LEVEL) continue;

				final int neighbour = this.skyLevelAt(nx, ny, nz);
				if (neighbour <= 0 || level - feedCost(direction == Direction.DOWN, level, dampening) != neighbour) continue;
				if (!withinReach(nx, nz, neighbour)) continue;

				this.skyMarked[next] = 1;
				this.skyOrder[marked++] = next;
				this.skyQueue.enqueue(next);
			}
		}

		final int core = marked;
		for (int i = 0; i < core; i++) {
			final int cell = this.skyOrder[i];
			final int rx = cell % SPAN;
			final int rz = (cell / SPAN) % SPAN;
			final int ry = cell / SPAN_SQ;

			for (Direction direction : DIRECTIONS) {
				final int nx = rx + direction.getStepX();
				final int ny = ry + direction.getStepY();
				final int nz = rz + direction.getStepZ();
				if (isOutside(nx, ny, nz)) continue;

				final int next = cellIndex(nx, ny, nz);
				if (this.skyMarked[next] != 0) continue;
				if (this.stateAt(this.minBlockX + nx, this.minBlockY + ny, this.minBlockZ + nz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;

				final int neighbour = this.skyLevelAt(nx, ny, nz);
				if (neighbour <= 0 || !withinReach(nx, nz, neighbour)) continue;

				this.skyMarked[next] = 1;
				this.skyOrder[marked++] = next;
			}
		}

		final int[] buckets = this.skyBuckets;
		Arrays.fill(buckets, 0);
		for (int i = 0; i < marked; i++) buckets[this.skyBucket(this.skyOrder[i])]++;
		for (int i = 1; i < buckets.length; i++) buckets[i] += buckets[i - 1];
		for (int i = marked - 1; i >= 0; i--) {
			final int cell = this.skyOrder[i];
			this.skyOrdered[--buckets[this.skyBucket(cell)]] = cell;
		}

		for (int i = 0; i < marked; i++) this.resolveSkyCell(this.skyOrdered[i]);
		for (int i = 0; i < marked; i++) this.blendSkyCell(this.skyOrdered[i]);
	}

	private static final int[] SKY_PATH_WEIGHT = {100, 80, 64, 51, 41, 33};

	private void blendSkyCell(int cell) {
		final int rx = cell % SPAN;
		final int rz = (cell / SPAN) % SPAN;
		final int ry = cell / SPAN_SQ;
		final int level = this.skyLevels[cell];
		final int dampening = this.stateAt(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz).getLightDampening();

		int red = 0;
		int green = 0;
		int blue = 0;
		int total = 0;
		for (Direction direction : DIRECTIONS) {
			final int nx = rx + direction.getStepX();
			final int ny = ry + direction.getStepY();
			final int nz = rz + direction.getStepZ();
			if (isOutside(nx, ny, nz)) continue;

			final int neighbour = this.skyLevelAt(nx, ny, nz);
			if (neighbour <= 0) continue;
			if (this.stateAt(this.minBlockX + nx, this.minBlockY + ny, this.minBlockZ + nz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;

			final int deficit = Math.max(0, level - (neighbour - feedCost(direction == Direction.UP, neighbour, dampening)));
			if (deficit >= SKY_PATH_WEIGHT.length) continue;

			final int from = cellIndex(nx, ny, nz);
			if (deficit > 0 && this.skyMarked[from] != 0 && this.skyOut[from] != this.skyHues[from]) continue;
			int hue = this.skyMarked[from] != 0 ? this.skyOut[from] & GlowtoneChannels.WHITE_HUE : GlowtoneChannels.WHITE_HUE;
			if (hue == 0) hue = GlowtoneChannels.WHITE_HUE;

			final int weight = SKY_PATH_WEIGHT[deficit];
			red += ((hue >> 8) & 0xF) * 0x11 * weight;
			green += ((hue >> 4) & 0xF) * 0x11 * weight;
			blue += (hue & 0xF) * 0x11 * weight;
			total += weight;
		}

		if (total == 0) return;
		this.skyHues[cell] = (short) GlowtoneChannels.normaliseHue(
			(red / total) >> 4, (green / total) >> 4, (blue / total) >> 4
		);
	}

	private int skyBucket(int cell) {
		return (GlowtoneChannels.MAX_LEVEL - this.skyLevels[cell]) * SPAN + (SPAN - 1 - cell / SPAN_SQ);
	}

	private void resolveSkyCell(int cell) {
		final int rx = cell % SPAN;
		final int rz = (cell / SPAN) % SPAN;
		final int ry = cell / SPAN_SQ;
		final int level = this.skyLevels[cell];
		final BlockState state = this.stateAt(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz);
		final int dampening = state.getLightDampening();

		int red = 0;
		int green = 0;
		int blue = 0;
		int feeders = 0;
		for (Direction direction : DIRECTIONS) {
			final int fx = rx + direction.getStepX();
			final int fy = ry + direction.getStepY();
			final int fz = rz + direction.getStepZ();
			if (isOutside(fx, fy, fz)) continue;

			final int feeder = this.skyLevelAt(fx, fy, fz);
			if (feeder <= 0 || feeder - feedCost(direction == Direction.UP, feeder, dampening) != level) continue;
			if (this.stateAt(this.minBlockX + fx, this.minBlockY + fy, this.minBlockZ + fz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;

			final int from = cellIndex(fx, fy, fz);
			int hue = this.skyMarked[from] != 0 ? this.skyOut[from] & GlowtoneChannels.WHITE_HUE : GlowtoneChannels.WHITE_HUE;
			if (hue == 0) hue = GlowtoneChannels.WHITE_HUE;
			red += (hue >> 8) & 0xF;
			green += (hue >> 4) & 0xF;
			blue += hue & 0xF;
			feeders++;
		}

		final int incident = feeders == 0
			? GlowtoneChannels.WHITE_HUE
			: GlowtoneChannels.normaliseHue(red / feeders, green / feeders, blue / feeders);
		this.skyHues[cell] = (short) incident;

		final int filter = TINTS_DAYLIGHT.test(state) ? FilterColorHelper.filterFor(state) : FilterColorHelper.FULLY_TRANSMISSIVE;
		this.skyOut[cell] = (short) (filter == FilterColorHelper.FULLY_TRANSMISSIVE ? incident : GlowtoneChannels.filterHue(incident, filter));
	}

	private static int feedCost(boolean downward, int fromLevel, int enteredDampening) {
		if (downward && fromLevel == GlowtoneChannels.MAX_LEVEL && enteredDampening == 0) return 0;
		return Math.max(1, enteredDampening);
	}

	private static boolean withinReach(int rx, int rz, int level) {
		final int dx = Math.max(0, Math.max(WINDOW_MIN - rx, rx - WINDOW_MAX));
		final int dz = Math.max(0, Math.max(WINDOW_MIN - rz, rz - WINDOW_MAX));
		return dx + dz < level;
	}

	private int incidentHue(int cell) {
		return this.skyHues[cell] & GlowtoneChannels.WHITE_HUE;
	}

	private int skyLevelAt(int rx, int ry, int rz) {
		final int cell = cellIndex(rx, ry, rz);
		final byte cached = this.skyLevels[cell];
		if (cached != SKY_UNKNOWN) return cached;

		int level = 0;
		final RenderSectionRegion region = this.region;
		if (region != null) {
			this.scratchPos.set(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz);
			level = region.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(this.scratchPos);
		} else {
			final DataLayer[] layers = this.skyLightLayers;
			if (layers != null) {
				final DataLayer layer = layers[index27(rx >> 4, ry >> 4, rz >> 4)];
				if (layer != null) level = layer.get(rx & 15, ry & 15, rz & 15);
			}
		}

		this.skyLevels[cell] = (byte) level;
		return level;
	}

	private static int tintMask(PalettedContainerRO<BlockState>[] sections) {
		int mask = 0;

		for (int i = 0; i < sections.length; i++) {
			final PalettedContainerRO<BlockState> container = sections[i];
			if (container == null || !container.maybeHas(TINTS_DAYLIGHT)) continue;

			mask |= 1 << i;
		}

		return mask;
	}

	public void release() {
		this.region = null;
		this.skyLightLayers = null;
		this.containersBound = false;
		Arrays.fill(this.containers, null);
		this.sections = null;
		this.debugRegion = false;
		this.lit = false;
		this.skyTinted = false;
	}

	public int cellLevelsAt(int worldX, int worldY, int worldZ) {
		if (!this.lit || !this.containersBound) return 0;

		final int baseX = worldX & ~(ENTITY_CELL_BLOCKS - 1);
		final int baseY = worldY & ~(ENTITY_CELL_BLOCKS - 1);
		final int baseZ = worldZ & ~(ENTITY_CELL_BLOCKS - 1);

		final int cellX = baseX - this.minBlockX - WINDOW_MIN;
		final int cellY = baseY - this.minBlockY - WINDOW_MIN;
		final int cellZ = baseZ - this.minBlockZ - WINDOW_MIN;

		int slot = -1;
		if ((cellX | cellY | cellZ) >= 0
			&& cellX < WINDOW_SPAN - 1 && cellY < WINDOW_SPAN - 1 && cellZ < WINDOW_SPAN - 1
		) {
			slot = ((cellY >> 1) * DOWN_SPAN + (cellZ >> 1)) * DOWN_SPAN + (cellX >> 1);
			if (this.downsampledStamp[slot] == this.stateGeneration) {
				return this.downsampled[slot] & GlowtoneChannels.LEVEL_MASK;
			}
		}

		int best = 0;
		for (int dy = 0; dy < ENTITY_CELL_BLOCKS; dy++) {
			for (int dz = 0; dz < ENTITY_CELL_BLOCKS; dz++) {
				for (int dx = 0; dx < ENTITY_CELL_BLOCKS; dx++) {
					final int rx = baseX + dx - this.minBlockX;
					final int ry = baseY + dy - this.minBlockY;
					final int rz = baseZ + dz - this.minBlockZ;
					if (isOutside(rx, ry, rz)) continue;

					best = GlowtoneChannels.merge(
						best,
						this.levels[cellIndex(rx, ry, rz)] & GlowtoneChannels.LEVEL_MASK
					);
				}
			}
		}

		if (slot >= 0) {
			this.downsampled[slot] = (short) (best & GlowtoneChannels.LEVEL_MASK);
			this.downsampledStamp[slot] = this.stateGeneration;
		}
		return best;
	}

	public int levelsAt(int worldX, int worldY, int worldZ) {
		if (!this.lit || !this.containersBound) return 0;

		int rx = worldX - this.minBlockX;
		int ry = worldY - this.minBlockY;
		int rz = worldZ - this.minBlockZ;
		if (isOutside(rx, ry, rz)) return 0;

		return this.levels[cellIndex(rx, ry, rz)] & GlowtoneChannels.LEVEL_MASK;
	}

	public BlockState stateAt(int worldX, int worldY, int worldZ) {
		if (!this.containersBound) return AIR;

		final int rx = worldX - this.minBlockX;
		final int ry = worldY - this.minBlockY;
		final int rz = worldZ - this.minBlockZ;
		if (isOutside(rx, ry, rz)) return AIR;

		return this.stateAt(cellIndex(rx, ry, rz), rx, ry, rz);
	}

	public short @Nullable [] downsampleCentre() {
		if (this.levels == null) return null;

		short[] payload = null;

		for (int cellY = 0; cellY < ENTITY_SPAN; cellY++) {
			for (int cellZ = 0; cellZ < ENTITY_SPAN; cellZ++) {
				for (int cellX = 0; cellX < ENTITY_SPAN; cellX++) {
					int best = 0;
					for (int dy = 0; dy < 2; dy++) {
						for (int dz = 0; dz < 2; dz++) {
							for (int dx = 0; dx < 2; dx++) {
								int packed = this.levels[cellIndex(
									16 + (cellX << 1) + dx,
									16 + (cellY << 1) + dy,
									16 + (cellZ << 1) + dz
								)] & GlowtoneChannels.LEVEL_MASK;
								best = GlowtoneChannels.merge(best, packed);
							}
						}
					}

					if (best == 0) continue;
					if (payload == null) payload = new short[ENTITY_CELLS];
					payload[(cellY * ENTITY_SPAN + cellZ) * ENTITY_SPAN + cellX] =
						(short) (best & GlowtoneChannels.LEVEL_MASK);
				}
			}
		}

		return payload;
	}

	public short @Nullable [] downsampleCentreSky() {
		if (!this.skyTinted) return null;

		short[] payload = null;

		for (int cellY = 0; cellY < ENTITY_SPAN; cellY++) {
			for (int cellZ = 0; cellZ < ENTITY_SPAN; cellZ++) {
				for (int cellX = 0; cellX < ENTITY_SPAN; cellX++) {
					int red = 0;
					int green = 0;
					int blue = 0;
					boolean tinted = false;

					int counted = 0;
					for (int dy = 0; dy < 2; dy++) {
						for (int dz = 0; dz < 2; dz++) {
							for (int dx = 0; dx < 2; dx++) {
								final int rx = 16 + (cellX << 1) + dx;
								final int ry = 16 + (cellY << 1) + dy;
								final int rz = 16 + (cellZ << 1) + dz;
								final BlockState state = this.stateAt(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz);
								if (state.getLightDampening() >= GlowtoneChannels.MAX_LEVEL || TINTS_DAYLIGHT.test(state)) continue;

								int hue = this.incidentHue(cellIndex(rx, ry, rz));
								if (hue == 0) {
									hue = GlowtoneChannels.WHITE_HUE;
								} else {
									tinted = true;
								}

								red += (hue >> 8) & 0xF;
								green += (hue >> 4) & 0xF;
								blue += hue & 0xF;
								counted++;
							}
						}
					}

					if (!tinted || counted == 0) continue;

					final int hue = GlowtoneChannels.normaliseHue(red / counted, green / counted, blue / counted);
					if (hue == GlowtoneChannels.WHITE_HUE) continue;

					if (payload == null) payload = new short[ENTITY_CELLS];
					payload[(cellY * ENTITY_SPAN + cellZ) * ENTITY_SPAN + cellX] = (short) hue;
				}
			}
		}

		return payload;
	}

	public static int skyHueToRgb(int hue) {
		return hue == 0 ? WHITE_RGB : GlowtoneChannels.toNormalisedRgb(GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, hue));
	}

	private static int emitterMask(PalettedContainerRO<BlockState>[] sections) {
		int mask = 0;

		for (int i = 0; i < sections.length; i++) {
			final PalettedContainerRO<BlockState> container = sections[i];
			if (container == null || !container.maybeHas(EMITS_LIGHT)) continue;

			mask |= 1 << i;
		}

		return mask;
	}

	private void seedDynamicLights() {
		final int[] dynamic = GlowtoneDynamicLights.get().snapshot();

		for (int index = 0; index < dynamic.length; index += GlowtoneDynamicLights.STRIDE) {
			final int luminance = dynamic[index + 3];
			if (luminance <= 0) continue;

			this.seedDynamicLight(
				Float.intBitsToFloat(dynamic[index + 5]) - this.minBlockX,
				Float.intBitsToFloat(dynamic[index + 6]) - this.minBlockY,
				Float.intBitsToFloat(dynamic[index + 7]) - this.minBlockZ,
				luminance,
				dynamic[index + 4]
			);
		}
	}

	private void seedDynamicLight(double sourceX, double sourceY, double sourceZ, int luminance, int rgb) {
		final int reach = (int) Math.ceil(GlowtoneDynamicLights.RADIUS);
		final int minX = Math.max(0, (int) Math.floor(sourceX) - reach);
		final int minY = Math.max(0, (int) Math.floor(sourceY) - reach);
		final int minZ = Math.max(0, (int) Math.floor(sourceZ) - reach);
		final int maxX = Math.min(SPAN - 1, (int) Math.floor(sourceX) + reach);
		final int maxY = Math.min(SPAN - 1, (int) Math.floor(sourceY) + reach);
		final int maxZ = Math.min(SPAN - 1, (int) Math.floor(sourceZ) + reach);

		for (int y = minY; y <= maxY; y++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					final int level = GlowtoneDynamicLights.levelAt(
						x - sourceX + 0.5D, y - sourceY + 0.5D, z - sourceZ + 0.5D, luminance
					);
					if (level <= 0) continue;

					final int packed = GlowtoneChannels.emissionLevels(level, rgb);
					if (packed == 0) continue;
					if (!reaches(GlowtoneChannels.level(packed), x, y, z)) continue;

					final int cell = cellIndex(x, y, z);
					final int merged = GlowtoneChannels.merge(this.levels[cell] & GlowtoneChannels.LEVEL_MASK, packed);
					this.levels[cell] = (short) (merged & GlowtoneChannels.LEVEL_MASK);
					this.lit = true;
				}
			}
		}
	}

	private void seed(PalettedContainerRO<BlockState>[] sections, int emitterMask) {
		for (int sectionZ = 0; sectionZ < SECTION_GRID; sectionZ++) {
			for (int sectionY = 0; sectionY < SECTION_GRID; sectionY++) {
				for (int sectionX = 0; sectionX < SECTION_GRID; sectionX++) {
					int slot = index27(sectionX, sectionY, sectionZ);
					if ((emitterMask & (1 << slot)) == 0) continue;

					final PalettedContainerRO<BlockState> container = sections[slot];
					if (container == null) continue;

					this.seedSection(container, sectionX << 4, sectionY << 4, sectionZ << 4);
				}
			}
		}
	}

	private void seedSection(PalettedContainerRO<BlockState> container, int baseX, int baseY, int baseZ) {
		for (int localY = 0; localY < 16; localY++) {
			final int ry = baseY + localY;
			final int distanceY = axisDistance(ry);
			if (distanceY >= GlowtoneChannels.MAX_LEVEL) continue;

			for (int localZ = 0; localZ < 16; localZ++) {
				int rz = baseZ + localZ;
				int distanceYZ = distanceY + axisDistance(rz);
				if (distanceYZ >= GlowtoneChannels.MAX_LEVEL) continue;

				for (int localX = 0; localX < 16; localX++) {
					final int rx = baseX + localX;
					if (distanceYZ + axisDistance(rx) >= GlowtoneChannels.MAX_LEVEL) continue;

					final int cell = cellIndex(rx, ry, rz);
					final BlockState state = container.get(localX, localY, localZ);
					this.states[cell] = state;
					this.stateStamp[cell] = this.stateGeneration;

					final int emission = state.getLightEmission();
					if (emission <= 0) continue;

					final int packed = GlowtoneChannels.emissionLevels(emission, EmitterColorHelper.rgbFor(state));
					if (packed == 0) continue;
					if (!reaches(GlowtoneChannels.level(packed), rx, ry, rz)) continue;

					final int merged = GlowtoneChannels.merge(this.levels[cell] & GlowtoneChannels.LEVEL_MASK, packed);
					this.levels[cell] = (short) (merged & GlowtoneChannels.LEVEL_MASK);
					this.lit = true;
					this.enqueue(rx, ry, rz, merged, LightEngine.isEmptyShape(state), 0);
				}
			}
		}
	}

	private void propagate() {
		int expansions = 0;

		for (int level = GlowtoneChannels.MAX_LEVEL; level >= 2; level--) {
			final int[] bucket = this.buckets[level];
			final int size = this.bucketSizes[level];
			if (bucket == null || size == 0) continue;

			for (int i = 0; i < size; i++) {
				final int entry = bucket[i];
				final int rx = entry & ENTRY_COORD_MASK;
				final int ry = (entry >>> ENTRY_Y_SHIFT) & ENTRY_COORD_MASK;
				final int rz = (entry >>> ENTRY_Z_SHIFT) & ENTRY_COORD_MASK;

				final int cell = cellIndex(rx, ry, rz);
				final int packed = this.levels[cell] & GlowtoneChannels.LEVEL_MASK;
				if (GlowtoneChannels.level(packed) != level) continue;

				if (++expansions > MAX_EXPANSIONS) return;

				this.propagateFrom(
					cell, rx, ry, rz, packed,
					(entry & ENTRY_EMPTY_SHAPE) != 0,
					(entry >>> ENTRY_SKIP_SHIFT) & 0x3F
				);
			}
		}
	}

	private void propagateFrom(int cell, int x, int y, int z, int packed, boolean fromEmptyShape, int skipMask) {
		final int reduced = GlowtoneChannels.subtract(packed, 1);
		if (reduced == 0) return;

		final int reducedLevel = GlowtoneChannels.level(reduced);
		BlockState fromState = null;

		for (Direction direction : DIRECTIONS) {
			if ((skipMask & (1 << direction.ordinal())) != 0) continue;

			final int neighbourX = x + direction.getStepX();
			final int neighbourY = y + direction.getStepY();
			final int neighbourZ = z + direction.getStepZ();
			if (isOutside(neighbourX, neighbourY, neighbourZ)) continue;

			final int neighbourDistance = axisDistance(neighbourX) + axisDistance(neighbourY) + axisDistance(neighbourZ);
			if (reducedLevel - neighbourDistance <= 0) continue;

			final int neighbourCell = cellIndex(neighbourX, neighbourY, neighbourZ);
			final int stored = this.levels[neighbourCell] & GlowtoneChannels.LEVEL_MASK;
			final boolean brighter = GlowtoneChannels.anyGreater(reduced, stored);
			if (!brighter && !canBlend(reduced, stored, packed)) continue;

			final BlockState toState = this.stateAt(neighbourCell, neighbourX, neighbourY, neighbourZ);
			final int next = GlowtoneChannels.attenuate(
				packed,
				Math.max(1, toState.getLightDampening()),
				FilterColorHelper.filterFor(toState)
			);
			final boolean nextBrighter = GlowtoneChannels.anyGreater(next, stored);
			if (!nextBrighter && !canBlend(next, stored, next)) continue;
			if (GlowtoneChannels.level(next) - neighbourDistance <= 0) continue;

			if (fromState == null) fromState = fromEmptyShape ? AIR : this.stateAt(cell, x, y, z);

			if (shapeOccludes(fromState, toState, direction)) continue;

			if (!nextBrighter) {
				final int blended = GlowtoneChannels.blendHues(stored, next, GlowtoneChannels.level(stored), GlowtoneChannels.level(next));
				if (blended != stored) this.levels[neighbourCell] = (short) (blended & GlowtoneChannels.LEVEL_MASK);
				continue;
			}

			final int merged = GlowtoneChannels.merge(next, stored);
			this.levels[neighbourCell] = (short) (merged & GlowtoneChannels.LEVEL_MASK);
			this.lit = true;

			if (GlowtoneChannels.level(merged) > 1) {
				this.enqueue(
					neighbourX, neighbourY, neighbourZ,
					merged,
					LightEngine.isEmptyShape(toState),
					1 << direction.getOpposite().ordinal()
				);
			}
		}
	}

	private static boolean canBlend(int candidate, int stored, int hueSource) {
		final int storedLevel = GlowtoneChannels.level(stored);
		final int candidateLevel = GlowtoneChannels.level(candidate);
		return storedLevel > 0
			&& candidateLevel <= storedLevel
			&& candidateLevel + BLEND_TOLERANCE >= storedLevel
			&& GlowtoneChannels.hue(hueSource) != GlowtoneChannels.hue(stored);
	}

	private static final int BLEND_TOLERANCE = 6;

	private void enqueue(int x, int y, int z, int packed, boolean fromEmptyShape, int skipMask) {
		final int level = GlowtoneChannels.level(packed);
		int[] bucket = this.buckets[level];
		final int size = this.bucketSizes[level];
		if (bucket == null) {
			bucket = this.buckets[level] = new int[256];
		} else if (size == bucket.length) {
			bucket = this.buckets[level] = Arrays.copyOf(bucket, size * 2);
		}

		bucket[size] = x
			| (y << ENTRY_Y_SHIFT)
			| (z << ENTRY_Z_SHIFT)
			| (fromEmptyShape ? ENTRY_EMPTY_SHAPE : 0)
			| (skipMask << ENTRY_SKIP_SHIFT);
		this.bucketSizes[level] = size + 1;
	}

	private static boolean shapeOccludes(BlockState from, BlockState to, Direction direction) {
		return Shapes.faceShapeOccludes(
			LightEngine.getOcclusionShape(from, direction),
			LightEngine.getOcclusionShape(to, direction.getOpposite())
		);
	}

	private BlockState stateAt(int cell, int x, int y, int z) {
		if (this.stateStamp[cell] == this.stateGeneration) {
			final BlockState cached = this.states[cell];
			if (cached != null) return cached;
		}

		final BlockState state = this.readState(x, y, z);
		this.states[cell] = state;
		this.stateStamp[cell] = this.stateGeneration;
		return state;
	}

	private BlockState readState(int x, int y, int z) {
		if (this.debugRegion) {
			final RenderSectionRegion region = this.region;
			return region == null
				? AIR
				: region.getBlockState(this.scratchPos.set(this.minBlockX + x, this.minBlockY + y, this.minBlockZ + z));
		}

		if (!this.containersBound) return AIR;

		final PalettedContainerRO<BlockState> container = this.containers[index27(x >> 4, y >> 4, z >> 4)];
		return container == null ? AIR : container.get(x & 15, y & 15, z & 15);
	}

	private static int cellIndex(int x, int y, int z) {
		return (y * SPAN + z) * SPAN + x;
	}

	private static int index27(int sectionX, int sectionY, int sectionZ) {
		return sectionX + sectionY * SECTION_GRID + sectionZ * SECTION_GRID * SECTION_GRID;
	}

	private static boolean isOutside(int x, int y, int z) {
		return (x | y | z) < 0 || x >= SPAN || y >= SPAN || z >= SPAN;
	}

	private static boolean reaches(int level, int x, int y, int z) {
		return level - (axisDistance(x) + axisDistance(y) + axisDistance(z)) > 0;
	}

	private static int axisDistance(int coordinate) {
		if (coordinate < WINDOW_MIN) return WINDOW_MIN - coordinate;
		if (coordinate > WINDOW_MAX) return coordinate - WINDOW_MAX;
		return 0;
	}
}
