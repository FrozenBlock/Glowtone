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
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.frozenblock.glowtone.material.MaterialShaderNames;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;
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
import java.util.Comparator;
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
	private static final FileToIdConverter MATERIAL_LISTER = FileToIdConverter.json(BlockMaterialRenderer.RESOURCE_PACK_DIRECTORY);

	private static final FileToIdConverter SHADER_LISTER = new FileToIdConverter(MaterialShader.RESOURCE_PACK_DIRECTORY, MaterialShader.FILE_SUFFIX);
	public record Definitions(Map<Identifier, BlockMaterial> materials, Map<Identifier, String> shaderSources) {}

	public static CompletableFuture<Definitions> load(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			final Map<Identifier, BlockMaterial> materials = resolve(read(manager));
			publishWantedSlots(materials);
			final Map<Identifier, String> sources = readShaderSources(manager, materials);
			LOGGER.info("Glowtone read {} block material definitions and {} material shaders", materials.size(), sources.size());
			return new Definitions(materials, sources);
		}, executor);
	}

	private static void publishWantedSlots(Map<Identifier, BlockMaterial> materials) {
		final Set<String> wanted = new HashSet<>();
		materials.values().forEach(material -> {
			material.shader().ifPresent(shader -> wanted.addAll(shader.blockTextures()));
			wanted.addAll(material.target());
		});

		BlockTextureSlots.setWanted(wanted);
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
		for (Identifier materialId : definitions.keySet()) resolveDependencies(materialId, definitions, resolved);
		return resolved;
	}

	private static BlockMaterial resolveDependencies(
		Identifier materialId,
		Map<Identifier, BlockMaterialDefinition> definitions,
		Map<Identifier, BlockMaterial> resolved
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

	public static void apply(Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides, Definitions definitions) {
		final Map<Identifier, BlockMaterial> registry = definitions.materials();
		BuiltInRegistries.BLOCK.forEach(block -> block.frozenLib$removeAttached(BlockMaterial.ATTACHMENT_KEY));

		final Map<Block, Map<BlockState, BlockMaterial.Assigned>> perBlock = new IdentityHashMap<>();
		final Set<Identifier> missing = new HashSet<>();
		final Set<Identifier> unsupportedLayers = new HashSet<>();
		final Map<ShaderKey, Integer> shaderIndices = new HashMap<>();
		final List<MaterialShaderPatcher.Loaded> shaders = new ArrayList<>();
		final List<Identifier> samplerSlots = new ArrayList<>();
		boolean layers = false;
		boolean selfCulling = false;
		boolean castCulling = false;
		boolean renderShape = false;
		boolean blockEntity = false;
		boolean targets = false;

		overrides.values().stream()
			.map(assignment -> new ShaderKey(assignment.material(), assignment.parameters()))
			.distinct()
			.sorted(Comparator.comparing(ShaderKey::sortOrder))
			.forEach(key -> {
				final BlockMaterial material = registry.get(key.material());
				if (material == null || material.isNone()) return;

				shaderIndices.computeIfAbsent(
					key, entry -> allocateShader(entry, material, definitions.shaderSources(), shaders, samplerSlots)
				);
			});

		for (Map.Entry<BlockState, BlockMaterialOverrideDispatcher.Assignment> entry : overrides.entrySet()) {
			final Identifier materialId = entry.getValue().material();
			final BlockMaterial material = registry.get(materialId);
			if (material == null) {
				if (missing.add(materialId)) LOGGER.warn("Block material {} was assigned but never defined, ignoring it", materialId);
				continue;
			}
			if (material.isNone()) continue;

			final int shaderIndex = shaderIndices.getOrDefault(
				new ShaderKey(materialId, entry.getValue().parameters()), BlockMaterialRenderer.NO_SHADER);

			final BlockState state = entry.getKey();
			perBlock.computeIfAbsent(state.getBlock(), block -> new IdentityHashMap<>())
				.put(state, new BlockMaterial.Assigned(materialId, material, shaderIndex, material.target().isEmpty() ? null : material.target()));

			material.layer().filter(MaterialLayer::custom).ifPresent(layer -> {
				if (!unsupportedLayers.add(layer.id())) return;
				LOGGER.warn("Block material {} asks for layer {}, but only solid, cutout and translucent render; leaving it on its model's layer", materialId, layer.id());
			});

			layers |= material.overridesLayer();
			selfCulling |= material.cull().selfMode().decides();
			castCulling |= material.cull().castMode().decides();
			renderShape |= material.overridesRenderShape();
			blockEntity |= material.overridesBlockEntityRender();
			targets |= !material.target().isEmpty();
		}

		BlockMaterialRenderer.setLoadedFeatures(layers, selfCulling, castCulling, !shaders.isEmpty(), renderShape, blockEntity, targets);

		final Map<Integer, BlockMaterial.Assigned> byIndex = new HashMap<>();
		perBlock.values().forEach(states -> states.values().forEach(assigned -> {
			if (assigned.shaderIndex() != BlockMaterialRenderer.NO_SHADER) byIndex.putIfAbsent(assigned.shaderIndex(), assigned);
		}));
		BlockMaterialRenderer.setAssignedByIndex(byIndex);

		final String previousShaderSource = MaterialShaderPatcher.generateFunctions(true);
		MaterialSamplers.apply(samplerSlots);
		MaterialShaderPatcher.apply(shaders);
		if (!previousShaderSource.equals(MaterialShaderPatcher.generateFunctions(true))) GlowtoneReload.request();

		perBlock.forEach((block, materials) -> {
			final BlockMaterial.Baked baked;
			if (materials.keySet().containsAll(block.getStateDefinition().getPossibleStates())
				&& materials.values().stream().distinct().count() <= 1
			) {
				baked = new BlockMaterial.Simple(materials.get(block.defaultBlockState()));
			} else {
				baked = new BlockMaterial.MultiVariant(materials);
			}

			block.frozenLib$setAttached(BlockMaterial.ATTACHMENT_KEY, baked);
		});

		if (!shaders.isEmpty()) MaterialShaderPatcher.describe().forEach(LOGGER::info);
		rebuildChunks();

		LOGGER.info("Glowtone feature flags: shaders={} targets={}", !shaders.isEmpty(), targets);
		LOGGER.info("Glowtone applied block materials: {} blockstates across {} blocks, {} shader materials", overrides.size(), perBlock.size(), shaders.size());
	}

	// The material index is baked into chunk meshes, so terrain only picks up a change once they rebuild.
	private static void rebuildChunks() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;

		minecraft.execute(() -> {
			if (minecraft.level != null) minecraft.levelExtractor.allChanged();
		});
	}

	private static MaterialShader withParameterOverrides(Identifier materialId, MaterialShader shader, Map<String, String> overrides) {
		if (overrides.isEmpty()) return shader;

		final Map<String, String> merged = new LinkedHashMap<>(shader.parameters());
		overrides.forEach((name, value) -> {
			if (merged.containsKey(name)) {
				merged.put(name, value);
				return;
			}

			LOGGER.error("A block assigns parameter '{}' to block material {}, which does not declare it; ignoring that value",
				name, materialId);
		});

		return new MaterialShader(
			shader.fragment(), shader.vertex(), shader.textures(), shader.constants(), merged, shader.blockTextures()
		);
	}

	private record ShaderKey(Identifier material, Map<String, String> parameters) {
		String sortOrder() {
			return this.material + " " + new java.util.TreeMap<>(this.parameters);
		}
	}

	private static int allocateShader(
		ShaderKey key,
		BlockMaterial material,
		Map<Identifier, String> sources,
		List<MaterialShaderPatcher.Loaded> shaders,
		List<Identifier> samplerSlots
	) {
		final Identifier materialId = key.material();
		final MaterialShader declared = material.shader().orElse(null);
		if (declared == null || declared.isEmpty()) return BlockMaterialRenderer.NO_SHADER;

		final MaterialShader shader = withParameterOverrides(materialId, declared, key.parameters());

		final String fragmentSource = checkedStage(materialId, "fragment", shader.fragment(), sources);
		final String vertexSource = checkedStage(materialId, "vertex", shader.vertex(), sources);
		if (fragmentSource == null && vertexSource == null) return BlockMaterialRenderer.NO_SHADER;

		if (shaders.size() >= BlockMaterialRenderer.MAX_SHADER_INDEX) {
			LOGGER.error("Block material {} exceeds the limit of {} shader materials, ignoring its shader", materialId, BlockMaterialRenderer.MAX_SHADER_INDEX);
			return BlockMaterialRenderer.NO_SHADER;
		}

		if (!namesAreLegal(materialId, shader)) return BlockMaterialRenderer.NO_SHADER;

		final List<Identifier> pending = new ArrayList<>();
		final Map<String, Integer> slots = new LinkedHashMap<>(shader.textures().size());
		for (Map.Entry<String, Identifier> texture : shader.textures().entrySet()) {
			int slot = samplerSlots.indexOf(texture.getValue());
			if (slot < 0) {
				final int pendingSlot = pending.indexOf(texture.getValue());
				if (pendingSlot >= 0) {
					slot = samplerSlots.size() + pendingSlot;
				} else {
					if (samplerSlots.size() + pending.size() >= MaterialSamplers.SLOTS) {
						LOGGER.error("Block material {} needs more than {} distinct shader textures, ignoring its shader", materialId, MaterialSamplers.SLOTS);
						return BlockMaterialRenderer.NO_SHADER;
					}

					pending.add(texture.getValue());
					slot = samplerSlots.size() + pending.size() - 1;
				}
			}

			slots.put(texture.getKey(), slot);
		}

		samplerSlots.addAll(pending);
		shaders.add(new MaterialShaderPatcher.Loaded(
			materialId, shader, fragmentSource, vertexSource, slots, blockTexturesFor(materialId, shader)
		));
		return shaders.size();
	}

	private static Map<String, BlockTextureSlots.Slot> blockTexturesFor(Identifier materialId, MaterialShader shader) {
		if (shader.blockTextures().isEmpty()) return Map.of();

		final Map<String, BlockTextureSlots.Slot> resolved = BlockTextureSlots.resolve(shader.blockTextures());
		if (resolved == null) {
			LOGGER.error("Block material {} wants block textures {} but no block model declares them all",
				materialId, shader.blockTextures());
			return Map.of();
		}

		return resolved;
	}

	private static boolean namesAreLegal(Identifier materialId, MaterialShader shader) {
		boolean legal = true;

		for (String texture : shader.textures().keySet()) {
			final String rejection = MaterialShaderNames.rejection(texture);
			if (rejection == null) continue;

			LOGGER.error("Block material {} declares texture '{}', which cannot be a sampler name because {}", materialId, texture, rejection);
			legal = false;
		}

		for (String constant : shader.constants().keySet()) {
			final String rejection = MaterialShaderNames.rejection(constant);
			if (rejection == null) continue;

			LOGGER.error("Block material {} declares constant '{}', which cannot be a #define name because {}", materialId, constant, rejection);
			legal = false;
		}

		for (String parameter : shader.parameters().keySet()) {
			final String rejection = MaterialShaderNames.rejection(parameter);
			if (rejection == null) continue;

			LOGGER.error("Block material {} declares parameter '{}', which cannot be an argument name because {}", materialId, parameter, rejection);
			legal = false;
		}

		for (String clash : shader.parameters().keySet()) {
			if (!shader.constants().containsKey(clash)) continue;

			LOGGER.error("Block material {} declares '{}' as both a constant and a parameter; the #define would shadow the argument", materialId, clash);
			legal = false;
		}

		if (!legal) LOGGER.error("Ignoring the shader on block material {} because of the names above", materialId);
		return legal;
	}

	@Nullable
	private static String checkedStage(Identifier materialId, String stage, Optional<Identifier> file, Map<Identifier, String> sources) {
		final String source = stageSource(materialId, stage, file, sources);
		if (source == null) return null;

		final String rejection = MaterialShaderNames.snippetRejection(source);
		if (rejection == null) return source;

		LOGGER.error("Block material {} has an unusable {} snippet ({}): {}", materialId, stage, file.orElseThrow(), rejection);
		return null;
	}

	@Nullable
	private static String stageSource(Identifier materialId, String stage, Optional<Identifier> file, Map<Identifier, String> sources) {
		final Identifier id = file.orElse(null);
		if (id == null) return null;

		final String source = sources.get(id);
		if (source == null) LOGGER.error("Block material {} wants a {} shader from {} but its source was not read, skipping that stage", materialId, stage, id);

		return source;
	}

	private BlockMaterialLoader() {}
}
