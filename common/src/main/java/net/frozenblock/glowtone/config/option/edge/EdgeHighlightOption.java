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

package net.frozenblock.glowtone.config.option.edge;

import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class EdgeHighlightOption {
	public static final int MIN = 0;
	public static final int MAX = 100;
	public static final int DEFAULT = 50;
	private static final int STRENGTH_REFERENCE = 25;
	private static final float STRENGTH_AT_REFERENCE = 0.08F;
	private static final String CAPTION = "options.glowtone.edge_highlight";
	private static @Nullable OptionInstance<Integer> instance;

	public static synchronized OptionInstance<Integer> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				Options::genericValueOrOffLabel,
				new OptionInstance.IntRange(MIN, MAX),
				GlowtoneConfig.edgeHighlight(),
				EdgeHighlightOption::apply
			);
		}
		return instance;
	}

	public static boolean enabled() {
		return GlowtoneConfig.edgeHighlight() > MIN;
	}

	public static float strength() {
		return GlowtoneConfig.edgeHighlight() / (float) STRENGTH_REFERENCE * STRENGTH_AT_REFERENCE;
	}

	private static boolean pendingReload;

	private static void apply(int value) {
		if (GlowtoneConfig.edgeHighlight() == value) return;

		GlowtoneConfig.setEdgeHighlight(value);
		pendingReload = true;
	}

	public static void flush() {
		if (!pendingReload) return;
		pendingReload = false;

		GlowtoneReload.request();
	}

	private EdgeHighlightOption() {}
}
