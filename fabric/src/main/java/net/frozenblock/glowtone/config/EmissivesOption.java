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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class EmissivesOption {
	private static final String CAPTION = "options.glowtone.emissives";
	private static final Codec<EmissivesMode> CODEC = Codec.STRING.xmap(EmissivesMode::byId, EmissivesMode::id);
	private static @Nullable OptionInstance<EmissivesMode> instance;

	private EmissivesOption() {}

	public static synchronized OptionInstance<EmissivesMode> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				(caption, value) -> Component.translatable(value.translationKey()),
				new OptionInstance.Enum<>(List.of(EmissivesMode.values()), CODEC),
				GlowtoneConfig.emissives(),
				EmissivesOption::apply
			);
		}
		return instance;
	}

	public static void applyFlags(EmissivesMode mode) {
		GlowtoneConstants.GLOWTONE_EMISSIVES = true;
		GlowtoneConstants.GLOWTONE_SHADING = mode.shadeless();
	}

	private static void apply(EmissivesMode mode) {
		if (GlowtoneConfig.emissives() == mode) return;

		GlowtoneConfig.setEmissives(mode);
		applyFlags(mode);

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getResourceManager() != null) minecraft.reloadResourcePacks();
	}
}
