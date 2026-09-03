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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneColorWindowCache {
	private static final int MAX_ENTRIES = 512;

	private static final Map<Long, short[]> WINDOWS = Collections.synchronizedMap(
		new LinkedHashMap<>(MAX_ENTRIES * 2, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, short[]> eldest) {
				return this.size() > MAX_ENTRIES;
			}
		}
	);

	public static short @Nullable [] get(long section) {
		return WINDOWS.get(section);
	}

	public static void put(long section, short @Nullable [] window) {
		if (window == null) {
			WINDOWS.remove(section);
		} else {
			WINDOWS.put(section, window);
		}
	}

	public static void invalidate(long section) {
		WINDOWS.remove(section);
	}

	public static void invalidateAround(int sectionX, int sectionY, int sectionZ) {
		if (WINDOWS.isEmpty()) return;
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					WINDOWS.remove(SectionPos.asLong(sectionX + x, sectionY + y, sectionZ + z));
				}
			}
		}
	}

	public static void clear() {
		WINDOWS.clear();
	}

	private GlowtoneColorWindowCache() {}
}
