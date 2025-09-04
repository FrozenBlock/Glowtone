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
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.frozenblock.glowtone.render_type.impl.BlockRenderTypeOverwriteManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import java.util.Optional;

public final class GlowtoneClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(GlowtoneConstants.MOD_ID);
		if (modContainer.isEmpty()) return;
		final ModContainer container = modContainer.get();

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(BlockRenderTypeOverwriteManager.INSTANCE);

		ResourceManagerHelper.registerBuiltinResourcePack(
			GlowtoneConstants.id("glowtone_shading"),
			container,
			Component.translatable("pack.glowtone.glowtone_shading"),
			ResourcePackActivationType.NORMAL
		);

		ResourceManagerHelper.registerBuiltinResourcePack(
			GlowtoneConstants.id("glowtone_emissives"),
			container,
			Component.translatable("pack.glowtone.glowtone_emissives"),
			ResourcePackActivationType.DEFAULT_ENABLED
		);
	}

}
