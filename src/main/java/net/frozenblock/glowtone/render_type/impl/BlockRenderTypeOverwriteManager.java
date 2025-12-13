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

package net.frozenblock.glowtone.render_type.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

@ApiStatus.Internal
@Environment(EnvType.CLIENT)
public class BlockRenderTypeOverwriteManager extends SimpleResourceReloader<BlockRenderTypeOverwriteManager.RenderTypeLoader> {
	private static final Logger LOGGER = LoggerFactory.getLogger("Block Render Type Overwrite Manager");
	private static final String DIRECTORY = "glowtone_block_render_type_overwrites";

	public static final BlockRenderTypeOverwriteManager INSTANCE = new BlockRenderTypeOverwriteManager();

	private final List<BlockRenderTypeOverwrite> overwrites = new ArrayList<>();

	public void addFinalizedOverwrite(BlockRenderTypeOverwrite overwrite) {
		this.overwrites.add(overwrite);
	}

	private void applyOverwrites() {
		this.overwrites.forEach(overwrite -> {
			BlockRenderLayerMap.putBlock(overwrite.block(), overwrite.getRenderType());
		});
	}

	@Override
	protected RenderTypeLoader prepare(SharedState sharedState) {
		return new RenderTypeLoader(sharedState.resourceManager());
	}

	@Override
	protected void apply(RenderTypeLoader renderTypeLoader, SharedState sharedState) {
		this.overwrites.clear();
		renderTypeLoader.getOverwrites().forEach(this::addFinalizedOverwrite);
		this.applyOverwrites();
	}

	@Override
	public String getName() {
		return "Block Render Type Overwrite Manager";
	}

	public static class RenderTypeLoader {
		private static final String REPLACEMENT_DIRECTORY = DIRECTORY + "/";
		private final ResourceManager manager;
		private final List<BlockRenderTypeOverwrite> parsedOverwrites = new ArrayList<>();

		public RenderTypeLoader(ResourceManager manager) {
			this.manager = manager;
			this.loadRenderTypeOverwrites();
		}

		private void loadRenderTypeOverwrites() {
			final Map<Identifier, Resource> resources = this.manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"));
			final Set<Map.Entry<Identifier, Resource>> entrySet = resources.entrySet();
			for (Map.Entry<Identifier, Resource> entry : entrySet) this.addOverwrite(entry.getKey(), entry.getValue());
		}

		private void addOverwrite(Identifier id, Resource resource) {
			BufferedReader reader;
			try {
				reader = resource.openAsReader();
			} catch (IOException e) {
				LOGGER.error("Unable to open BufferedReader for file: `{}`", id);
				return;
			}

			final JsonObject json = GsonHelper.parse(reader);
			final DataResult<? extends Pair<? extends BlockRenderTypeOverwrite.RenderTypeOverwriteHolder, JsonElement>> dataResult
				= BlockRenderTypeOverwrite.RenderTypeOverwriteHolder.CODEC.decode(JsonOps.INSTANCE, json);

			dataResult.resultOrPartial(string -> LOGGER.error("Failed to parse render type override for file: '{}'", id))
				.ifPresent(overwrite -> {
					final Identifier blockName = Identifier.fromNamespaceAndPath(
						id.getNamespace(),
						id.getPath()
							.replace(".json", "")
							.replaceFirst(REPLACEMENT_DIRECTORY, "")
					);

					final Block block = BuiltInRegistries.BLOCK.getOptional(blockName).orElse(null);
					if (block != null) {
						parsedOverwrites.add(new BlockRenderTypeOverwrite(block, overwrite.getFirst().renderTypeOverwrite()));
					} else {
						LOGGER.error("Failed to find block of name: '{}'", blockName);
					}
				});
		}

		public List<BlockRenderTypeOverwrite> getOverwrites() {
			return this.parsedOverwrites;
		}
	}
}
