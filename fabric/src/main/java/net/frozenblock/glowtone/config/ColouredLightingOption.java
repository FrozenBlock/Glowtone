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

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.render.light.color.ChromaBlender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ColouredLightingOption {
	private static final String CAPTION = "options.glowtone.coloured_lighting";
	private static final Codec<ColouredLightingMode> CODEC = Codec.STRING.xmap(
		name -> ColouredLightingMode.valueOf(name.toUpperCase(Locale.ROOT)),
		mode -> mode.name().toLowerCase(Locale.ROOT)
	);
	private static @Nullable OptionInstance<ColouredLightingMode> instance;

	private ColouredLightingOption() {}

	public static synchronized OptionInstance<ColouredLightingMode> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				(caption, value) -> Component.translatable(value.translationKey()),
				new OptionInstance.Enum<>(List.of(ColouredLightingMode.values()), CODEC),
				GlowtoneConfig.colouredLighting(),
				ColouredLightingOption::apply
			);
		}
		return instance;
	}

	public static void applyMode(ColouredLightingMode mode) {
		ChromaBlender.setMode(mode);
	}

	private static void apply(ColouredLightingMode mode) {
		if (GlowtoneConfig.colouredLighting() == mode) return;

		GlowtoneConfig.setColouredLighting(mode);
		applyMode(mode);

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) minecraft.levelExtractor.allChanged();
	}
}
