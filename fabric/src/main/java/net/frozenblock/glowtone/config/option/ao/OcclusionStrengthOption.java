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

package net.frozenblock.glowtone.config.option.ao;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class OcclusionStrengthOption {
	public static final int MIN = 0;
	public static final int MAX = 100;

	public static final int VANILLA = 25;

	public static final float VANILLA_DEPTH = 0.8F;

	public static final float COVERAGE_REFERENCE = 1F / GlowtoneContactRects.COVERAGE_SCALE;

	public static final String CAPTION = "options.glowtone.occlusion_strength";

	private static @Nullable OptionInstance<Integer> instance;
	private static boolean pendingReload;

	private OcclusionStrengthOption() {}

	public static synchronized OptionInstance<Integer> get() {
		if (instance == null) {
			instance = new OptionInstance<>(
				CAPTION,
				OptionInstance.cachedConstantTooltip(Component.translatable(CAPTION + ".tooltip")),
				Options::genericValueOrOffLabel,
				new OptionInstance.IntRange(MIN, MAX),
				GlowtoneConfig.occlusionStrength(),
				OcclusionStrengthOption::apply
			);
		}
		return instance;
	}

	public static boolean enabled() {
		return GlowtoneConfig.occlusionStrength() > MIN;
	}

	public static boolean available() {
		return AmbientOcclusionOption.available() && GlowtoneConfig.ambientOcclusion() != AmbientOcclusionMode.OFF;
	}

	public static float scale() {
		return GlowtoneConfig.occlusionStrength() / (float) VANILLA;
	}

	public static float strength() {
		return scale() * VANILLA_DEPTH * COVERAGE_REFERENCE;
	}

	public static float brightness(float vanilla) {
		final int value = GlowtoneConfig.occlusionStrength();
		if (value == VANILLA) return vanilla;

		return 1F - (1F - vanilla) * (value / (float) VANILLA);
	}

	private static void apply(int value) {
		if (GlowtoneConfig.occlusionStrength() == value) return;

		GlowtoneConfig.setOcclusionStrength(value);
		pendingReload = true;
	}

	static void rebuildSatisfied() {
		pendingReload = false;
	}

	public static void flush() {
		if (!pendingReload) return;
		pendingReload = false;

		if (AmbientOcclusionOption.available()) AmbientOcclusionOption.rebuild();
	}
}
