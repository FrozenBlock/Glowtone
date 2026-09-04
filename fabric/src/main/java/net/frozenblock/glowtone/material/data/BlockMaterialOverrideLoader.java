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

package net.frozenblock.glowtone.material.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.slf4j.Logger;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@ClientOnly
public final class BlockMaterialOverrideLoader implements PreparableReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter OVERRIDE_LISTER = FileToIdConverter.json(BlockMaterialRenderer.OVERRIDE_DIRECTORY);

	private static CompletableFuture<Map<BlockState, BlockMaterialOverrideDispatcher.Assignment>> loadOverrides(ResourceManager manager, Executor executor) {
		final Function<Identifier, StateDefinition<Block, BlockState>> definitionToBlockState = BlockStateDefinitions.definitionLocationToBlockStateMapper();
		return CompletableFuture.supplyAsync(() -> OVERRIDE_LISTER.listMatchingResourceStacks(manager), executor).thenCompose(
			resources -> {
				final List<CompletableFuture<Map<BlockState, BlockMaterialOverrideDispatcher.Assignment>>> result = new ArrayList<>(resources.size());

				for (Map.Entry<Identifier, List<Resource>> resourceStack : resources.entrySet()) {
					result.add(CompletableFuture.supplyAsync(
						() -> {
							final Identifier stateDefinitionId = OVERRIDE_LISTER.fileToId(resourceStack.getKey());
							final StateDefinition<Block, BlockState> stateDefinition = definitionToBlockState.apply(stateDefinitionId);
							if (stateDefinition == null) {
								LOGGER.debug("Discovered unknown block material override {}, ignoring", stateDefinitionId);
								return Map.<BlockState, BlockMaterialOverrideDispatcher.Assignment>of();
							}

							final Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> assignments = new IdentityHashMap<>();
							for (Resource resource : resourceStack.getValue()) {
								try (Reader reader = resource.openAsReader()) {
									final JsonElement element = StrictJsonParser.parse(reader);
									final BlockMaterialOverrideDispatcher definition = BlockMaterialOverrideDispatcher.CODEC
										.parse(JsonOps.INSTANCE, element)
										.getOrThrow(JsonParseException::new);

									assignments.putAll(definition.instantiate(stateDefinition, () -> stateDefinitionId + "/" + resource.sourcePackId()));
								} catch (Exception e) {
									LOGGER.error("Failed to load block material override {} from pack {}", stateDefinitionId, resource.sourcePackId(), e);
								}
							}

							return assignments;
						},
						executor));
				}

				return Util.sequence(result).thenApply(partialMaps -> {
					final Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> fullMap = new IdentityHashMap<>();
					for (Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> partialMap : partialMaps) fullMap.putAll(partialMap);
					LOGGER.info("Glowtone found {} block material override files covering {} blockstates",
						resources.size(), fullMap.size());
					return fullMap;
				});
			});
	}

	public static void applyShaderSource(ResourceManager manager) {
		try {
			BlockMaterialLoader.applyShaderSource(
				loadOverrides(manager, Runnable::run).join(),
				BlockMaterialLoader.load(manager, Runnable::run).join()
			);
		} catch (Throwable failure) {
			LOGGER.error("Glowtone failed to read block materials before shaders were built", failure);
		}
	}

	@Override
	public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor) {
		final ResourceManager manager = currentReload.resourceManager();
		final CompletableFuture<BlockMaterialLoader.Definitions> definitions = BlockMaterialLoader.load(manager, taskExecutor);
		final CompletableFuture<Map<BlockState, BlockMaterialOverrideDispatcher.Assignment>> overrides = loadOverrides(manager, taskExecutor);

		return definitions
			.exceptionally(failure -> {
				LOGGER.error("Glowtone failed to read block material definitions", failure);
				return new BlockMaterialLoader.Definitions(Map.of(), Map.of());
			})
			.thenCombine(
				overrides.exceptionally(failure -> {
					LOGGER.error("Glowtone failed to read block material overrides", failure);
					return Map.of();
				}),
				Loaded::new
			)
			.thenCompose(preparationBarrier::wait)
			.thenAcceptAsync(loaded -> {
				try {
					BlockMaterialLoader.apply(loaded.overrides(), loaded.definitions());
				} catch (Throwable failure) {
					LOGGER.error("Glowtone failed to apply block materials", failure);
				}
			}, reloadExecutor);
	}

	private record Loaded(BlockMaterialLoader.Definitions definitions, Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides) {}
}
