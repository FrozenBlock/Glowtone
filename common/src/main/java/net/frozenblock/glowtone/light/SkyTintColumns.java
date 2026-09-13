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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;
import net.frozenblock.glowtone.light.color.FilterColorHelper;
import net.frozenblock.glowtone.light.color.render.GlowtoneColorWindowCache;
import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class SkyTintColumns {
	public static final int NO_FILTER = FilterColorHelper.FULLY_TRANSMISSIVE;
	private static final int FILTER_BITS = 12;
	private static final int FILTER_MASK = (1 << FILTER_BITS) - 1;
	private static final int COLUMNS = 16 * 16;
	private static final int SPREAD_REACH = GlowtoneChannels.MAX_LEVEL - 1;
	private static final int WINDOW_MARGIN = 2;
	private static final Predicate<BlockState> TINTS_DAYLIGHT = state -> filterOf(state) != NO_FILTER;

	private static final Map<Long, Chunk> CHUNKS = new ConcurrentHashMap<>();
	private static final LongOpenHashSet LOADED = new LongOpenHashSet();

	public static final class Chunk {
		private final AtomicReferenceArray<int @Nullable []> columns = new AtomicReferenceArray<>(COLUMNS);
		private int populated;

		public int @Nullable [] column(int localX, int localZ) {
			return this.columns.get(localX | (localZ << 4));
		}

		public int @Nullable [] column(int index) {
			return this.columns.get(index);
		}

		private void set(int localX, int y, int localZ, int filter) {
			final int index = localX | (localZ << 4);
			final int[] column = this.columns.get(index);
			if (column == null) {
				this.columns.set(index, new int[]{entry(y, filter)});
				this.populated++;
				return;
			}

			int at = 0;
			while (at < column.length && entryY(column[at]) > y) at++;
			if (at < column.length && entryY(column[at]) == y) {
				final int[] replaced = column.clone();
				replaced[at] = entry(y, filter);
				this.columns.set(index, replaced);
				return;
			}

			final int[] grown = new int[column.length + 1];
			System.arraycopy(column, 0, grown, 0, at);
			grown[at] = entry(y, filter);
			System.arraycopy(column, at, grown, at + 1, column.length - at);
			this.columns.set(index, grown);
		}

		private void remove(int localX, int y, int localZ) {
			final int index = localX | (localZ << 4);
			final int[] column = this.columns.get(index);
			if (column == null) return;

			int at = 0;
			while (at < column.length && entryY(column[at]) > y) at++;
			if (at == column.length || entryY(column[at]) != y) return;

			if (column.length == 1) {
				this.columns.set(index, null);
				this.populated--;
				return;
			}

			final int[] shrunk = new int[column.length - 1];
			System.arraycopy(column, 0, shrunk, 0, at);
			System.arraycopy(column, at + 1, shrunk, at, column.length - at - 1);
			this.columns.set(index, shrunk);
		}
	}

	public static @Nullable Chunk chunk(int chunkX, int chunkZ) {
		return CHUNKS.get(ChunkPos.pack(chunkX, chunkZ));
	}

	public static boolean any() {
		return !CHUNKS.isEmpty();
	}

	static int entry(int y, int filter) {
		return (y << FILTER_BITS) | (filter & FILTER_MASK);
	}

	static int entryY(int entry) {
		return entry >> FILTER_BITS;
	}

	static int entryFilter(int entry) {
		return entry & FILTER_MASK;
	}

	static int tintAbove(int @Nullable [] column, int y) {
		return mixLayers(column, y + 1);
	}

	static int tintThrough(int @Nullable [] column, int y) {
		return mixLayers(column, y);
	}

	private static int mixLayers(int @Nullable [] column, int lowestY) {
		if (column == null) return GlowtoneChannels.WHITE_HUE;

		int red = 0;
		int green = 0;
		int blue = 0;
		int count = 0;
		for (final int entry : column) {
			if (entryY(entry) < lowestY) break;
			final int filter = entryFilter(entry);
			red += (filter >> 8) & 0xF;
			green += (filter >> 4) & 0xF;
			blue += filter & 0xF;
			count++;
		}
		return GlowtoneChannels.meanHue(red, green, blue, count);
	}

	static int filterAt(int @Nullable [] column, int y) {
		if (column == null) return NO_FILTER;

		for (final int entry : column) {
			final int entryY = entryY(entry);
			if (entryY < y) break;
			if (entryY == y) return entryFilter(entry);
		}
		return NO_FILTER;
	}

	public static int filterOf(BlockState state) {
		if (!state.getFluidState().isEmpty()) return NO_FILTER;
		return FilterColorHelper.filterFor(state);
	}

	public static void onBlockChanged(LevelChunk chunk, BlockPos pos, BlockState oldState, BlockState newState) {
		if (!(chunk.getLevel() instanceof ClientLevel level)) return;

		final int oldFilter = filterOf(oldState);
		final int newFilter = filterOf(newState);
		final boolean colorChanged = !BlockLightProperties.hasSameColorProperties(newState, oldState, true, true);
		if (oldFilter == newFilter && !colorChanged) return;

		final int sectionX = SectionPos.blockToSectionCoord(pos.getX());
		final int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		final int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
		int minSectionY = sectionY - 1;
		int maxSectionY = sectionY + 1;

		if (oldFilter != newFilter) {
			update(chunk.getPos(), pos.getX() & 15, pos.getY(), pos.getZ() & 15, newFilter);

			final ChunkSkyLightSources sources = chunk.getSkyLightSources();
			int lowestSource = sources.getLowestSourceY(pos.getX() & 15, pos.getZ() & 15);
			if (lowestSource == ChunkSkyLightSources.NEGATIVE_INFINITY) lowestSource = level.getMinY();
			if (pos.getY() >= lowestSource) {
				minSectionY = Math.min(minSectionY, SectionPos.blockToSectionCoord(lowestSource - SPREAD_REACH - WINDOW_MARGIN));
			}
			GlowtoneColorWindowCache.invalidateSkyColumns(sectionX, minSectionY, maxSectionY, sectionZ);
		}

		level.setSectionRangeDirty(sectionX - 1, minSectionY, sectionZ - 1, sectionX + 1, maxSectionY, sectionZ + 1);
	}

	public static void update(ChunkPos chunkPos, int localX, int y, int localZ, int filter) {
		final long key = chunkPos.pack();
		Chunk chunk = CHUNKS.get(key);
		if (filter == NO_FILTER) {
			if (chunk == null) return;
			chunk.remove(localX, y, localZ);
			if (chunk.populated == 0) CHUNKS.remove(key);
			return;
		}

		if (chunk == null) {
			chunk = new Chunk();
			CHUNKS.put(key, chunk);
		}
		chunk.set(localX, y, localZ, filter);
	}

	public static void onChunkLoaded(LevelChunk chunk) {
		final long key = chunk.getPos().pack();
		LOADED.add(key);
		rebuild(key, chunk);
	}

	public static void onChunkUnloaded(ChunkPos pos) {
		final long key = pos.pack();
		LOADED.remove(key);
		CHUNKS.remove(key);
	}

	public static void rebuild(ClientLevel level) {
		CHUNKS.clear();
		final LongIterator loaded = LOADED.iterator();
		while (loaded.hasNext()) {
			final long key = loaded.nextLong();
			final LevelChunk chunk = level.getChunkSource().getChunk(ChunkPos.getX(key), ChunkPos.getZ(key), ChunkStatus.FULL, false);
			if (chunk != null) rebuild(key, chunk);
		}
	}

	private static void rebuild(long key, LevelChunk chunk) {
		Chunk built = null;
		final LevelChunkSection[] sections = chunk.getSections();
		for (int index = 0; index < sections.length; index++) {
			final LevelChunkSection section = sections[index];
			if (section.hasOnlyAir() || !section.maybeHas(TINTS_DAYLIGHT)) continue;

			final int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						final int filter = filterOf(section.getBlockState(x, y, z));
						if (filter == NO_FILTER) continue;

						if (built == null) built = new Chunk();
						built.set(x, baseY + y, z, filter);
					}
				}
			}
		}

		if (built == null) {
			CHUNKS.remove(key);
		} else {
			CHUNKS.put(key, built);
		}
	}

	public static void clear() {
		CHUNKS.clear();
		LOADED.clear();
	}

	private SkyTintColumns() {}
}
