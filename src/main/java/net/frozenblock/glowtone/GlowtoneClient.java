/*
 * Copyright 2025 FrozenBlock
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
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.frozenblock.glowtone.render_type.impl.BlockRenderTypeOverwriteManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import java.util.Optional;

public final class GlowtoneClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		final Optional<ModContainer> optionalModContainer = FabricLoader.getInstance().getModContainer(GlowtoneConstants.MOD_ID);
		if (optionalModContainer.isEmpty()) return;
		final ModContainer modContainer = optionalModContainer.get();

		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
			GlowtoneConstants.id("block_render_type_overwrite_manager"),
			BlockRenderTypeOverwriteManager.INSTANCE
		);

		ResourceLoader.registerBuiltinPack(
			GlowtoneConstants.id("glowtone_shading"),
			modContainer,
			Component.translatable("pack.glowtone.glowtone_shading"),
			PackActivationType.NORMAL
		);

		ResourceLoader.registerBuiltinPack(
			GlowtoneConstants.id("glowtone_emissives"),
			modContainer,
			Component.translatable("pack.glowtone.glowtone_emissives"),
			PackActivationType.DEFAULT_ENABLED
		);
	}

}
