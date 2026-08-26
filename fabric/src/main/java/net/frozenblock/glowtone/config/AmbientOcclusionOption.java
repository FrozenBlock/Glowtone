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
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class AmbientOcclusionOption {
	public static final String CAPTION = "options.glowtone.ambient_occlusion";

	public static final boolean SHADER_CONTACT_SHADING = true;

	public static final boolean BAKED_CONTACT_SHADING = false;

	private static final Codec<AmbientOcclusionMode> CODEC =
		Codec.STRING.xmap(AmbientOcclusionMode::byId, AmbientOcclusionMode::id);
	private static @Nullable OptionInstance<AmbientOcclusionMode> instance;

	private AmbientOcclusionOption() {}

	public static synchronized OptionInstance<AmbientOcclusionMode> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				(caption, value) -> Component.translatable(value.translationKey()),
				new OptionInstance.Enum<>(List.of(AmbientOcclusionMode.values()), CODEC),
				GlowtoneConfig.ambientOcclusion(),
				AmbientOcclusionOption::apply
			);
		}
		return instance;
	}

	public static boolean smoothLightingEnabled() {
		final Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options.ambientOcclusion().get();
	}

	public static boolean available() {
		return smoothLightingEnabled();
	}

	public static AmbientOcclusionMode effective() {
		if (!available() || !OcclusionStrengthOption.enabled()) return AmbientOcclusionMode.OFF;
		return GlowtoneConfig.ambientOcclusion();
	}

	public static boolean glowtoneActive() {
		return effective().glowtone();
	}

	public static boolean vanillaActive() {
		return effective().vanilla();
	}

	private static void apply(AmbientOcclusionMode mode) {
		if (GlowtoneConfig.ambientOcclusion() == mode) return;

		GlowtoneConfig.setAmbientOcclusion(mode);

		if (!OcclusionStrengthOption.enabled()) return;

		reload();
		OcclusionStrengthOption.rebuildSatisfied();
	}

	static void rebuild() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;

		switch (GlowtoneConfig.ambientOcclusion()) {
			case FAST -> {
				if (minecraft.level != null) minecraft.levelExtractor.allChanged();
			}
			case FANCY -> reload();
			case OFF -> { }
		}
		OcclusionStrengthOption.rebuildSatisfied();
	}

	public static void rebuildFromScreen() {
		if (OcclusionStrengthOption.enabled()) rebuild();
	}

	private static void reload() {
		GlowtoneReload.request();
	}
}
