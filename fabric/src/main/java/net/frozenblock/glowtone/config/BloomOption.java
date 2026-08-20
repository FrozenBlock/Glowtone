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

package net.frozenblock.glowtone.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BloomOption {
	public static final int MIN = 0;
	public static final int MAX = 100;
	public static final int PRESET_DEFAULT = 20;
	private static final String CAPTION = "options.glowtone.bloom";
	private static @Nullable OptionInstance<Integer> instance;

	private BloomOption() {}

	public static synchronized OptionInstance<Integer> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.noTooltip(),
				(caption, value) -> value == MAX
					? Options.genericValueLabel(caption, Component.translatable("options.glowtone.bloom.max"))
					: Options.genericValueOrOffLabel(caption, value),
				new OptionInstance.IntRange(MIN, MAX),
				GlowtoneConfig.bloom(),
				BloomOption::apply
			);
		}
		return instance;
	}

	public static float strength() {
		return get().get() / (float) MAX;
	}

	public static void set(int value) {
		get().set(value);
	}

	private static void apply(int value) {
		GlowtoneConfig.setBloom(value);
	}
}
