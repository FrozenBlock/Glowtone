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

package net.frozenblock.glowtone.light.color;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GlowtoneColorResources {
	private static final Logger LOGGER = LoggerFactory.getLogger("Glowtone|Colours");

	public static final Identifier RELOAD_ID = GlowtoneConstants.id("coloured_lighting/colours");

	public static final Identifier EMITTERS_RESOURCE = GlowtoneConstants.id("coloured_lighting/emitters.json");

	public static final Identifier TRANSMITTANCE_RESOURCE = GlowtoneConstants.id("coloured_lighting/transmittance.json");

	private static final String REPLACE_KEY = "replace";
	private static final String[] EMITTER_ENTRY_KEYS = {"colours", "colors"};
	private static final String[] TRANSMITTANCE_ENTRY_KEYS = {"filters", "transmittance"};

	private static final int UNKNOWN_ID_SAMPLE = 8;

	private GlowtoneColorResources() {
		throw new UnsupportedOperationException("GlowtoneColorResources is a static holder.");
	}

	public static PreparableReloadListener reloadListener() {
		return new Listener();
	}

	private record Payload(Overlay emitters, Overlay transmittance) {}

	private record Overlay(Reference2IntMap<Block> values, boolean replace) {}

	@FunctionalInterface
	private interface ValueParser {
		int parse(String raw);
	}

	private static Payload read(ResourceManager resourceManager) {
		return new Payload(
			read(
				resourceManager, EMITTERS_RESOURCE, EMITTER_ENTRY_KEYS,
				GlowtoneColorResources::parseRgb, GlowtoneEmitterColors.NO_COLOUR
			),
			read(
				resourceManager, TRANSMITTANCE_RESOURCE, TRANSMITTANCE_ENTRY_KEYS,
				GlowtoneColorResources::parseFilter, GlowtoneTransmittance.FULLY_TRANSMISSIVE
			)
		);
	}

	private static void apply(Payload payload) {
		GlowtoneEmitterColors.applyOverlay(payload.emitters().values(), payload.emitters().replace());
		GlowtoneEmitterColors.attachAndClearColors();

		GlowtoneTransmittance.applyOverlay(payload.transmittance().values(), payload.transmittance().replace());
		GlowtoneTransmittance.attachAndClearColors();

		LOGGER.info(
			"Coloured lighting data ready: {} emitter colours, {} transmittance filters.",
			GlowtoneEmitterColors.definedCount(), GlowtoneTransmittance.definedCount()
		);
	}

	private static Overlay read(ResourceManager resourceManager, Identifier path, String[] entryKeys, ValueParser parser, int noneValue) {
		final var values = new Reference2IntOpenHashMap<Block>();
		final var unknownIds = new ArrayList<String>();
		boolean replace = false;

		for (var resource : resourceManager.getResourceStack(path)) {
			final String packId = resource.sourcePackId();

			try (var reader = resource.openAsReader()) {
				final var root = JsonParser.parseReader(reader);

				if (!root.isJsonObject()) {
					LOGGER.warn("Ignoring \"{}\" from pack \"{}\": expected a JSON object.", path, packId);
					continue;
				}

				final var json = root.getAsJsonObject();

				if (json.has(REPLACE_KEY) && json.get(REPLACE_KEY).getAsBoolean()) {
					values.clear();
					unknownIds.clear();
					replace = true;
				}

				final var entries = findEntries(json, entryKeys);

				if (entries == null) {
					LOGGER.warn("Ignoring \"{}\" from pack \"{}\": no \"{}\" object.", path, packId, entryKeys[0]);
					continue;
				}

				readEntries(entries, values, unknownIds, parser, noneValue, path, packId);
			} catch (Exception e) {
				LOGGER.warn("Failed to read \"{}\" from pack \"{}\".", path, packId, e);
			}
		}

		if (!unknownIds.isEmpty()) reportUnknownIds(path, unknownIds);

		return new Overlay(values, replace);
	}

	private static void readEntries(
		JsonObject entries,
		Reference2IntMap<Block> values,
		List<String> unknownIds,
		ValueParser parser,
		int noneValue,
		Identifier path,
		String packId
	) {
		for (var entry : entries.entrySet()) {
			final String rawId = entry.getKey();
			final var id = Identifier.tryParse(rawId);

			if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
				unknownIds.add(rawId);
				continue;
			}

			int value;

			try {
				value = parseValue(entry.getValue(), parser, noneValue);
			} catch (Exception e) {
				LOGGER.warn("Ignoring entry \"{}\" of \"{}\" from pack \"{}\": {}", rawId, path, packId, e.getMessage());
				continue;
			}

			values.put(BuiltInRegistries.BLOCK.getValue(id), value);
		}
	}

	private static int parseValue(JsonElement element, ValueParser parser, int noneValue) {
		if (element.isJsonNull()) return noneValue;

		final var primitive = element.getAsJsonPrimitive();
		if (primitive.isNumber()) return primitive.getAsInt();

		final String raw = primitive.getAsString().trim();
		if (raw.isEmpty() || raw.equalsIgnoreCase("none")) return noneValue;

		return parser.parse(raw);
	}

	private static @Nullable JsonObject findEntries(JsonObject json, String[] keys) {
		for (var key : keys) {
			if (json.has(key) && json.get(key).isJsonObject()) return json.getAsJsonObject(key);
		}

		return null;
	}

	private static void reportUnknownIds(Identifier path, List<String> unknownIds) {
		var sample = String.join(", ", unknownIds.subList(0, Math.min(UNKNOWN_ID_SAMPLE, unknownIds.size())));
		if (unknownIds.size() > UNKNOWN_ID_SAMPLE) sample += ", …";

		LOGGER.info("Skipped {} unknown block id(s) in \"{}\": {}", unknownIds.size(), path, sample);
	}

	private static String hexDigits(String raw) {
		if (raw.startsWith("#")) return raw.substring(1);
		if (raw.startsWith("0x") || raw.startsWith("0X")) return raw.substring(2);
		return raw;
	}

	private static int parseRgb(String raw) {
		final String digits = hexDigits(raw);

		return switch (digits.length()) {
			case 3 -> {
				final int packed = Integer.parseUnsignedInt(digits, 16);
				yield (((packed >> 8) & 0xF) * 0x11) << 16
					| (((packed >> 4) & 0xF) * 0x11) << 8
					| ((packed & 0xF) * 0x11);
			}
			case 6 -> Integer.parseUnsignedInt(digits, 16);
			default -> throw new IllegalArgumentException("expected \"#RGB\" or \"#RRGGBB\", got \"" + raw + "\"");
		};
	}

	private static int parseFilter(String raw) {
		final String digits = hexDigits(raw);

		return switch (digits.length()) {
			case 3 -> Integer.parseUnsignedInt(digits, 16) & 0xFFF;
			case 6 -> {
				final int packed = Integer.parseUnsignedInt(digits, 16);
				yield ((packed >> 20) & 0xF) << 8 | ((packed >> 12) & 0xF) << 4 | ((packed >> 4) & 0xF);
			}
			default -> throw new IllegalArgumentException("expected \"#RGB\" or \"#RRGGBB\", got \"" + raw + "\"");
		};
	}

	private static final class Listener implements PreparableReloadListener {
		@Override
		public String getName() {
			return RELOAD_ID.toString();
		}

		@Override
		public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor, PreparationBarrier synchronizer, Executor applyExecutor) {
			return CompletableFuture.supplyAsync(() -> read(sharedState.resourceManager()), prepareExecutor)
					.thenCompose(synchronizer::wait)
					.thenAcceptAsync(GlowtoneColorResources::apply, applyExecutor);
		}
	}
}
