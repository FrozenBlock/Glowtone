/*
 * Copyright 2025-2026 FrozenBlock
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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneColorWindowCache {
	private static final int MAX_ENTRIES = 512;

	private static final Object LOCK = new Object();
	private static final Long2ObjectLinkedOpenHashMap<short[]> WINDOWS = new Long2ObjectLinkedOpenHashMap<>(MAX_ENTRIES * 2);
	private static final Long2ObjectLinkedOpenHashMap<short[]> SKY_WINDOWS = new Long2ObjectLinkedOpenHashMap<>(MAX_ENTRIES * 2);

	public static short @Nullable [] get(long section) {
		synchronized (LOCK) {
			return WINDOWS.getAndMoveToLast(section);
		}
	}

	public static short @Nullable [] getSky(long section) {
		synchronized (LOCK) {
			return SKY_WINDOWS.getAndMoveToLast(section);
		}
	}

	public static void putSky(long section, short @Nullable [] window) {
		synchronized (LOCK) {
			store(SKY_WINDOWS, section, window);
		}
	}

	public static void put(long section, short @Nullable [] window) {
		synchronized (LOCK) {
			store(WINDOWS, section, window);
		}
	}

	private static void store(Long2ObjectLinkedOpenHashMap<short[]> windows, long section, short @Nullable [] window) {
		if (window == null) {
			windows.remove(section);
			return;
		}

		windows.putAndMoveToLast(section, window);
		while (windows.size() > MAX_ENTRIES) windows.removeFirst();
	}

	public static void invalidate(long section) {
		synchronized (LOCK) {
			WINDOWS.remove(section);
			SKY_WINDOWS.remove(section);
		}
	}

	public static void invalidateAround(int sectionX, int sectionY, int sectionZ) {
		synchronized (LOCK) {
			if (WINDOWS.isEmpty() && SKY_WINDOWS.isEmpty()) return;
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					for (int z = -1; z <= 1; z++) {
						final long node = SectionPos.asLong(sectionX + x, sectionY + y, sectionZ + z);
						WINDOWS.remove(node);
						SKY_WINDOWS.remove(node);
					}
				}
			}
		}
	}

	public static void invalidateSkyColumns(int sectionX, int minSectionY, int maxSectionY, int sectionZ) {
		synchronized (LOCK) {
			if (SKY_WINDOWS.isEmpty()) return;
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					for (int y = minSectionY; y <= maxSectionY; y++) {
						SKY_WINDOWS.remove(SectionPos.asLong(sectionX + x, y, sectionZ + z));
					}
				}
			}
		}
	}

	public static void clear() {
		synchronized (LOCK) {
			WINDOWS.clear();
			SKY_WINDOWS.clear();
		}
	}

	private GlowtoneColorWindowCache() {}
}
