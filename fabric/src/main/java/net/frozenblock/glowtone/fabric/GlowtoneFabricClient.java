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

package net.frozenblock.glowtone.fabric;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.frozenblock.glowtone.GlowtoneClientCommon;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.fabric.render.EmissiveForwardingModel;
import net.frozenblock.glowtone.fabric.render.GlowtoneShaders;
import net.minecraft.network.chat.Component;

public final class GlowtoneFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GlowtoneClientCommon.init();

		CoreShaderRegistrationCallback.EVENT.register(context ->
			context.register(
				GlowtoneConstants.id("glowtone_item_emissive"),
				DefaultVertexFormat.NEW_ENTITY,
				shader -> GlowtoneShaders.ITEM_EMISSIVE = shader
			)
		);

		ModelLoadingPlugin.register(pluginContext ->
			pluginContext.modifyModelAfterBake().register((model, context) ->
				(model == null || model.isCustomRenderer()) ? model : new EmissiveForwardingModel(model)
			)
		);

		final ModContainer container = FabricLoader.getInstance()
			.getModContainer(GlowtoneConstants.MOD_ID)
			.orElseThrow(() -> new IllegalStateException("Glowtone mod container missing"));

		ResourceManagerHelper.registerBuiltinResourcePack(
			GlowtoneConstants.id("glowtone_emissives"),
			container,
			Component.translatable("pack.glowtone.glowtone_emissives"),
			ResourcePackActivationType.DEFAULT_ENABLED
		);
		ResourceManagerHelper.registerBuiltinResourcePack(
			GlowtoneConstants.id("glowtone_shading"),
			container,
			Component.translatable("pack.glowtone.glowtone_shading"),
			ResourcePackActivationType.NORMAL
		);
	}
}
