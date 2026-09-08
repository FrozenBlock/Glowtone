/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone;

import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneDebugCapture;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.frozenblock.glowtone.config.option.color.ColoredLightingOption;
import net.frozenblock.glowtone.config.option.shade.ShadingOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettingsLoader;
import net.frozenblock.glowtone.data.BlockMaterialOverrideLoader;
import net.frozenblock.glowtone.entity.RenderTypeTextureValidityCache;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.frozenblock.glowtone.light.data.block.BlockStateLightPropertiesLoader;
import net.frozenblock.glowtone.light.edge.impl.CasterBoxCacheReloader;
import net.frozenblock.glowtone.light.occlusion.impl.AmbientOcclusionCacheLoader;
import net.frozenblock.glowtone.platform.GlowtonePlatform;

public final class GlowtoneClient {

	public static void init() {
		GlowtoneDebugEntries.register();
		GlowtoneReload.register();
		GlowtoneDebugCapture.register();
		ShadingOption.applyFlags(GlowtoneConfig.shading());
		ColoredLightingOption.applyMode(GlowtoneConfig.coloredLighting());

		GlowtonePlatform.INSTANCE.registerResourceListener("block_light", new BlockStateLightPropertiesLoader());
		GlowtonePlatform.INSTANCE.registerResourceListener("block_material", new BlockMaterialOverrideLoader());
		GlowtonePlatform.INSTANCE.registerResourceListener("settings", new GlowtonePackSettingsLoader());
		GlowtonePlatform.INSTANCE.registerResourceListener("ambient_occlusion_cache", new AmbientOcclusionCacheLoader());
		GlowtonePlatform.INSTANCE.registerResourceListener("caster_box_cache_reloader", new CasterBoxCacheReloader());
		RenderTypeTextureValidityCache.init();

		GlowtonePlatform.INSTANCE.registerResourcePack("builtin_materials", false);

		// MOD COMPAT
		GlowtoneDynamicLights.init();
	}
}
