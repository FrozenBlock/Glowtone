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

package net.frozenblock.glowtone.config.option.shade;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ShadingOption {
	private static final String CAPTION = "options.glowtone.shading";
	private static @Nullable OptionInstance<ShadingMode> instance;

	public static synchronized OptionInstance<ShadingMode> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				(caption, value) -> Component.translatable(value.translationKey()),
				new OptionInstance.Enum<>(List.of(ShadingMode.values()), ShadingMode.CODEC),
				GlowtoneConfig.shading(),
				ShadingOption::apply
			);
		}
		return instance;
	}

	public static void applyFlags(ShadingMode mode) {
		GlowtoneConstants.GLOWTONE_EMISSIVES = true;
		GlowtoneConstants.GLOWTONE_SHADING = mode.unshadeEmissive();
		GlowtoneConstants.GLOWTONE_NO_SHADING = mode.unshadeAll();
	}

	private static void apply(ShadingMode mode) {
		if (GlowtoneConfig.shading() == mode) return;

		GlowtoneConfig.setShading(mode);
		applyFlags(mode);
		GlowtoneReload.request();
	}

	private ShadingOption() {}
}
