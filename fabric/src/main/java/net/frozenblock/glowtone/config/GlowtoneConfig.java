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

package net.frozenblock.glowtone.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public final class GlowtoneConfig {
	public static final int DEFAULT_BLOOM = 25;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String BLOOM_KEY = "bloom";
	private static final String EMISSIVES_KEY = "emissives";
	private static int bloom = DEFAULT_BLOOM;
	private static EmissivesMode emissives = EmissivesMode.DEFAULT;
	private static boolean loaded;

	private GlowtoneConfig() {}

	public static int bloom() {
		if (!loaded) load();
		return bloom;
	}

	public static EmissivesMode emissives() {
		if (!loaded) load();
		return emissives;
	}

	public static void setEmissives(EmissivesMode mode) {
		if (!loaded) load();
		if (emissives == mode) return;

		emissives = mode;
		save();
	}

	public static void setBloom(int value) {
		if (!loaded) load();

		final int clamped = Mth.clamp(value, BloomOption.MIN, BloomOption.MAX);
		if (clamped == bloom) return;

		bloom = clamped;
		save();
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(GlowtoneConstants.MOD_ID + ".json");
	}

	private static void load() {
		loaded = true;
		final Path path = path();
		if (!Files.exists(path)) return;

		try (Reader reader = Files.newBufferedReader(path)) {
			final JsonObject json = GsonHelper.parse(reader);
			bloom = Mth.clamp(GsonHelper.getAsInt(json, BLOOM_KEY, DEFAULT_BLOOM), BloomOption.MIN, BloomOption.MAX);
			emissives = EmissivesMode.byId(GsonHelper.getAsString(json, EMISSIVES_KEY, EmissivesMode.DEFAULT.id()));
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to read {}", path, exception);
		}
	}

	private static void save() {
		final Path path = path();

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				final JsonObject json = new JsonObject();
				json.addProperty(BLOOM_KEY, bloom);
				json.addProperty(EMISSIVES_KEY, emissives.id());
				writer.write(json.toString());
			}
		} catch (IOException exception) {
			LOGGER.error("Failed to write {}", path, exception);
		}
	}
}
