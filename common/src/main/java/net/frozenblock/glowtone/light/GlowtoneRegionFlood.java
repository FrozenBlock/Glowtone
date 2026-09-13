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

import com.mojang.logging.LogUtils;
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
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneRegionFlood {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int SPAN = 48;
	public static final int WHITE_RGB = 0xFFFFFF;
	public static final short[] NO_SKY_TINT = new short[0];

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
	private static int expansionTrips;
	private static volatile boolean skyTintDimension = true;
	private static final byte SKY_UNKNOWN = -1;
	private static final byte ROW_UNKNOWN = -1;
	private static final byte SKY_MARKED = 1;
	private static final byte SKY_FRINGE = 2;
	private static final byte SKY_SOURCE = 4;
	private static final byte SKY_RESOLVED = SKY_MARKED | SKY_FRINGE;
	private static final int[] SKY_PATH_WEIGHT = {100, 80, 64, 51, 41, 33};
	private static final int SECTION_GRID = RenderSectionRegion.SIZE;
	private static final int SECTION_SLOTS = SECTION_GRID * SECTION_GRID * SECTION_GRID;
	private static final int SECTION_COLUMNS = 16 * 16;

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
	private @Nullable BlockState[] states;

	private short[] skyHues;
	private short[] skyOut;
	private byte[] skyMarked;
	private int[] skyOrder = new int[1024];
	private int[] skyResolve = new int[1024];
	private int skyCount;
	private boolean skyRestored;
	private boolean skyEvaluated;
	private final int[] skyLevelCounts = new int[GlowtoneChannels.MAX_LEVEL + 1];
	private final IntArrayFIFOQueue skyQueue = new IntArrayFIFOQueue();
	private final DataLayer @Nullable [] skyLayers = new DataLayer[SECTION_SLOTS];
	private final byte[] skyColumnLevels = new byte[SECTION_SLOTS * SECTION_COLUMNS];
	private final byte[] skyRowFull = new byte[SECTION_SLOTS * 16];
	private final byte[] skySectionFull = new byte[SECTION_SLOTS];
	private final SkyTintColumns.@Nullable Chunk[] tintChunks = new SkyTintColumns.Chunk[SECTION_GRID * SECTION_GRID];

	private final int[][] buckets = new int[GlowtoneChannels.MAX_LEVEL + 1][];
	private final int[] bucketSizes = new int[GlowtoneChannels.MAX_LEVEL + 1];

	private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

	private @Nullable RenderSectionRegion region;
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
		return new PalettedContainerRO[SECTION_SLOTS];
	}

	public boolean begin(
		PalettedContainerRO<BlockState>[] grid, DataLayer @Nullable [] skyGrid,
		int minSectionX, int minSectionY, int minSectionZ,
		short @Nullable [] cachedWindow, short @Nullable [] cachedSky
	) {
		this.release();

		if (grid.length != SECTION_SLOTS) return false;

		System.arraycopy(grid, 0, this.containers, 0, grid.length);

		final int emitterMask = emitterMask(this.containers);
		final boolean dynamic = GlowtoneDynamicLights.get().anyWithin(minSectionX << 4, minSectionY << 4, minSectionZ << 4, SPAN);
		final boolean sky = skyTintActive() && skyGrid != null && skyGrid.length == SECTION_SLOTS;
		if (sky) System.arraycopy(skyGrid, 0, this.skyLayers, 0, SECTION_SLOTS);
		return this.beginBound(null, minSectionX, minSectionY, minSectionZ, emitterMask, dynamic, sky, cachedWindow, cachedSky);
	}

	public boolean begin(
		RenderSectionRegion region, int centreSectionX, int centreSectionY, int centreSectionZ,
		short @Nullable [] cachedWindow, short @Nullable [] cachedSky
	) {
		this.release();

		final SectionCopy[] sections = region.sections;
		if (sections == null || sections.length != SECTION_SLOTS) {
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
		final boolean dynamic = GlowtoneDynamicLights.get()
			.anyWithin(minSectionX << 4, minSectionY << 4, minSectionZ << 4, SPAN);
		final boolean sky = skyTintActive();
		if (sky) this.bindSkyLayers(region, minSectionX, minSectionY, minSectionZ);

		this.region = region;
		this.sections = sections;
		this.debugRegion = sections[index27(1, 1, 1)].debug;
		return this.beginBound(region, minSectionX, minSectionY, minSectionZ, emitterMask, dynamic, sky, cachedWindow, cachedSky);
	}

	private boolean beginBound(
		@Nullable RenderSectionRegion region, int minSectionX, int minSectionY, int minSectionZ,
		int emitterMask, boolean dynamic, boolean sky,
		short @Nullable [] cachedWindow, short @Nullable [] cachedSky
	) {
		final boolean tintColumns = sky && this.bindTintChunks(minSectionX, minSectionZ);
		if (emitterMask == 0 && !tintColumns && !dynamic) {
			this.region = null;
			this.sections = null;
			this.debugRegion = false;
			this.skyEvaluated = sky;
			Arrays.fill(this.skyLayers, null);
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
		this.containersBound = true;
		this.minBlockX = minSectionX << 4;
		this.minBlockY = minSectionY << 4;
		this.minBlockZ = minSectionZ << 4;
		this.lit = false;
		this.skyTinted = false;
		this.skyEvaluated = sky;

		this.nextStateGeneration();
		if (tintColumns) {
			this.allocateSky();
			if (cachedSky != null) {
				if (cachedSky.length == WINDOW_CELLS) this.restoreSkyWindow(cachedSky);
			} else {
				this.floodSkyTint();
			}
		}

		this.dynamicSeeded = dynamic;
		if (cachedWindow != null && !dynamic) {
			this.restoreWindow(cachedWindow);
			return this.lit || this.skyTinted;
		}

		if (emitterMask != 0 || dynamic) {
			Arrays.fill(this.levels, (short) 0);
			Arrays.fill(this.bucketSizes, 0);
			this.levelsBeyondWindow = true;

			this.seed(this.containers, emitterMask);
			this.propagate();
			if (dynamic) this.seedDynamicLights();
		}
		return this.lit || this.skyTinted;
	}

	private void bindSkyLayers(RenderSectionRegion region, int minSectionX, int minSectionY, int minSectionZ) {
		final LayerLightEventListener listener = region.getLightEngine().getLayerListener(LightLayer.SKY);
		boolean missing = false;
		for (int sectionZ = 0; sectionZ < SECTION_GRID; sectionZ++) {
			for (int sectionY = 0; sectionY < SECTION_GRID; sectionY++) {
				for (int sectionX = 0; sectionX < SECTION_GRID; sectionX++) {
					final DataLayer layer = listener.getDataLayerData(
						SectionPos.of(minSectionX + sectionX, minSectionY + sectionY, minSectionZ + sectionZ)
					);
					this.skyLayers[index27(sectionX, sectionY, sectionZ)] = layer;
					missing |= layer == null;
				}
			}
		}
		if (missing) Arrays.fill(this.skyColumnLevels, SKY_UNKNOWN);
	}

	private boolean bindTintChunks(int minSectionX, int minSectionZ) {
		boolean any = false;
		for (int chunkZ = 0; chunkZ < SECTION_GRID; chunkZ++) {
			for (int chunkX = 0; chunkX < SECTION_GRID; chunkX++) {
				final SkyTintColumns.Chunk chunk = SkyTintColumns.chunk(minSectionX + chunkX, minSectionZ + chunkZ);
				this.tintChunks[chunkX + chunkZ * SECTION_GRID] = chunk;
				any |= chunk != null;
			}
		}
		return any;
	}

	public short @Nullable [] extractSkyWindow() {
		if (!this.skyEvaluated) return null;
		if (!this.skyTinted || this.skyHues == null) return NO_SKY_TINT;

		final short[] window = new short[WINDOW_CELLS];
		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				System.arraycopy(this.skyHues, cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z), window, at, WINDOW_SPAN);
				at += WINDOW_SPAN;
			}
		}
		return window;
	}

	private void restoreSkyWindow(short[] window) {
		int at = 0;
		for (int y = 0; y < WINDOW_SPAN; y++) {
			for (int z = 0; z < WINDOW_SPAN; z++) {
				System.arraycopy(window, at, this.skyHues, cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z), WINDOW_SPAN);
				at += WINDOW_SPAN;
			}
		}
		this.skyRestored = true;
		this.skyTinted = true;
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
		if (hue == GlowtoneChannels.WHITE_HUE) return WHITE_RGB;

		return GlowtoneChannels.toNormalisedRgb(GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, hue));
	}

	private void allocateSky() {
		if (this.skyHues != null) return;
		this.skyHues = new short[CELLS];
		this.skyOut = new short[CELLS];
		this.skyMarked = new byte[CELLS];
	}

	private void floodSkyTint() {
		this.skyQueue.clear();
		Arrays.fill(this.skyRowFull, ROW_UNKNOWN);
		Arrays.fill(this.skySectionFull, ROW_UNKNOWN);

		for (int chunkIndex = 0; chunkIndex < this.tintChunks.length; chunkIndex++) {
			final SkyTintColumns.Chunk chunk = this.tintChunks[chunkIndex];
			if (chunk == null) continue;

			final int baseX = (chunkIndex % SECTION_GRID) << 4;
			final int baseZ = (chunkIndex / SECTION_GRID) << 4;
			for (int index = 0; index < SECTION_COLUMNS; index++) {
				final int[] column = chunk.column(index);
				if (column == null) continue;

				final int rx = baseX + (index & 15);
				final int rz = baseZ + (index >> 4);
				if (!withinReach(rx, rz, GlowtoneChannels.MAX_LEVEL)) continue;
				this.walkColumn(column, rx, rz);
			}
		}

		this.spreadSky();
		this.skyTinted = this.windowTinted();
	}

	private void walkColumn(int[] column, int rx, int rz) {
		final int topY = this.minBlockY + SPAN - 1;
		int at = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		int layers = 0;
		while (at < column.length && SkyTintColumns.entryY(column[at]) > topY) {
			final int filter = SkyTintColumns.entryFilter(column[at++]);
			red += (filter >> 8) & 0xF;
			green += (filter >> 4) & 0xF;
			blue += filter & 0xF;
			layers++;
		}
		int above = GlowtoneChannels.meanHue(red, green, blue, layers);

		final boolean windowColumn = axisDistance(rx) == 0 && axisDistance(rz) == 0;
		final int sectionX = rx >> 4;
		final int sectionZ = rz >> 4;
		for (int sectionY = SECTION_GRID - 1; sectionY >= 0; sectionY--) {
			final int slot = index27(sectionX, sectionY, sectionZ);
			final int bottom = sectionY << 4;
			final boolean entries = at < column.length && SkyTintColumns.entryY(column[at]) >= this.minBlockY + bottom;
			if (!entries && this.sectionFull(slot)) {
				if (above == GlowtoneChannels.WHITE_HUE) continue;
				if (windowColumn) {
					for (int ry = bottom + 15; ry >= bottom; ry--) {
						if (axisDistance(ry) == 0) this.writeSkySource(cellIndex(rx, ry, rz), above);
					}
				}
				if (!this.neighbourSectionsFull(sectionX, sectionY, sectionZ)) {
					for (int ry = bottom + 15; ry >= bottom; ry--) {
						if (!this.skyInterior(slot, rx, ry, rz)) this.seedFromSkySource(rx, ry, rz);
					}
				}
				continue;
			}

			for (int ry = bottom + 15; ry >= bottom; ry--) {
				int through = above;
				if (at < column.length && SkyTintColumns.entryY(column[at]) == this.minBlockY + ry) {
					final int filter = SkyTintColumns.entryFilter(column[at++]);
					red += (filter >> 8) & 0xF;
					green += (filter >> 4) & 0xF;
					blue += filter & 0xF;
					layers++;
					through = GlowtoneChannels.meanHue(red, green, blue, layers);
				}

				if (this.rowFull(slot, ry & 15) || this.skyLevelAt(rx, ry, rz) == GlowtoneChannels.MAX_LEVEL) {
					if (windowColumn && above != GlowtoneChannels.WHITE_HUE && axisDistance(ry) == 0) {
						this.writeSkySource(cellIndex(rx, ry, rz), above);
					}
					if (through != GlowtoneChannels.WHITE_HUE && !this.skyInterior(slot, rx, ry, rz)) this.seedFromSkySource(rx, ry, rz);
				}
				above = through;
			}
		}
	}

	private boolean sectionFull(int slot) {
		final byte known = this.skySectionFull[slot];
		if (known != ROW_UNKNOWN) return known != 0;

		boolean full = true;
		for (int row = 0; row < 16 && full; row++) full = this.rowFull(slot, row);
		this.skySectionFull[slot] = (byte) (full ? 1 : 0);
		return full;
	}

	private boolean neighbourSectionsFull(int sectionX, int sectionY, int sectionZ) {
		for (Direction direction : DIRECTIONS) {
			final int nx = sectionX + direction.getStepX();
			final int ny = sectionY + direction.getStepY();
			final int nz = sectionZ + direction.getStepZ();
			if ((nx | ny | nz) < 0 || nx >= SECTION_GRID || ny >= SECTION_GRID || nz >= SECTION_GRID) continue;
			if (!this.sectionFull(index27(nx, ny, nz))) return false;
		}
		return true;
	}

	private boolean rowFull(int slot, int row) {
		final byte known = this.skyRowFull[slot * 16 + row];
		if (known != ROW_UNKNOWN) return known != 0;

		final DataLayer layer = this.skyLayers[slot];
		boolean full = false;
		if (layer != null) {
			if (layer.isDefinitelyHomogenous()) {
				full = layer.isDefinitelyFilledWith(GlowtoneChannels.MAX_LEVEL);
			} else {
				final byte[] data = layer.getData();
				full = true;
				for (int i = row << 7, end = (row + 1) << 7; i < end; i++) {
					if (data[i] != (byte) 0xFF) {
						full = false;
						break;
					}
				}
			}
		}
		this.skyRowFull[slot * 16 + row] = (byte) (full ? 1 : 0);
		return full;
	}

	private boolean skyInterior(int slot, int rx, int ry, int rz) {
		final int lx = rx & 15;
		final int ly = ry & 15;
		final int lz = rz & 15;
		if (lx == 0 || lx == 15 || lz == 0 || lz == 15) return false;
		if (!this.rowFull(slot, ly)) return false;

		if (ly > 0) {
			if (!this.rowFull(slot, ly - 1)) return false;
		} else if (ry == 0 || !this.rowFull(index27(rx >> 4, (ry >> 4) - 1, rz >> 4), 15)) {
			return false;
		}

		if (ly < 15) return this.rowFull(slot, ly + 1);
		return ry != SPAN - 1 && this.rowFull(index27(rx >> 4, (ry >> 4) + 1, rz >> 4), 0);
	}

	private void writeSkySource(int cell, int hue) {
		this.skyHues[cell] = (short) hue;
		this.skyMarked[cell] |= SKY_SOURCE;
		this.appendSkyCell(cell);
	}

	private void seedFromSkySource(int rx, int ry, int rz) {
		BlockState fromState = null;
		for (Direction direction : DIRECTIONS) {
			final int nx = rx + direction.getStepX();
			final int ny = ry + direction.getStepY();
			final int nz = rz + direction.getStepZ();
			if (isOutside(nx, ny, nz)) continue;

			final int neighbour = this.skyLevelAt(nx, ny, nz);
			if (neighbour <= 0 || neighbour >= GlowtoneChannels.MAX_LEVEL) continue;

			final int next = cellIndex(nx, ny, nz);
			if (this.skyMarked[next] != 0) continue;

			final BlockState toState = this.stateAt(next, nx, ny, nz);
			final int dampening = toState.getLightDampening();
			if (dampening >= GlowtoneChannels.MAX_LEVEL) continue;
			if (GlowtoneChannels.MAX_LEVEL - Math.max(1, dampening) != neighbour) continue;
			if (!withinReach(nx, nz, neighbour)) continue;

			if (fromState == null) fromState = this.stateAt(cellIndex(rx, ry, rz), rx, ry, rz);
			if (shapeOccludes(fromState, toState, direction)) continue;

			this.skyMarked[next] = SKY_MARKED;
			this.appendSkyCell(next);
			this.skyQueue.enqueue(next);
		}
	}

	private void spreadSky() {
		while (!this.skyQueue.isEmpty()) {
			final int cell = this.skyQueue.dequeueInt();
			final int rx = cell % SPAN;
			final int rz = (cell / SPAN) % SPAN;
			final int ry = cell / SPAN_SQ;
			final int level = this.skyLevelAt(rx, ry, rz);
			BlockState fromState = null;

			for (Direction direction : DIRECTIONS) {
				final int nx = rx + direction.getStepX();
				final int ny = ry + direction.getStepY();
				final int nz = rz + direction.getStepZ();
				if (isOutside(nx, ny, nz)) continue;

				final int neighbour = this.skyLevelAt(nx, ny, nz);
				if (neighbour <= 0 || neighbour >= GlowtoneChannels.MAX_LEVEL) continue;

				final int next = cellIndex(nx, ny, nz);
				if (this.skyMarked[next] != 0) continue;

				final BlockState toState = this.stateAt(next, nx, ny, nz);
				final int dampening = toState.getLightDampening();
				if (dampening >= GlowtoneChannels.MAX_LEVEL) continue;
				if (level - Math.max(1, dampening) != neighbour) continue;
				if (!withinReach(nx, nz, neighbour)) continue;

				if (fromState == null) fromState = this.stateAt(cell, rx, ry, rz);
				if (shapeOccludes(fromState, toState, direction)) continue;

				this.skyMarked[next] = SKY_MARKED;
				this.appendSkyCell(next);
				this.skyQueue.enqueue(next);
			}
		}

		final int core = this.skyCount;
		for (int i = 0; i < core; i++) {
			final int cell = this.skyOrder[i];
			if ((this.skyMarked[cell] & SKY_MARKED) == 0) continue;

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

				final int neighbour = this.skyLevelAt(nx, ny, nz);
				if (neighbour <= 0 || neighbour >= GlowtoneChannels.MAX_LEVEL) continue;
				if (!withinReach(nx, nz, neighbour)) continue;
				if (this.stateAt(next, nx, ny, nz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;

				this.skyMarked[next] = SKY_FRINGE;
				this.appendSkyCell(next);
			}
		}

		final int[] counts = this.skyLevelCounts;
		Arrays.fill(counts, 0);
		for (int i = 0; i < this.skyCount; i++) {
			final int cell = this.skyOrder[i];
			if ((this.skyMarked[cell] & SKY_RESOLVED) == 0) continue;
			counts[this.skyLevelAt(cell % SPAN, cell / SPAN_SQ, (cell / SPAN) % SPAN)]++;
		}

		int resolvable = 0;
		for (int level = GlowtoneChannels.MAX_LEVEL; level >= 0; level--) {
			final int count = counts[level];
			counts[level] = resolvable;
			resolvable += count;
		}
		if (this.skyResolve.length < resolvable) this.skyResolve = new int[Math.max(resolvable, this.skyResolve.length * 2)];

		for (int i = 0; i < this.skyCount; i++) {
			final int cell = this.skyOrder[i];
			if ((this.skyMarked[cell] & SKY_RESOLVED) == 0) continue;
			this.skyResolve[counts[this.skyLevelAt(cell % SPAN, cell / SPAN_SQ, (cell / SPAN) % SPAN)]++] = cell;
		}

		for (int i = 0; i < resolvable; i++) this.resolveSkyCell(this.skyResolve[i]);
		for (int i = 0; i < resolvable; i++) this.blendSkyCell(this.skyResolve[i]);
	}

	private void resolveSkyCell(int cell) {
		final int rx = cell % SPAN;
		final int rz = (cell / SPAN) % SPAN;
		final int ry = cell / SPAN_SQ;
		final int level = this.skyLevelAt(rx, ry, rz);
		final BlockState state = this.stateAt(cell, rx, ry, rz);
		final int cost = Math.max(1, state.getLightDampening());

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
			if (feeder - cost != level) continue;

			final int from = cellIndex(fx, fy, fz);
			final BlockState fromState = this.stateAt(from, fx, fy, fz);
			if (fromState.getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;
			if (shapeOccludes(fromState, state, direction.getOpposite())) continue;

			final int hue = this.outgoingHue(from, fx, fy, fz, feeder);
			red += (hue >> 8) & 0xF;
			green += (hue >> 4) & 0xF;
			blue += hue & 0xF;
			feeders++;
		}

		final int incident = GlowtoneChannels.meanHue(red, green, blue, feeders);
		this.skyHues[cell] = (short) incident;

		final int filter = this.filterAt(rx, ry, rz);
		this.skyOut[cell] = (short) (filter == SkyTintColumns.NO_FILTER ? incident : GlowtoneChannels.mixHue(incident, filter));
	}

	private void blendSkyCell(int cell) {
		final int rx = cell % SPAN;
		final int rz = (cell / SPAN) % SPAN;
		final int ry = cell / SPAN_SQ;
		final int level = this.skyLevelAt(rx, ry, rz);
		final int cost = Math.max(1, this.stateAt(cell, rx, ry, rz).getLightDampening());

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

			final int deficit = Math.max(0, level - (neighbour - cost));
			if (deficit >= SKY_PATH_WEIGHT.length) continue;

			final int from = cellIndex(nx, ny, nz);
			if (this.stateAt(from, nx, ny, nz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;
			if (deficit > 0 && this.filterAt(nx, ny, nz) != SkyTintColumns.NO_FILTER) continue;

			final int hue = this.outgoingHue(from, nx, ny, nz, neighbour);
			final int weight = SKY_PATH_WEIGHT[deficit];
			red += ((hue >> 8) & 0xF) * weight;
			green += ((hue >> 4) & 0xF) * weight;
			blue += (hue & 0xF) * weight;
			total += weight;
		}

		if (total == 0) return;
		this.skyHues[cell] = (short) GlowtoneChannels.meanHue(red, green, blue, total);
	}

	private int outgoingHue(int cell, int rx, int ry, int rz, int level) {
		if (level >= GlowtoneChannels.MAX_LEVEL) {
			return SkyTintColumns.tintThrough(this.tintColumn(rx, rz), this.minBlockY + ry);
		}
		if ((this.skyMarked[cell] & SKY_RESOLVED) != 0) return this.skyOut[cell] & GlowtoneChannels.WHITE_HUE;
		return GlowtoneChannels.WHITE_HUE;
	}

	private boolean windowTinted() {
		for (int i = 0; i < this.skyCount; i++) {
			final int cell = this.skyOrder[i];
			if (this.incidentHue(cell) == GlowtoneChannels.WHITE_HUE) continue;

			if (axisDistance(cell % SPAN) == 0 && axisDistance(cell / SPAN_SQ) == 0 && axisDistance((cell / SPAN) % SPAN) == 0) {
				return true;
			}
		}
		return false;
	}

	private void appendSkyCell(int cell) {
		if (this.skyCount == this.skyOrder.length) this.skyOrder = Arrays.copyOf(this.skyOrder, this.skyCount * 2);
		this.skyOrder[this.skyCount++] = cell;
	}

	private void resetSky() {
		final short[] hues = this.skyHues;
		if (hues != null) {
			for (int i = 0; i < this.skyCount; i++) {
				final int cell = this.skyOrder[i];
				hues[cell] = 0;
				this.skyMarked[cell] = 0;
			}
			if (this.skyRestored) {
				for (int y = 0; y < WINDOW_SPAN; y++) {
					for (int z = 0; z < WINDOW_SPAN; z++) {
						final int row = cellIndex(WINDOW_MIN, WINDOW_MIN + y, WINDOW_MIN + z);
						Arrays.fill(hues, row, row + WINDOW_SPAN, (short) 0);
					}
				}
			}
		}
		this.skyCount = 0;
		this.skyRestored = false;
		this.skyEvaluated = false;
		this.skyTinted = false;
		Arrays.fill(this.skyLayers, null);
		Arrays.fill(this.tintChunks, null);
	}

	private static boolean withinReach(int rx, int rz, int level) {
		return axisDistance(rx) + axisDistance(rz) <= level;
	}

	private int incidentHue(int cell) {
		final int stored = this.skyHues[cell] & 0xFFFF;
		return stored == 0 ? GlowtoneChannels.WHITE_HUE : stored & GlowtoneChannels.WHITE_HUE;
	}

	private int @Nullable [] tintColumn(int rx, int rz) {
		final SkyTintColumns.Chunk chunk = this.tintChunks[(rx >> 4) + (rz >> 4) * SECTION_GRID];
		return chunk == null ? null : chunk.column(rx & 15, rz & 15);
	}

	private int filterAt(int rx, int ry, int rz) {
		return SkyTintColumns.filterAt(this.tintColumn(rx, rz), this.minBlockY + ry);
	}

	private int skyLevelAt(int rx, int ry, int rz) {
		final int slot = index27(rx >> 4, ry >> 4, rz >> 4);
		final DataLayer layer = this.skyLayers[slot];
		if (layer != null) return layer.get(rx & 15, ry & 15, rz & 15);

		final RenderSectionRegion region = this.region;
		if (region == null) return 0;

		final int column = (slot << 8) | ((rz & 15) << 4) | (rx & 15);
		final byte cached = this.skyColumnLevels[column];
		if (cached != SKY_UNKNOWN) return cached;

		this.scratchPos.set(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz);
		final int level = region.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(this.scratchPos);
		this.skyColumnLevels[column] = (byte) level;
		return level;
	}

	public void release() {
		this.region = null;
		this.containersBound = false;
		Arrays.fill(this.containers, null);
		this.sections = null;
		this.debugRegion = false;
		this.lit = false;
		this.resetSky();
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
		if (!this.skyTinted || !this.containersBound) return null;

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
								final int cell = cellIndex(rx, ry, rz);
								if (this.stateAt(cell, rx, ry, rz).getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;
								if (this.filterAt(rx, ry, rz) != SkyTintColumns.NO_FILTER) continue;

								final int hue = this.incidentHue(cell);
								tinted |= this.skyHues[cell] != 0;

								red += (hue >> 8) & 0xF;
								green += (hue >> 4) & 0xF;
								blue += hue & 0xF;
								counted++;
							}
						}
					}

					if (!tinted || counted == 0) continue;

					final int hue = GlowtoneChannels.meanHue(red, green, blue, counted);
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

				if (++expansions > MAX_EXPANSIONS) {
					if (expansionTrips++ == 0) {
						LOGGER.debug("Glowtone colored light flood hit its expansion cap of {}, truncating propagation", MAX_EXPANSIONS);
					}
					return;
				}

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
