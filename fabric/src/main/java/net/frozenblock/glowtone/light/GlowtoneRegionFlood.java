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

import java.util.Arrays;
import java.util.function.Predicate;
import net.frozenblock.glowtone.light.color.GlowtoneEmitterColors;
import net.frozenblock.glowtone.light.color.GlowtoneTransmittance;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

public final class GlowtoneRegionFlood {
	public static final int SPAN = 48;
	public static final int WHITE_RGB = 0xFFFFFF;
	private static final java.util.function.Predicate<BlockState> TINTS_DAYLIGHT = state ->
		GlowtoneTransmittance.filterFor(state) != GlowtoneTransmittance.FULLY_TRANSMISSIVE
			&& state.getFluidState().isEmpty();

	public static final int ENTITY_SPAN = 8;
	public static final int ENTITY_CELL_BLOCKS = 2;
	public static final int ENTITY_CELLS = ENTITY_SPAN * ENTITY_SPAN * ENTITY_SPAN;

	private static final int WINDOW_MIN = 14;
	private static final int WINDOW_MAX = 33;

	private static final int MAX_REACH = GlowtoneChannels.MAX_LEVEL - 1;

	private static final int SPAN_SQ = SPAN * SPAN;
	private static final int CELLS = SPAN * SPAN * SPAN;

