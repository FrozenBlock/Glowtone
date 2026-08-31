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

package net.frozenblock.glowtone.config.option.animation;

import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class SmoothAnimationOption {
	public static final boolean DEFAULT = true;
	private static final String CAPTION = "options.glowtone.smooth_animation";
	private static @Nullable OptionInstance<Boolean> instance;

	public static synchronized OptionInstance<Boolean> get() {
		if (instance == null) {
			instance = OptionInstance.createBoolean(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				DEFAULT,
				GlowtoneConfig::setSmoothAnimation
			);
		}
		return instance;
	}

	private SmoothAnimationOption() {}
}
