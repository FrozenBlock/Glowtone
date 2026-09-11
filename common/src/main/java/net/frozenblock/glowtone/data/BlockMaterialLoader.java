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

package net.frozenblock.glowtone.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.frozenblock.glowtone.config.pack.GlowtonePackApi;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.render.BlockModelChains;
import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.MaterialBlockTextures;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.frozenblock.glowtone.material.MaterialShaderNames;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.frozenblock.glowtone.config.GlowtoneReload;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ClientOnly
public final class BlockMaterialLoader {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final boolean GLOWTONE_TRACE_TARGETS = Boolean.getBoolean("glowtone.traceTargets");
	private static final FileToIdConverter MATERIAL_LISTER = FileToIdConverter.json(BlockMaterialRenderer.RESOURCE_PACK_DIRECTORY);

	private static final FileToIdConverter SHADER_LISTER = new FileToIdConverter(MaterialShader.RESOURCE_PACK_DIRECTORY, MaterialShader.FILE_SUFFIX);
	public record Definitions(Map<Identifier, BlockMaterial> materials, Map<Identifier, String> shaderSources) {}

	public static CompletableFuture<Definitions> load(ResourceManager manager, Executor executor) {
		return CompletableFuture.supplyAsync(() -> {
			final Map<Identifier, BlockMaterial> materials = resolve(read(manager));
			final Map<Identifier, String> sources = readShaderSources(manager, materials);
			LOGGER.info("Glowtone read {} block material definitions and {} material shaders", materials.size(), sources.size());
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

	private static BlockMaterialDefinition withPackSettings(BlockMaterialDefinition definition, String packId) {
		final Optional<MaterialShader> shader = definition.material().shader();
		if (shader.isEmpty()) return definition;

		final MaterialShader source = shader.get();
		final Map<String, String> constants = GlowtonePackApi.resolve(packId, source.constants());
		final Map<String, String> parameters = GlowtonePackApi.resolve(packId, source.parameters());
		if (constants == source.constants() && parameters == source.parameters()) return definition;

		final MaterialShader patched = new MaterialShader(
			source.fragment(), source.vertex(), source.textures(), constants,
			parameters, source.blockTextures()
		);
		final BlockMaterial material = definition.material();
		return new BlockMaterialDefinition(definition.parent(), new BlockMaterial(
			material.layer(), material.cull(), material.renderShape(),
			material.blockEntityRender(), Optional.of(patched), material.target()
		));
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

					definitions.merge(materialId, withPackSettings(definition, resource.sourcePackId()), BlockMaterialLoader::mergedOver);
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

	private static void allocateShaders(
		Collection<BlockMaterialOverrideDispatcher.Assignment> assignments,
		Definitions definitions,
		Map<Identifier, Integer> shaderIndices,
		List<MaterialShaderPatcher.Loaded> shaders,
		List<Identifier> samplerSlots
	) {
		final Map<Identifier, BlockMaterial> registry = definitions.materials();
		assignments.stream()
			.map(BlockMaterialOverrideDispatcher.Assignment::material)
			.distinct()
			.sorted(Comparator.comparing(Identifier::toString))
			.forEach(materialId -> {
				final BlockMaterial material = registry.get(materialId);
				if (material == null || material.isNone()) return;

				shaderIndices.computeIfAbsent(
					materialId, id -> allocateShader(id, material, definitions.shaderSources(), shaders, samplerSlots)
				);
			});
	}

	private static List<BlockMaterialOverrideDispatcher.Assignment> shaderCandidates(
		Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides, List<BlockMaterialModelRule> modelRules
	) {
		final List<BlockMaterialOverrideDispatcher.Assignment> candidates = new ArrayList<>(overrides.values());
		for (BlockMaterialModelRule rule : modelRules) candidates.add(rule.assignment());
		return candidates;
	}

	public static void applyShaderSource(
		Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides,
		List<BlockMaterialModelRule> modelRules,
		Definitions definitions
	) {
		final List<MaterialShaderPatcher.Loaded> shaders = new ArrayList<>();
		final List<Identifier> samplerSlots = new ArrayList<>();
		allocateShaders(shaderCandidates(overrides, modelRules), definitions, new HashMap<>(), shaders, samplerSlots);

		MaterialSamplers.apply(samplerSlots);
		MaterialShaderPatcher.apply(shaders);
	}

	private static Map<BlockState, List<BlockMaterialOverrideDispatcher.Assignment>> withModelRules(
		Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides, List<BlockMaterialModelRule> modelRules
	) {
		final Map<BlockState, List<BlockMaterialOverrideDispatcher.Assignment>> merged = new IdentityHashMap<>(overrides.size());
		final Set<Block> claimed = Collections.newSetFromMap(new IdentityHashMap<>());
		overrides.forEach((state, assignment) -> {
			merged.put(state, new ArrayList<>(List.of(assignment)));
			if (assignment.target().isEmpty()) claimed.add(state.getBlock());
		});
		if (modelRules.isEmpty()) return merged;

		int matched = 0;
		for (Map.Entry<BlockState, Set<Identifier>> entry : BlockModelChains.chains().entrySet()) {
			final BlockState state = entry.getKey();
			if (claimed.contains(state.getBlock())) continue;

			for (BlockMaterialModelRule rule : modelRules) {
				if (!rule.matches(entry.getValue())) continue;

				merged.computeIfAbsent(state, key -> new ArrayList<>(1)).add(rule.assignment());
				matched++;
				break;
			}
		}

		LOGGER.info("Glowtone model rules claimed {} blockstates", matched);
		return merged;
	}

	public static void apply(
		Map<BlockState, BlockMaterialOverrideDispatcher.Assignment> overrides,
		List<BlockMaterialModelRule> modelRules,
		Definitions definitions
	) {
		final List<BlockMaterialOverrideDispatcher.Assignment> candidates = shaderCandidates(overrides, modelRules);
		final Map<BlockState, List<BlockMaterialOverrideDispatcher.Assignment>> assigned = withModelRules(overrides, modelRules);
		final Map<Identifier, BlockMaterial> registry = definitions.materials();
		BuiltInRegistries.BLOCK.forEach(block -> block.glowtone$setMaterial(BlockMaterial.EMPTY));

		final Map<Block, Map<BlockState, BlockMaterial.Assigned>> perBlock = new IdentityHashMap<>();
		final Set<Identifier> missing = new HashSet<>();
		final Set<Identifier> unsupportedLayers = new HashSet<>();
		final Set<Identifier> exhausted = new HashSet<>();
		final Set<String> badParameters = new HashSet<>();
		final Map<UnresolvedSlot, Integer> unresolved = new LinkedHashMap<>();
		final Map<Identifier, Integer> shaderIndices = new HashMap<>();
		final List<MaterialShaderPatcher.Loaded> shaders = new ArrayList<>();
		final List<Identifier> samplerSlots = new ArrayList<>();
		final Map<VariantKey, Integer> variantIndices = new HashMap<>();
		final List<MaterialBlockTextures.Variant> variants = new ArrayList<>();
		boolean layers = false;
		boolean selfCulling = false;
		boolean castCulling = false;
		boolean renderShape = false;
		boolean blockEntity = false;
		boolean targets = false;

		allocateShaders(candidates, definitions, shaderIndices, shaders, samplerSlots);

		final List<Ordered> ordered = new ArrayList<>(assigned.size());
		assigned.forEach((state, assignments) -> ordered.add(new Ordered(state.toString(), state, assignments)));
		ordered.sort(Comparator.comparing(Ordered::key));

		for (Ordered entry : ordered) {
			final BlockState state = entry.state();
			BlockMaterial.Assigned primary = null;
			final List<BlockMaterial.Assigned> extras = new ArrayList<>(0);

			for (BlockMaterialOverrideDispatcher.Assignment assignment : entry.assignments()) {
				final Identifier materialId = assignment.material();
				final BlockMaterial material = registry.get(materialId);
				if (material == null) {
					if (missing.add(materialId)) LOGGER.warn("Block material {} was assigned but never defined, ignoring it", materialId);
					continue;
				}
				if (material.isNone()) continue;

				final int materialCase = shaderIndices.getOrDefault(materialId, BlockMaterialRenderer.NO_SHADER);
				int shaderIndex = BlockMaterialRenderer.NO_SHADER;
				if (materialCase != BlockMaterialRenderer.NO_SHADER) {
					final MaterialShaderPatcher.Loaded loaded = shaders.get(materialCase - 1);
					final List<BlockTextureSlots.Slot> rectangles = resolveSlots(state, materialId, loaded.blockTextures(), unresolved);
					final List<Float> parameters = parameterValues(materialId, loaded.shader(), assignment.parameters(), badParameters);
					final VariantKey variantKey = new VariantKey(materialCase, rectangles, parameters);
					final Integer existing = variantIndices.get(variantKey);
					if (existing != null) {
						shaderIndex = existing;
					} else if (variants.size() >= BlockMaterialRenderer.MAX_SHADER_INDEX) {
						if (exhausted.add(materialId)) {
							LOGGER.error("Block material {} pushes the loaded materials past {} variants; its remaining blockstates render without a shader",
								materialId, BlockMaterialRenderer.MAX_SHADER_INDEX);
						}
					} else {
						variants.add(new MaterialBlockTextures.Variant(materialCase, rectangles, toArray(parameters)));
						shaderIndex = variants.size();
						variantIndices.put(variantKey, shaderIndex);
					}
				}

				final List<String> targetSlots = assignment.target().isEmpty() ? material.target() : assignment.target();
				final List<BlockTextureSlots.Slot> targetRectangles = targetSlots.isEmpty()
					? List.of()
					: resolveTargets(state, materialId, targetSlots, unresolved);
				if (GLOWTONE_TRACE_TARGETS) {
					LOGGER.info("Glowtone target trace: {} -> {} shader={} slots={} rects={}",
						state, materialId, shaderIndex, targetSlots,
						targetRectangles.stream().map(slot -> String.valueOf(slot.sprite().contents().name())).toList());
				}

				final BlockMaterial.Assigned built = new BlockMaterial.Assigned(
					materialId, material, shaderIndex,
					targetSlots.isEmpty() ? null : targetSlots,
					targetRectangles,
					targetSlots.contains(BlockMaterialRenderer.EMISSIVE_TARGET)
				);
				if (primary == null && !built.targeted()) {
					primary = built;
				} else {
					extras.add(built);
				}
				targets |= !targetSlots.isEmpty();
			}

			if (primary == null) {
				if (extras.isEmpty()) continue;
				primary = extras.removeFirst();
			}
			for (BlockMaterial.Assigned extra : extras) primary = primary.withExtra(extra);

			final BlockMaterial material = primary.material();
			final Identifier chosen = primary.id();
			material.layer().filter(MaterialLayer::custom).ifPresent(layer -> {
				if (!unsupportedLayers.add(layer.id())) return;
				LOGGER.warn("Block material {} asks for layer {}, but only solid, cutout and translucent render; leaving it on its model's layer", chosen, layer.id());
			});

			layers |= material.overridesLayer();
			selfCulling |= material.cull().selfMode().decides();
			castCulling |= material.cull().castMode().decides();
			renderShape |= material.overridesRenderShape();
			blockEntity |= material.overridesBlockEntityRender();

			perBlock.computeIfAbsent(state.getBlock(), block -> new IdentityHashMap<>()).put(state, primary);
		}

		BlockMaterialRenderer.setLoadedFeatures(layers, selfCulling, castCulling, !shaders.isEmpty(), renderShape, blockEntity, targets);

		final Map<Integer, BlockMaterial.Assigned> byIndex = new HashMap<>();
		perBlock.values().forEach(states -> states.values().forEach(assigned1 -> {
			if (assigned1.shaderIndex() != BlockMaterialRenderer.NO_SHADER) byIndex.putIfAbsent(assigned1.shaderIndex(), assigned1);
			for (BlockMaterial.Assigned extra : assigned1.extra()) {
				if (extra.shaderIndex() != BlockMaterialRenderer.NO_SHADER) byIndex.putIfAbsent(extra.shaderIndex(), extra);
			}
		}));
		BlockMaterialRenderer.setAssignedByIndex(byIndex);

		final String previousShaderSource = MaterialShaderPatcher.generateFunctions(true);
		MaterialBlockTextures.apply(variants);
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

			block.glowtone$setMaterial(baked);
		});

		unresolved.forEach((slot, count) -> LOGGER.warn(
			"Block material {} names block texture slot '{}', but {} of its blockstates have no model texture with that name; they get an empty rectangle",
			slot.material(), slot.slot(), count
		));

		if (!shaders.isEmpty()) MaterialShaderPatcher.describe().forEach(LOGGER::info);
		rebuildChunks();

		LOGGER.info("Glowtone feature flags: shaders={} targets={}", !shaders.isEmpty(), targets);
		LOGGER.info("Glowtone applied block materials: {} blockstates across {} blocks, {} shader materials in {} variants",
			assigned.size(), perBlock.size(), shaders.size(), variants.size());
	}

	private static List<BlockTextureSlots.Slot> resolveSlots(
		BlockState state, Identifier materialId, List<String> names, Map<UnresolvedSlot, Integer> unresolved
	) {
		final List<BlockTextureSlots.Slot> slots = new ArrayList<>(names.size());
		for (String name : names.stream().sorted().toList()) {
			final BlockTextureSlots.Slot slot = BlockTextureSlots.resolve(state, name);
			if (slot == null) unresolved.merge(new UnresolvedSlot(materialId, name), 1, Integer::sum);
			slots.add(slot);
		}

		return Collections.unmodifiableList(slots);
	}

	private static List<BlockTextureSlots.Slot> resolveTargets(
		BlockState state, Identifier materialId, List<String> names, Map<UnresolvedSlot, Integer> unresolved
	) {
		final List<BlockTextureSlots.Slot> slots = new ArrayList<>(names.size());
		for (String name : names) {
			if (name.equals(BlockMaterialRenderer.EMISSIVE_TARGET)) continue;

			final BlockTextureSlots.Slot slot = BlockTextureSlots.resolve(state, name);
			if (slot == null) {
				unresolved.merge(new UnresolvedSlot(materialId, name), 1, Integer::sum);
				continue;
			}

			slots.add(slot);
			final BlockTextureSlots.Slot overlay = BlockTextureSlots.overlayOf(slot);
			if (overlay != null) slots.add(overlay);
		}

		return List.copyOf(slots);
	}

	// The material index is baked into chunk meshes, so terrain only picks up a change once they rebuild.
	private static void rebuildChunks() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;

		minecraft.execute(() -> {
			if (minecraft.level != null) minecraft.levelExtractor.allChanged();
		});
	}

	private static List<Float> parameterValues(
		Identifier materialId, MaterialShader shader, Map<String, String> overrides, Set<String> reported
	) {
		final List<Float> values = new ArrayList<>(shader.parameters().size());
		for (Map.Entry<String, String> parameter : new TreeMap<>(shader.parameters()).entrySet()) {
			final String value = overrides.getOrDefault(parameter.getKey(), parameter.getValue());
			values.add(parseParameter(materialId, parameter.getKey(), value, reported));
		}

		for (String name : overrides.keySet()) {
			if (shader.parameters().containsKey(name) || !reported.add(materialId + " " + name)) continue;

			LOGGER.error("A block assigns parameter '{}' to block material {}, which does not declare it; ignoring that value", name, materialId);
		}

		return List.copyOf(values);
	}

	private static float parseParameter(Identifier materialId, String name, String value, Set<String> reported) {
		final Float number = parseNumber(value);
		if (number != null) return number;

		if (reported.add(materialId + " " + name + "=" + value)) {
			LOGGER.error("Block material {} gives parameter '{}' the value '{}', which is not a number; reading it as 0", materialId, name, value);
		}
		return 0F;
	}

	@Nullable
	private static Float parseNumber(String value) {
		try {
			final float number = Float.parseFloat(value.trim());
			return Float.isFinite(number) ? number : null;
		} catch (NumberFormatException notANumber) {
			return null;
		}
	}

	private static float[] toArray(List<Float> values) {
		final float[] array = new float[values.size()];
		for (int index = 0; index < array.length; index++) array[index] = values.get(index);
		return array;
	}

	private record Ordered(String key, BlockState state, List<BlockMaterialOverrideDispatcher.Assignment> assignments) {}

	private record VariantKey(int materialCase, List<BlockTextureSlots.Slot> rectangles, List<Float> parameters) {}

	private record UnresolvedSlot(Identifier material, String slot) {}

	private static int allocateShader(
		Identifier materialId,
		BlockMaterial material,
		Map<Identifier, String> sources,
		List<MaterialShaderPatcher.Loaded> shaders,
		List<Identifier> samplerSlots
	) {
		final MaterialShader shader = material.shader().orElse(null);
		if (shader == null || shader.isEmpty()) return BlockMaterialRenderer.NO_SHADER;

		final String fragmentSource = checkedStage(materialId, "fragment", shader.fragment(), sources);
		final String vertexSource = checkedStage(materialId, "vertex", shader.vertex(), sources);
		if (fragmentSource == null && vertexSource == null) return BlockMaterialRenderer.NO_SHADER;

		if (shaders.size() >= BlockMaterialRenderer.MAX_MATERIALS) {
			LOGGER.error("Block material {} exceeds the limit of {} shader materials, ignoring its shader", materialId, BlockMaterialRenderer.MAX_MATERIALS);
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
			materialId, shader, fragmentSource, vertexSource, slots, List.copyOf(shader.blockTextures())
		));
		return shaders.size();
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

		for (String blockTexture : shader.blockTextures()) {
			final String rejection = MaterialShaderNames.rejection(blockTexture);
			if (rejection == null) continue;

			LOGGER.error("Block material {} declares block texture '{}', which cannot be an argument name because {}", materialId, blockTexture, rejection);
			legal = false;
		}

		for (Map.Entry<String, String> constant : shader.constants().entrySet()) {
			final String rejection = MaterialShaderNames.valueRejection(constant.getValue());
			if (rejection == null) continue;

			LOGGER.error("Block material {} gives constant '{}' a value that cannot be pasted into GLSL because {}", materialId, constant.getKey(), rejection);
			legal = false;
		}

		for (Map.Entry<String, String> parameter : shader.parameters().entrySet()) {
			if (parseNumber(parameter.getValue()) != null) continue;

			LOGGER.error("Block material {} gives parameter '{}' the default '{}', which is not a number", materialId, parameter.getKey(), parameter.getValue());
			legal = false;
		}

		for (String clash : shader.parameters().keySet()) {
			if (!shader.constants().containsKey(clash)) continue;

			LOGGER.error("Block material {} declares '{}' as both a constant and a parameter; the #define would shadow the argument", materialId, clash);
			legal = false;
		}

		for (String clash : shader.blockTextures()) {
			if (!shader.parameters().containsKey(clash) && !shader.constants().containsKey(clash) && !shader.textures().containsKey(clash)) continue;

			LOGGER.error("Block material {} declares '{}' as a block texture and as another shader name; the arguments would collide", materialId, clash);
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
