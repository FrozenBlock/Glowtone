/*
 * Copyright 2025-2026 FrozenBlock
 * This file is part of Wilder Wild.
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

package net.frozenblock.glowtone.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.data.light.color.GlowtoneBlockLightProvider;
import net.frozenblock.lib.feature_flag.api.FeatureFlagApi;

public final class GTDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
		FeatureFlagApi.rebuild();
		final FabricDataGenerator.Pack pack = dataGenerator.createPack();

		// ASSETS
		pack.addProvider((FabricDataGenerator.Pack.Factory<GlowtoneBlockLightProvider>) GlowtoneBlockLightProvider::new);
	}

	@Override
	public String getEffectiveModId() {
		return GlowtoneConstants.MOD_ID;
	}
}
