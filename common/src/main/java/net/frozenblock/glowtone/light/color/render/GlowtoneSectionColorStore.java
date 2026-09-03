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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneSectionColorStore {
	public record Colors(short @Nullable [] levels, short @Nullable [] skyHues) {}

	private static final Map<Long, Colors> SECTIONS = new ConcurrentHashMap<>();

	public static void publish(long section, short @Nullable [] levels, short @Nullable [] skyHues) {
		if (levels == null && skyHues == null) {
			SECTIONS.remove(section);
			return;
		}
		SECTIONS.put(section, new Colors(levels, skyHues));
	}

	public static @Nullable Colors colors(long section) {
		return SECTIONS.get(section);
	}

	public static void remove(int sectionX, int sectionY, int sectionZ) {
		SECTIONS.remove(SectionPos.asLong(sectionX, sectionY, sectionZ));
	}

	public static void clear() {
		SECTIONS.clear();
	}

	private GlowtoneSectionColorStore() {}
}
