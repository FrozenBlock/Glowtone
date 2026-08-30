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
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.frozenblock.glowtone.material.BlockMaterial;
import net.frozenblock.glowtone.material.BlockMaterialDefinition;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.frozenblock.glowtone.material.MaterialShader;
import net.frozenblock.glowtone.material.MaterialShaders;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ClientOnly
public final class BlockMaterialLoader {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter MATERIAL_LISTER = FileToIdConverter.json(BlockMaterials.RESOURCE_PACK_DIRECTORY);

	private static final FileToIdConverter SHADER_LISTER =
		new FileToIdConverter(MaterialShader.RESOURCE_PACK_DIRECTORY, MaterialShader.FILE_SUFFIX);
	public record Definitions(Map<Identifier, BlockMaterial> materials, Map<Identifier, String> shaderSources) {}

	public static CompletableFuture<Definitions> load(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			final Map<Identifier, BlockMaterial> materials = resolve(read(manager));
			final Map<Identifier, String> sources = readShaderSources(manager, materials);
			LOGGER.info("Glowtone read {} block material definitions and {} material shaders",
				materials.size(), sources.size());
			return new Definitions(materials, sources);
		}, executor);
	}

	private static Map<Identifier, String> readShaderSources(ResourceManager manager, Map<Identifier, BlockMaterial> materials) {
		final Set<Identifier> wanted = new HashSet<>();
		materials.values().forEach(material -> material.shader().ifPresent(shader -> {
			shader.fragment().ifPresent(wanted::add);
			shader.vertex().ifPresent(wanted::add);
		}));
		if (wanted.isEmpty()) return Map.of();

		final Map<Identifier, String> sources = new HashMap<>(wanted.size());
		for (Identifier fragment : wanted) {
			final Identifier file = SHADER_LISTER.idToFile(fragment);
			manager.getResource(file).ifPresentOrElse(
				resource -> {
					try (Reader reader = resource.openAsReader()) {
						sources.put(fragment, readAll(reader));
					} catch (Exception e) {
						LOGGER.error("Failed to read material shader {}", file, e);
					}
				},
				() -> LOGGER.error("Material shader {} does not exist", file)
			);
		}

		return sources;
	}

	private static String readAll(Reader reader) throws IOException {
		final StringBuilder builder = new StringBuilder();
		final char[] buffer = new char[8192];
		int read;
		while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);

		return builder.toString();
	}

	private static Map<Identifier, BlockMaterialDefinition> read(ResourceManager manager) {
		final Map<Identifier, BlockMaterialDefinition> definitions = new HashMap<>();

		for (Map.Entry<Identifier, List<Resource>> resourceStack : MATERIAL_LISTER.listMatchingResourceStacks(manager).entrySet()) {
			final Identifier materialId = MATERIAL_LISTER.fileToId(resourceStack.getKey());

			for (Resource resource : resourceStack.getValue()) {
				try (Reader reader = resource.openAsReader()) {
					final JsonElement element = StrictJsonParser.parse(reader);
					final BlockMaterialDefinition definition = BlockMaterialDefinition.CODEC
						.parse(JsonOps.INSTANCE, element)
						.getOrThrow(JsonParseException::new);

					definitions.merge(materialId, definition, BlockMaterialLoader::mergedOver);
				} catch (Exception e) {
					LOGGER.error("Failed to load block material {} from pack {}", materialId, resource.sourcePackId(), e);
				}
			}
		}

		return definitions;
	}

	private static BlockMaterialDefinition mergedOver(BlockMaterialDefinition under, BlockMaterialDefinition over) {
		return new BlockMaterialDefinition(
			over.parent().or(under::parent),
			over.material().mergedOver(under.material())
		);
	}

	private static Map<Identifier, BlockMaterial> resolve(Map<Identifier, BlockMaterialDefinition> definitions) {
		final Map<Identifier, BlockMaterial> resolved = new HashMap<>(definitions.size());
		for (Identifier materialId : definitions.keySet()) resolveInto(materialId, definitions, resolved);
		return resolved;
	}

	private static BlockMaterial resolveInto(
		Identifier materialId, Map<Identifier, BlockMaterialDefinition> definitions, Map<Identifier, BlockMaterial> resolved
	) {
		final BlockMaterial alreadyResolved = resolved.get(materialId);
		if (alreadyResolved != null) return alreadyResolved;

		final Set<Identifier> chain = new HashSet<>();
		BlockMaterial merged = BlockMaterial.NONE;
		Identifier current = materialId;

		while (current != null && chain.add(current)) {
			final BlockMaterialDefinition definition = definitions.get(current);
			if (definition == null) {
				LOGGER.warn("Block material {} refers to unknown parent {}, ignoring the rest of the chain", materialId, current);
				current = null;
				break;
			}

			merged = merged.mergedOver(definition.material());
			current = definition.parent().orElse(null);
		}

		if (current != null) LOGGER.warn("Block material {} has a cyclic parent chain at {}, stopping there", materialId, current);

		resolved.put(materialId, merged);
		return merged;
	}

	public static void apply(Map<BlockState, Identifier> overrides, Definitions definitions) {
		final Map<Identifier, BlockMaterial> registry = definitions.materials();
		BuiltInRegistries.BLOCK.forEach(block -> block.frozenLib$removeAttached(BlockMaterials.ATTACHMENT_KEY));

		final Map<Block, Map<BlockState, BlockMaterials.Assigned>> perBlock = new IdentityHashMap<>();
		final Set<Identifier> missing = new HashSet<>();
		final Set<Identifier> unsupportedLayers = new HashSet<>();
		final Map<Identifier, Integer> shaderIndices = new HashMap<>();
		final List<MaterialShaders.Loaded> shaders = new ArrayList<>();
		final List<Identifier> samplerSlots = new ArrayList<>();
		boolean layers = false;
		boolean selfCulling = false;
		boolean castCulling = false;
		boolean renderShape = false;
		boolean blockEntity = false;

		for (Map.Entry<BlockState, Identifier> entry : overrides.entrySet()) {
			final Identifier materialId = entry.getValue();
			final BlockMaterial material = registry.get(materialId);
			if (material == null) {
				if (missing.add(materialId)) LOGGER.warn("Block material {} was assigned but never defined, ignoring it", materialId);
				continue;
			}
			if (material.isNone()) continue;

			final int shaderIndex = shaderIndices.computeIfAbsent(
				materialId, id -> allocateShader(id, material, definitions.shaderSources(), shaders, samplerSlots)
			);

			final BlockState state = entry.getKey();
			perBlock.computeIfAbsent(state.getBlock(), block -> new IdentityHashMap<>())
				.put(state, new BlockMaterials.Assigned(materialId, material, shaderIndex));

			material.layer().filter(MaterialLayer::custom).ifPresent(layer -> {
				if (unsupportedLayers.add(layer.id())) {
					LOGGER.warn("Block material {} asks for layer {}, but only solid, cutout and translucent render; leaving it on its model's layer",
						materialId, layer.id());
				}
			});

			layers |= material.overridesLayer();
			selfCulling |= material.cull().selfMode().decides();
			castCulling |= material.cull().castMode().decides();
			renderShape |= material.overridesRenderShape();
			blockEntity |= material.overridesBlockEntityRender();
		}

		BlockMaterials.setLoadedFeatures(layers, selfCulling, castCulling, !shaders.isEmpty(), renderShape, blockEntity);

		final String previousShaderSource = MaterialShaders.generateFunctions(true);
		MaterialSamplers.apply(samplerSlots);
		MaterialShaders.apply(shaders);
		if (!previousShaderSource.equals(MaterialShaders.generateFunctions(true))) GlowtoneReload.request();

		perBlock.forEach((block, materials) -> {
			final BlockMaterials.Baked baked;
			if (materials.keySet().containsAll(block.getStateDefinition().getPossibleStates())
				&& materials.values().stream().distinct().count() <= 1
			) {
				baked = new BlockMaterials.Simple(materials.get(block.defaultBlockState()));
			} else {
				baked = new BlockMaterials.MultiVariant(materials);
			}

			block.frozenLib$setAttached(BlockMaterials.ATTACHMENT_KEY, baked);
		});

		rebuildChunks();

		LOGGER.info("Glowtone applied block materials: {} blockstates across {} blocks, {} shader materials",
			overrides.size(), perBlock.size(), shaders.size());
	}

	// The material index is baked into chunk meshes, so terrain only picks up a change once they rebuild.
	private static void rebuildChunks() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;

		minecraft.execute(() -> {
			if (minecraft.level != null) minecraft.levelExtractor.allChanged();
		});
	}

	private static int allocateShader(
		Identifier materialId,
		BlockMaterial material,
		Map<Identifier, String> sources,
		List<MaterialShaders.Loaded> shaders,
		List<Identifier> samplerSlots
	) {
		final MaterialShader shader = material.shader().orElse(null);
		if (shader == null || shader.isEmpty()) return BlockMaterials.NO_SHADER;

		final String fragmentSource = stageSource(materialId, "fragment", shader.fragment(), sources);
		final String vertexSource = stageSource(materialId, "vertex", shader.vertex(), sources);
		if (fragmentSource == null && vertexSource == null) return BlockMaterials.NO_SHADER;

		if (shaders.size() >= BlockMaterials.MAX_SHADER_INDEX) {
			LOGGER.error("Block material {} exceeds the limit of {} shader materials, ignoring its shader",
				materialId, BlockMaterials.MAX_SHADER_INDEX);
			return BlockMaterials.NO_SHADER;
		}

		final Map<String, Integer> slots = new LinkedHashMap<>(shader.textures().size());
		for (Map.Entry<String, Identifier> texture : shader.textures().entrySet()) {
			int slot = samplerSlots.indexOf(texture.getValue());
			if (slot < 0) {
				if (samplerSlots.size() >= MaterialSamplers.SLOTS) {
					LOGGER.error("Block material {} needs more than {} distinct shader textures, ignoring its shader",
						materialId, MaterialSamplers.SLOTS);
					return BlockMaterials.NO_SHADER;
				}

				samplerSlots.add(texture.getValue());
				slot = samplerSlots.size() - 1;
			}

			slots.put(texture.getKey(), slot);
		}

		shaders.add(new MaterialShaders.Loaded(materialId, shader, fragmentSource, vertexSource, slots));
		return shaders.size();
	}

	private static @Nullable String stageSource(
		Identifier materialId, String stage, Optional<Identifier> file, Map<Identifier, String> sources
	) {
		final Identifier id = file.orElse(null);
		if (id == null) return null;

		final String source = sources.get(id);
		if (source == null) {
			LOGGER.error("Block material {} wants a {} shader from {} but its source was not read, skipping that stage",
				materialId, stage, id);
		}

		return source;
	}

	private BlockMaterialLoader() {}
}
