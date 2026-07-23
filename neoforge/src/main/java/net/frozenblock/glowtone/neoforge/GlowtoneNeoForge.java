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

package net.frozenblock.glowtone.neoforge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.Map;
import net.frozenblock.glowtone.GlowtoneClientCommon;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.neoforge.render.GlowtoneNeoForgeItemRenderTypes;
import net.frozenblock.glowtone.neoforge.render.NeoForgeEmissiveModel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(value = GlowtoneConstants.MOD_ID, dist = Dist.CLIENT)
public final class GlowtoneNeoForge {
	public GlowtoneNeoForge(IEventBus modEventBus) {
		GlowtoneClientCommon.init();
		modEventBus.addListener(this::onAddPackFinders);
		modEventBus.addListener(this::onModifyBakingResult);
		modEventBus.addListener(this::onRegisterShaders);
	}

	private void onRegisterShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(
				new ShaderInstance(event.getResourceProvider(), GlowtoneConstants.id("glowtone_item_emissive"), DefaultVertexFormat.NEW_ENTITY),
				shader -> GlowtoneNeoForgeItemRenderTypes.itemEmissiveShader = shader
			);
		} catch (IOException exception) {
			GlowtoneConstants.LOGGER.error("Failed to load Glowtone item emissive shader", exception);
		}
	}

	private void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		final Map<ModelResourceLocation, BakedModel> models = event.getModels();
		for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
			entry.setValue(new NeoForgeEmissiveModel(entry.getValue()));
		}
	}

	private void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

		event.addPackFinders(
			GlowtoneConstants.id("resourcepacks/glowtone_emissives"),
			PackType.CLIENT_RESOURCES,
			Component.translatable("pack.glowtone.glowtone_emissives"),
			PackSource.BUILT_IN,
			true,
			Pack.Position.TOP
		);
		event.addPackFinders(
			GlowtoneConstants.id("resourcepacks/glowtone_shading"),
			PackType.CLIENT_RESOURCES,
			Component.translatable("pack.glowtone.glowtone_shading"),
			PackSource.BUILT_IN,
			false,
			Pack.Position.TOP
		);
	}
}
