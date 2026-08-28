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

package net.frozenblock.glowtone.config.option.color;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ColoredLightingOption {
	private static final String CAPTION = "options.glowtone.colored_lighting";
	private static @Nullable OptionInstance<ColoredLightingMode> instance;

	public static synchronized OptionInstance<ColoredLightingMode> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				(caption, value) -> Component.translatable(value.translationKey()),
				new OptionInstance.Enum<>(List.of(ColoredLightingMode.values()), ColoredLightingMode.CODEC),
				GlowtoneConfig.coloredLighting(),
				ColoredLightingOption::apply
			);
		}
		return instance;
	}

	public static void applyMode(ColoredLightingMode mode) {
		ChromaBlender.setMode(mode);
	}

	private static void apply(ColoredLightingMode mode) {
		if (GlowtoneConfig.coloredLighting() == mode) return;

		GlowtoneConfig.setColoredLighting(mode);
		applyMode(mode);

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) minecraft.levelExtractor.allChanged();
	}

	private ColoredLightingOption() {}
}
