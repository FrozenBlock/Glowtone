package net.frozenblock.glowtone.light.color.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
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

@Environment(EnvType.CLIENT)
public final class BlockStateLightLoader implements PreparableReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter BLOCKSTATE_LIGHT_LISTER = FileToIdConverter.json("glowtone_blockstate_lights");

	public static CompletableFuture<LoadedLights> loadBlockStates(ResourceManager manager, Executor executor) {
		final Function<Identifier, StateDefinition<Block, BlockState>> definitionToBlockState = BlockStateDefinitions.definitionLocationToBlockStateMapper();
		return CompletableFuture.supplyAsync(() -> BLOCKSTATE_LIGHT_LISTER.listMatchingResourceStacks(manager), executor).thenCompose(
			resources -> {
				final List<CompletableFuture<LoadedLights>> result = new ArrayList<>(resources.size());

				for (Map.Entry<Identifier, List<Resource>> resourceStack : resources.entrySet()) {
					result.add(CompletableFuture.supplyAsync(
						() -> {
							final Identifier stateDefinitionId = BLOCKSTATE_LIGHT_LISTER.fileToId(resourceStack.getKey());
							final StateDefinition<Block, BlockState> stateDefinition = definitionToBlockState.apply(stateDefinitionId);
							if (stateDefinition == null) {
								LOGGER.debug("Discovered unknown block state light definition {}, ignoring", stateDefinitionId);
								return null;
							}

							final List<Resource> stack = resourceStack.getValue();
							final List<LoadedBlockStateLightDispatcher> loadedStack = new ArrayList<>(stack.size());
							for (Resource resource : stack) {
								try (Reader reader = resource.openAsReader()) {
									final JsonElement element = StrictJsonParser.parse(reader);
									final BlockLightDispatcher definition = BlockLightDispatcher.CODEC
										.parse(JsonOps.INSTANCE, element)
										.getOrThrow(JsonParseException::new);
									loadedStack.add(new LoadedBlockStateLightDispatcher(resource.sourcePackId(), definition));
								} catch (Exception e) {
									LOGGER.error("Failed to load blockstate light definition {} from pack {}", stateDefinitionId, resource.sourcePackId(), e);
								}
							}

							try {
								return loadBlockStateDefinitionStack(stateDefinitionId, stateDefinition, loadedStack);
							} catch (Exception e) {
								LOGGER.error("Failed to load blockstate light definition {}", stateDefinitionId, e);
								return null;
							}
						},
						executor));
				}

				return Util.sequence(result).thenApply(partialMaps -> {
					final Map<BlockState, BlockLight> fullMap = new IdentityHashMap<>();

					for (LoadedLights partialMap : partialMaps) {
						if (partialMap != null) fullMap.putAll(partialMap.lights());
					}

					return new LoadedLights(fullMap);
				});
			});
	}

	private static LoadedLights loadBlockStateDefinitionStack(
		Identifier stateDefinitionId,
		StateDefinition<Block, BlockState> stateDefinition,
		List<LoadedBlockStateLightDispatcher> definitionStack
	) {
		final Map<BlockState, BlockLight> result = new IdentityHashMap<>();

		for (LoadedBlockStateLightDispatcher definition : definitionStack) {
			result.putAll(definition.contents.instantiate(stateDefinition, () -> stateDefinitionId + "/" + definition.source));
		}

		return new LoadedLights(result);
	}

	@Override
	public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor) {
		final ResourceManager manager = currentReload.resourceManager();
		final CompletableFuture<LoadedLights> blockStateModels = loadBlockStates(manager, taskExecutor);

		return blockStateModels
			.thenCompose(preparationBarrier::wait)
			.thenAcceptAsync(lights -> {
				BuiltInRegistries.BLOCK.forEach(block -> block.frozenLib$removeAttached(BlockLight.ATTACHMENT_KEY));

				final Map<Block, Map<BlockState, BlockLight>> fullMap = new IdentityHashMap<>();
				lights.lights().forEach((blockState, light) -> {
					final Map<BlockState, BlockLight> blockMap = fullMap.getOrDefault(blockState.getBlock(), new IdentityHashMap<>());
					blockMap.put(blockState, light);
					fullMap.put(blockState.getBlock(), blockMap);
				});

				final Map<Block, BlockLight.Baked> bakedMap = new IdentityHashMap<>();
				BuiltInRegistries.BLOCK.forEach(block -> {
					final Map<BlockState, BlockLight> lightMap = fullMap.get(block);
					if (lightMap == null) return;

					if (block.getStateDefinition().getPossibleStates().containsAll(lightMap.keySet())) {
						bakedMap.put(block, new BlockLight.Simple(lightMap.get(block.defaultBlockState())));
					} else {
						bakedMap.put(block, new BlockLight.MultiVariant(lightMap));
					}
				});

				bakedMap.forEach((block, blockLight) -> block.frozenLib$setAttached(BlockLight.ATTACHMENT_KEY, blockLight));
			});
	}

	private record LoadedBlockStateLightDispatcher(String source, BlockLightDispatcher contents) {}

	public record LoadedLights(Map<BlockState, BlockLight> lights) {}
}
