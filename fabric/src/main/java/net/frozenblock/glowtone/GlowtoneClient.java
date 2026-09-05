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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneDebugCapture;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.frozenblock.glowtone.config.option.color.ColoredLightingOption;
import net.frozenblock.glowtone.config.option.shade.ShadingOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettingsLoader;
import net.frozenblock.glowtone.emissive.entity.RenderTypeTextureValidityCache;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.frozenblock.glowtone.light.data.block.BlockStateLightPropertiesLoader;
import net.frozenblock.glowtone.material.data.BlockMaterialOverrideLoader;
import net.minecraft.server.packs.PackType;

public final class GlowtoneClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		GlowtoneDebugEntries.register();
		GlowtoneReload.register();
		GlowtoneDebugCapture.register();
		ShadingOption.applyFlags(GlowtoneConfig.shading());
		ColoredLightingOption.applyMode(GlowtoneConfig.coloredLighting());

		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GlowtoneConstants.id("block_light"), new BlockStateLightPropertiesLoader());
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GlowtoneConstants.id("block_material"), new BlockMaterialOverrideLoader());
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GlowtoneConstants.id("settings"), new GlowtonePackSettingsLoader());
		RenderTypeTextureValidityCache.init();

		FabricLoader.getInstance().getModContainer(GlowtoneConstants.MOD_ID).ifPresent(container ->
			ResourceLoader.registerBuiltinPack(
				GlowtoneConstants.id("test"),
				container,
				Component.literal("Glowtone Materials (Experimental)"),
				PackActivationType.NORMAL
			));

		// MOD COMPAT
		GlowtoneDynamicLights.init();
	}
}
