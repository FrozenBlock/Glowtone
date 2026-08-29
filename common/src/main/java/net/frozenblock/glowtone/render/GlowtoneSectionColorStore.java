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

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ClientOnly
public final class GlowtoneSectionColorStore {
	private static final Map<Long, short[]> COLORS = new ConcurrentHashMap<>();
	private static final Map<Long, short[]> SKY_HUES = new ConcurrentHashMap<>();

	private GlowtoneSectionColorStore() {}

	public static void publish(long section, short @Nullable [] colors, short @Nullable [] skyHues) {
		store(COLORS, section, colors);
		store(SKY_HUES, section, skyHues);
	}

	private static void store(Map<Long, short[]> into, long section, short @Nullable [] payload) {
		if (payload == null) {
			into.remove(section);
		} else {
			into.put(section, payload);
		}
	}

	public static short @Nullable [] colors(long section) {
		return COLORS.get(section);
	}

	public static short @Nullable [] skyHues(long section) {
		return SKY_HUES.get(section);
	}

	public static boolean isEmpty() {
		return COLORS.isEmpty() && SKY_HUES.isEmpty();
	}

	public static void remove(int sectionX, int sectionY, int sectionZ) {
		final long section = SectionPos.asLong(sectionX, sectionY, sectionZ);
		COLORS.remove(section);
		SKY_HUES.remove(section);
	}

	public static void clear() {
		COLORS.clear();
		SKY_HUES.clear();
	}
}