	private static final int MAX_EXPANSIONS = CELLS;
	private static final int SKY_SPREAD = 12;
	private static final boolean SKY_TINT_ENABLED = false;
	private static final byte SKY_UNKNOWN = -1;
	private static final short COLUMN_UNKNOWN = Short.MIN_VALUE;
	private static final short COLUMN_OPEN = -1;
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
			throw new AssertionError("Glowtone assumes a 3x3x3 RenderSectionRegion, found SIZE="
					+ RenderSectionRegion.SIZE);
		}
		if (WINDOW_MIN - MAX_REACH != 0 || WINDOW_MAX + MAX_REACH != SPAN - 1) {
			throw new AssertionError("Coloured-light reach does not match the region: window ["
					+ WINDOW_MIN + ", " + WINDOW_MAX + "] expanded by " + MAX_REACH + " must be [0, " + (SPAN - 1) + "]");
		}
		if (SPAN != RenderSectionRegion.SIZE * 16 || SPAN > ENTRY_COORD_MASK + 1) {
			throw new AssertionError("Region span " + SPAN + " does not fit the flood's cell encoding.");
		}
	}

	private short[] levels;
	private short[] skyHues;
	private byte[] skyLevels;
	private short[] columnCover;
	private byte[] skyReach;
	private final it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue skyQueue = new it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue();
	private @Nullable BlockState[] states;

	private final int[][] buckets = new int[GlowtoneChannels.MAX_LEVEL + 1][];
	private final int[] bucketSizes = new int[GlowtoneChannels.MAX_LEVEL + 1];

	private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

	private @Nullable RenderSectionRegion region;
	private SectionCopy @Nullable [] sections;
	private boolean skyTinted;
	private boolean debugRegion;
	private int minBlockX;
	private int minBlockY;
	private int minBlockZ;
	private boolean lit;

	public static int entityCellIndex(int localX, int localY, int localZ) {
		return (((localY & 15) >> 1) * ENTITY_SPAN + ((localZ & 15) >> 1)) * ENTITY_SPAN + ((localX & 15) >> 1);
	}

	public boolean begin(RenderSectionRegion region, int centreSectionX, int centreSectionY, int centreSectionZ) {
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

		final int emitterMask = emitterMask(sections);
		final int tintMask = SKY_TINT_ENABLED ? tintMask(sections) : 0;
		if (emitterMask == 0 && tintMask == 0) return false;

		if (this.levels == null) {
			this.levels = new short[CELLS];
			this.states = new BlockState[CELLS];
		}

		this.region = region;
		this.sections = sections;
		this.debugRegion = sections[index27(1, 1, 1)].debug;
		this.minBlockX = minSectionX << 4;
		this.minBlockY = minSectionY << 4;
		this.minBlockZ = minSectionZ << 4;
		this.lit = false;
		this.skyTinted = SKY_TINT_ENABLED && tintMask != 0;

		if (this.skyTinted) this.floodSkyTint(sections, tintMask);

		if (emitterMask != 0) {
			Arrays.fill(this.levels, (short) 0);
			Arrays.fill(this.states, null);
			Arrays.fill(this.bucketSizes, 0);

			this.seed(sections, emitterMask);
			this.propagate();
		}
		return this.lit || this.skyTinted;
	}

	public boolean isLit() {
		return this.lit;
	}

	public boolean hasSkyTint() {
		return this.skyTinted;
	}

	public int skyHueAt(int worldX, int worldY, int worldZ) {
		if (!this.skyTinted || this.sections == null) return WHITE_RGB;

		final int rx = worldX - this.minBlockX;
		final int ry = worldY - this.minBlockY;
		final int rz = worldZ - this.minBlockZ;
		if (isOutside(rx, ry, rz)) return WHITE_RGB;

		final int hue = this.fadedHue(cellIndex(rx, ry, rz));
		if (hue == 0 || hue == GlowtoneChannels.WHITE_HUE) return WHITE_RGB;

		return GlowtoneChannels.toNormalisedRgb(GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, hue));
	}

	private void floodSkyTint(SectionCopy[] sections, int tintMask) {
		if (this.skyHues == null) {
			this.skyHues = new short[CELLS];
			this.skyLevels = new byte[CELLS];
			this.skyReach = new byte[CELLS];
			this.columnCover = new short[SPAN * SPAN];
		}

		Arrays.fill(this.skyHues, (short) 0);
		Arrays.fill(this.skyReach, (byte) 0);
		Arrays.fill(this.skyLevels, SKY_UNKNOWN);
		Arrays.fill(this.columnCover, COLUMN_UNKNOWN);
		this.skyQueue.clear();

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

						final BlockState state = this.stateAt(this.minBlockX + rx, this.minBlockY + ry, this.minBlockZ + rz);
						final int filter = GlowtoneTransmittance.filterFor(state);
						if (filter == GlowtoneTransmittance.FULLY_TRANSMISSIVE) continue;
						if (!state.getFluidState().isEmpty()) continue;
						if (this.skyLevelAt(rx, ry, rz) <= 0) continue;

						final int cell = cellIndex(rx, ry, rz);
						final int existing = this.skyHues[cell] & GlowtoneChannels.WHITE_HUE;
						final int incoming = existing == 0 ? GlowtoneChannels.WHITE_HUE : existing;
						this.writeSky(cell, GlowtoneChannels.filterHue(incoming, filter), SKY_SPREAD);
					}
				}
			}
		}

		this.drainSkyQueue();
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
		}

		this.skyLevels[cell] = (byte) level;
		return level;
	}

	private int fadedHue(int cell) {
		final int hue = this.skyHues[cell] & GlowtoneChannels.WHITE_HUE;
		if (hue == 0) return 0;

		final int reach = this.skyReach[cell];
		if (reach >= SKY_SPREAD) return hue;

		return GlowtoneChannels.hue(GlowtoneChannels.blendHues(
			GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, hue),
			GlowtoneChannels.pack(GlowtoneChannels.MAX_LEVEL, GlowtoneChannels.WHITE_HUE),
			reach,
			SKY_SPREAD - reach
		));
	}

	private void drainSkyQueue() {
		while (!this.skyQueue.isEmpty()) {
			final int packed = this.skyQueue.dequeueInt();
			final int cell = packed & 0x3FFFFF;
			final int budget = packed >>> 22;
			if (budget <= 0) continue;

			final int rx = cell % SPAN;
			final int rz = (cell / SPAN) % SPAN;
			final int ry = cell / SPAN_SQ;
			final int hue = this.skyHues[cell] & GlowtoneChannels.WHITE_HUE;
			final int sourceSky = this.skyLevelAt(rx, ry, rz);

			for (Direction direction : DIRECTIONS) {
				final int nx = rx + direction.getStepX();
				final int ny = ry + direction.getStepY();
				final int nz = rz + direction.getStepZ();
				if (isOutside(nx, ny, nz)) continue;

				final BlockState state = this.stateAt(this.minBlockX + nx, this.minBlockY + ny, this.minBlockZ + nz);
				if (state.getLightDampening() >= GlowtoneChannels.MAX_LEVEL) continue;

				final int neighbourSky = this.skyLevelAt(nx, ny, nz);
				if (neighbourSky <= 0 || neighbourSky > sourceSky) continue;
				if (direction != Direction.DOWN && !this.isUnderCover(nx, ny, nz)) continue;

				final int filter = GlowtoneTransmittance.filterFor(state);
				final int next = filter == GlowtoneTransmittance.FULLY_TRANSMISSIVE ? hue : GlowtoneChannels.filterHue(hue, filter);
				this.writeSky(cellIndex(nx, ny, nz), next, budget - 1);
			}
		}
	}

	private boolean isUnderCover(int rx, int ry, int rz) {
		final int column = rz * SPAN + rx;
		int top = this.columnCover[column];

		if (top == COLUMN_UNKNOWN) {
			top = COLUMN_OPEN;
			for (int y = SPAN - 1; y >= 0; y--) {
				final BlockState state = this.stateAt(this.minBlockX + rx, this.minBlockY + y, this.minBlockZ + rz);
				if (state.getLightDampening() >= GlowtoneChannels.MAX_LEVEL
					|| GlowtoneTransmittance.filterFor(state) != GlowtoneTransmittance.FULLY_TRANSMISSIVE
				) {
					top = y;
					break;
				}
			}
			this.columnCover[column] = (short) top;
		}

		return ry < top;
	}

	private void writeSky(int cell, int hue, int budget) {
		if (budget < 0) return;
		if ((this.skyHues[cell] & GlowtoneChannels.WHITE_HUE) != 0) return;

		this.skyHues[cell] = (short) (hue & GlowtoneChannels.WHITE_HUE);
		this.skyReach[cell] = (byte) budget;
		if (budget > 0) this.skyQueue.enqueue(cell | (budget << 22));
	}

	private static int tintMask(SectionCopy[] sections) {
		int mask = 0;

		for (int i = 0; i < sections.length; i++) {
			final SectionCopy sectionCopy = sections[i];
			if (sectionCopy.debug) continue;

			final PalettedContainer<BlockState> container = sectionCopy.section;
			if (container == null || !container.maybeHas(TINTS_DAYLIGHT)) continue;

			mask |= 1 << i;
		}

		return mask;
	}

	public void release() {
		this.region = null;
		this.sections = null;
		this.debugRegion = false;
		this.lit = false;
		this.skyTinted = false;
	}

	public int cellLevelsAt(int worldX, int worldY, int worldZ) {
		if (!this.lit || this.sections == null) return 0;

		final int baseX = worldX & ~(ENTITY_CELL_BLOCKS - 1);
		final int baseY = worldY & ~(ENTITY_CELL_BLOCKS - 1);
		final int baseZ = worldZ & ~(ENTITY_CELL_BLOCKS - 1);

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
		return best;
	}

	public int levelsAt(int worldX, int worldY, int worldZ) {
		if (!this.lit) return 0;

		if (this.sections == null) return 0;

		int rx = worldX - this.minBlockX;
		int ry = worldY - this.minBlockY;
		int rz = worldZ - this.minBlockZ;
		if (isOutside(rx, ry, rz)) return 0;

		return this.levels[cellIndex(rx, ry, rz)] & GlowtoneChannels.LEVEL_MASK;
	}

	public BlockState stateAt(int worldX, int worldY, int worldZ) {
		if (this.sections == null) return AIR;

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

					for (int dy = 0; dy < 2; dy++) {
						for (int dz = 0; dz < 2; dz++) {
							for (int dx = 0; dx < 2; dx++) {
								int hue = this.fadedHue(cellIndex(
									16 + (cellX << 1) + dx,
									16 + (cellY << 1) + dy,
									16 + (cellZ << 1) + dz
								));

								if (hue == 0) {
									hue = GlowtoneChannels.WHITE_HUE;
								} else {
									tinted = true;
								}

								red += (hue >> 8) & 0xF;
								green += (hue >> 4) & 0xF;
								blue += hue & 0xF;
							}
						}
					}

					if (!tinted) continue;

					final int hue = ((red >> 3) << 8) | ((green >> 3) << 4) | (blue >> 3);
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

	private static int emitterMask(SectionCopy[] sections) {
		int mask = 0;

		for (int i = 0; i < sections.length; i++) {
			final SectionCopy sectionCopy = sections[i];
			if (sectionCopy.debug) continue;

			final PalettedContainer<BlockState> container = sectionCopy.section;
			if (container == null || !container.maybeHas(EMITS_LIGHT)) continue;

			mask |= 1 << i;
		}

		return mask;
	}

	private void seed(SectionCopy[] sections, int emitterMask) {
		for (int sectionZ = 0; sectionZ < SECTION_GRID; sectionZ++) {
			for (int sectionY = 0; sectionY < SECTION_GRID; sectionY++) {
				for (int sectionX = 0; sectionX < SECTION_GRID; sectionX++) {
					int slot = index27(sectionX, sectionY, sectionZ);
					if ((emitterMask & (1 << slot)) == 0) continue;

					final PalettedContainer<BlockState> container = sections[slot].section;
					if (container == null) continue;

					this.seedSection(container, sectionX << 4, sectionY << 4, sectionZ << 4);
				}
			}
		}
	}

	private void seedSection(PalettedContainer<BlockState> container, int baseX, int baseY, int baseZ) {
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

					final int emission = state.getLightEmission();
					if (emission <= 0) continue;

					final int packed = GlowtoneChannels.emissionLevels(emission, GlowtoneEmitterColors.rgbFor(state));
					if (packed == 0) continue;
					if (!reaches(GlowtoneChannels.level(packed), rx, ry, rz)) continue;

					final int merged = GlowtoneChannels.merge(this.levels[cell] & GlowtoneChannels.LEVEL_MASK, packed);
					this.levels[cell] = (short) (merged & GlowtoneChannels.LEVEL_MASK);
					this.lit = true;
					this.enqueue(rx, ry, rz, merged, isEmptyShape(state), 0);
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
			if (!reaches(reducedLevel, neighbourX, neighbourY, neighbourZ)) continue;

			final int neighbourCell = cellIndex(neighbourX, neighbourY, neighbourZ);
			final int stored = this.levels[neighbourCell] & GlowtoneChannels.LEVEL_MASK;
			final boolean brighter = GlowtoneChannels.anyGreater(reduced, stored);
			if (!brighter && !canBlend(reduced, stored, packed)) continue;

			final BlockState toState = this.stateAt(neighbourCell, neighbourX, neighbourY, neighbourZ);
			final int next = GlowtoneChannels.attenuate(
				packed,
				Math.max(1, toState.getLightDampening()),
				GlowtoneTransmittance.filterFor(toState)
			);
			final boolean nextBrighter = GlowtoneChannels.anyGreater(next, stored);
			if (!nextBrighter && !canBlend(next, stored, next)) continue;
			if (!reaches(GlowtoneChannels.level(next), neighbourX, neighbourY, neighbourZ)) continue;

			if (fromState == null) fromState = fromEmptyShape ? AIR : this.stateAt(cell, x, y, z);

			if (shapeOccludes(fromState, toState, direction)) continue;

			if (!nextBrighter) {
				final int blended = GlowtoneChannels.blendHues(
					stored, next, GlowtoneChannels.level(stored), GlowtoneChannels.level(next)
				);
				if (blended != stored) this.levels[neighbourCell] = (short) (blended & GlowtoneChannels.LEVEL_MASK);
				continue;
			}

			final int merged = GlowtoneChannels.merge(next, stored);
			this.levels[neighbourCell] = (short) (merged & GlowtoneChannels.LEVEL_MASK);
			this.lit = true;

			if (GlowtoneChannels.max(merged) > 1) {
				this.enqueue(
					neighbourX, neighbourY, neighbourZ,
					merged,
					isEmptyShape(toState),
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

	private static boolean isEmptyShape(BlockState state) {
		return !state.canOcclude() || !state.useShapeForLightOcclusion();
	}

	private BlockState stateAt(int cell, int x, int y, int z) {
		final BlockState cached = this.states[cell];
		if (cached != null) return cached;

		final BlockState state = this.readState(x, y, z);
		this.states[cell] = state;
		return state;
	}

	private BlockState readState(int x, int y, int z) {
		if (this.debugRegion) {
			final RenderSectionRegion region = this.region;
			return region == null
				? AIR
				: region.getBlockState(this.scratchPos.set(this.minBlockX + x, this.minBlockY + y, this.minBlockZ + z));
		}

		final SectionCopy[] sections = this.sections;
		if (sections == null) return AIR;

		final PalettedContainer<BlockState> container = sections[index27(x >> 4, y >> 4, z >> 4)].section;
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
