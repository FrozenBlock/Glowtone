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

package net.frozenblock.glowtone.config.pack;

import java.util.EnumSet;
import java.util.Set;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public enum GlowtonePackChoice {
	SLOT_0, SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5, SLOT_6, SLOT_7,
	SLOT_8, SLOT_9, SLOT_10, SLOT_11, SLOT_12, SLOT_13, SLOT_14, SLOT_15;

	public static final int CAPACITY = values().length;

	public static GlowtonePackChoice of(int index) {
		return values()[Math.max(0, Math.min(index, CAPACITY - 1))];
	}

	public static Set<GlowtonePackChoice> first(int count) {
		final EnumSet<GlowtonePackChoice> set = EnumSet.noneOf(GlowtonePackChoice.class);
		for (int index = 0; index < Math.min(count, CAPACITY); index++) set.add(values()[index]);
		return set;
	}
}
