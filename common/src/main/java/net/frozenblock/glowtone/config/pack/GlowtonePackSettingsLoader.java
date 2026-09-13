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

package net.frozenblock.glowtone.config.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.frozenblock.glowtone.lighting.GlowtoneLighting;
import net.frozenblock.glowtone.lighting.LightingAssignment;
import net.frozenblock.glowtone.lighting.LightingProfile;
import net.frozenblock.glowtone.lighting.LightingSettings;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GlowtonePackSettingsLoader implements PreparableReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("glowtone");
	private static final FileToIdConverter PROFILES = FileToIdConverter.json("glowtone/lighting_profiles");
	private static final FileToIdConverter DIMENSIONS = FileToIdConverter.json("glowtone/lighting_dimensions");
	private static final FileToIdConverter BIOMES = FileToIdConverter.json("glowtone/lighting_biomes");
	private static final String SETTINGS = "settings";
	private static final String HIGHLIGHT = "highlight";
	private static final String WATER = "water_highlight";
	private static final String BLOOM = "bloom";
	private static final String LIGHTING = "lighting";
	private static final Set<String> SECTIONS = Set.of(HIGHLIGHT, WATER, BLOOM, LIGHTING);

	private record Loaded(
		GlowtonePackSettings settings,
		Map<Identifier, LightingProfile> profiles,
		Map<Identifier, LightingAssignment> dimensions,
		Map<Identifier, LightingAssignment> biomes
	) {
		void applyLighting() {
			GlowtoneLighting.load(this.profiles, this.dimensions, this.biomes, this.settings.lighting());
		}
	}

	public static void applyFrom(ResourceManager manager) {
		final Loaded loaded = load(manager);
		GlowtonePackSettings.apply(loaded.settings());
		loaded.applyLighting();
	}

	@Override
	public CompletableFuture<Void> reload(
		SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor
	) {
		final ResourceManager manager = currentReload.resourceManager();

		return CompletableFuture
			.supplyAsync(() -> load(manager), taskExecutor)
			.thenCompose(preparationBarrier::wait)
			.thenAcceptAsync(loaded -> {
				GlowtonePackOptions.afterReload();
				if (GlowtonePackSettings.apply(loaded.settings())) GlowtoneReload.request();
				loaded.applyLighting();
			}, reloadExecutor);
	}

	private static Loaded load(ResourceManager manager) {
		GlowtonePackSettings merged = GlowtonePackSettings.NONE;
		boolean loaded = false;

		for (Map.Entry<Identifier, List<Resource>> entry : LISTER.listMatchingResourceStacks(manager).entrySet()) {
			if (!LISTER.fileToId(entry.getKey()).getPath().equals(SETTINGS)) continue;

			for (Resource resource : entry.getValue()) {
				try (Reader reader = resource.openAsReader()) {
					merged = parse(StrictJsonParser.parse(reader), resource.sourcePackId()).mergedOver(merged);
					loaded = true;
				} catch (JsonParseException e) {
					LOGGER.error(
						"Glowtone settings in pack {} are not valid JSON, so NONE of them are being used: {}",
						resource.sourcePackId(), rootMessage(e)
					);
				} catch (Exception e) {
					LOGGER.error("Failed to read Glowtone settings from pack {}", resource.sourcePackId(), e);
				}
			}
		}

		final Map<Identifier, LightingProfile> profiles = stack(manager, PROFILES, LightingProfile.CODEC, "lighting profile", LightingProfile::mergedOver);
		final Map<Identifier, LightingAssignment> dimensions = stack(manager, DIMENSIONS, LightingAssignment.CODEC, "lighting dimension", LightingAssignment::mergedOver);
		final Map<Identifier, LightingAssignment> biomes = stack(manager, BIOMES, LightingAssignment.CODEC, "lighting biome", LightingAssignment::mergedOver);

		if (loaded) LOGGER.info("Glowtone pack settings in use: {}", merged.describe());
		if (!profiles.isEmpty() || !dimensions.isEmpty() || !biomes.isEmpty()) {
			LOGGER.info(
				"Glowtone read {} lighting profiles, {} dimension files and {} biome files",
				profiles.size(), dimensions.size(), biomes.size()
			);
		}

		return new Loaded(merged, profiles, dimensions, biomes);
	}

	private static <T> Map<Identifier, T> stack(
		ResourceManager manager, FileToIdConverter lister, Codec<T> codec, String kind, BinaryOperator<T> merge
	) {
		final Map<Identifier, T> values = new LinkedHashMap<>();

		for (Map.Entry<Identifier, List<Resource>> entry : lister.listMatchingResourceStacks(manager).entrySet()) {
			final Identifier id = lister.fileToId(entry.getKey());
			for (Resource resource : entry.getValue()) {
				final T value = parseOne(resource, codec, kind, id);
				if (value == null) continue;

				final T below = values.get(id);
				values.put(id, below == null ? value : merge.apply(value, below));
			}
		}

		return Map.copyOf(values);
	}

	private static <T> @Nullable T parseOne(Resource resource, Codec<T> codec, String kind, Identifier id) {
		try (Reader reader = resource.openAsReader()) {
			return codec.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader))
				.resultOrPartial(error -> LOGGER.error(
					"Glowtone {} {} in pack {} is unusable and is being skipped: {}", kind, id, resource.sourcePackId(), error
				))
				.orElse(null);
		} catch (JsonParseException e) {
			LOGGER.error("Glowtone {} {} in pack {} is not valid JSON: {}", kind, id, resource.sourcePackId(), rootMessage(e));
		} catch (Exception e) {
			LOGGER.error("Failed to read Glowtone {} {} from pack {}", kind, id, resource.sourcePackId(), e);
		}
		return null;
	}

	private static String rootMessage(Throwable error) {
		Throwable root = error;
		while (root.getCause() != null) root = root.getCause();
		return root.getMessage();
	}

	private static GlowtonePackSettings parse(JsonElement json, String packId) {
		if (!(json instanceof JsonObject object)) {
			LOGGER.error("Glowtone settings in pack {} are not a JSON object, ignoring them", packId);
			return GlowtonePackSettings.NONE;
		}

		for (String key : object.keySet()) {
			if (!SECTIONS.contains(key)) {
				LOGGER.warn("Glowtone settings in pack {} have an unknown \"{}\" section, expected one of {}", packId, key, SECTIONS);
			}
		}

		return new GlowtonePackSettings(
			section(object, HIGHLIGHT, GlowtonePackSettings.Highlight.CODEC, GlowtonePackSettings.Highlight.NONE, packId),
			section(object, WATER, GlowtonePackSettings.Water.CODEC, GlowtonePackSettings.Water.NONE, packId),
			section(object, BLOOM, GlowtonePackSettings.Bloom.CODEC, GlowtonePackSettings.Bloom.NONE, packId),
			section(object, LIGHTING, LightingSettings.CODEC, LightingSettings.NONE, packId)
		);
	}

	private static <T> T section(JsonObject object, String name, Codec<T> codec, T fallback, String packId) {
		final JsonElement element = object.get(name);
		if (element == null) return fallback;

		return codec.parse(JsonOps.INSTANCE, element)
			.resultOrPartial(error -> LOGGER.error(
				"Glowtone settings in pack {} have an unusable \"{}\" section, falling back to defaults for it: {}",
				packId, name, error
			))
			.orElse(fallback);
	}
}
