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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Environment(EnvType.CLIENT)
public final class GlowtonePackSettingsLoader implements PreparableReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final FileToIdConverter LISTER = FileToIdConverter.json("glowtone");
	private static final String SETTINGS = "settings";
	private static final String HIGHLIGHT = "highlight";
	private static final String WATER = "water_highlight";
	private static final String BLOOM = "bloom";
	private static final Set<String> SECTIONS = Set.of(HIGHLIGHT, WATER, BLOOM);

	@Override
	public CompletableFuture<Void> reload(
		SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor
	) {
		final ResourceManager manager = currentReload.resourceManager();

		return CompletableFuture
			.supplyAsync(() -> load(manager), taskExecutor)
			.thenCompose(preparationBarrier::wait)
			.thenAcceptAsync(settings -> {
				if (GlowtonePackSettings.apply(settings)) GlowtoneReload.request();
			}, reloadExecutor);
	}

	private static GlowtonePackSettings load(ResourceManager manager) {
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

		if (loaded) LOGGER.info("Glowtone pack settings in use: {}", merged.describe());

		return merged;
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
			section(object, BLOOM, GlowtonePackSettings.Bloom.CODEC, GlowtonePackSettings.Bloom.NONE, packId)
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
