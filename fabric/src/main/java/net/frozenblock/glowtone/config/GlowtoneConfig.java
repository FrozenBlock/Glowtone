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
	public static final int DEFAULT_EDGE_HIGHLIGHT = 0;
	public static final int DEFAULT_OCCLUSION_STRENGTH = OcclusionStrengthOption.VANILLA;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String BLOOM_KEY = "bloom";
	private static final String SHADING_KEY = "shading";
	private static final String LEGACY_EMISSIVES_KEY = "emissives";
	private static final String EDGE_HIGHLIGHT_KEY = "edge_highlight";
	private static final String COLOURED_LIGHTING_KEY = "coloured_lighting";
	private static final String AMBIENT_OCCLUSION_KEY = "ambient_occlusion";
	private static final String OCCLUSION_SCALE_KEY = "occlusion_scale";
	private static final String LEGACY_OCCLUSION_DEPTH_KEY = "occlusion_strength";
	private static int bloom = DEFAULT_BLOOM;
	private static int edgeHighlight = DEFAULT_EDGE_HIGHLIGHT;
	private static boolean bloomEnabled = true;
	private static ShadingMode shading = ShadingMode.DEFAULT;
	private static ColouredLightingMode colouredLighting = ColouredLightingMode.SUBTLE;
	private static AmbientOcclusionMode ambientOcclusion = AmbientOcclusionMode.DEFAULT;
	private static int occlusionStrength = DEFAULT_OCCLUSION_STRENGTH;
	private static boolean loaded;

	private GlowtoneConfig() {}

	public static int bloom() {
		if (!loaded) load();
		return bloom;
	}

	public static boolean bloomEnabled() {
		return bloomEnabled;
	}

	private static ShadingMode readShading(JsonObject json) {
		if (json.has(SHADING_KEY)) {
			return ShadingMode.byId(GsonHelper.getAsString(json, SHADING_KEY, ShadingMode.DEFAULT.id()));
		}
		if (json.has(LEGACY_EMISSIVES_KEY)) {
			return "shaded".equals(GsonHelper.getAsString(json, LEGACY_EMISSIVES_KEY, ""))
				? ShadingMode.ALL : ShadingMode.NON_EMISSIVE;
		}
		return ShadingMode.DEFAULT;
	}

	public static ShadingMode shading() {
		if (!loaded) load();
		return shading;
	}

	public static ColouredLightingMode colouredLighting() {
		if (!loaded) load();
		return colouredLighting;
	}

	public static void setColouredLighting(ColouredLightingMode mode) {
		if (!loaded) load();
		if (colouredLighting == mode) return;

		colouredLighting = mode;
		save();
	}

	public static void setShading(ShadingMode mode) {
		if (!loaded) load();
		if (shading == mode) return;

		shading = mode;
		save();
	}

	public static AmbientOcclusionMode ambientOcclusion() {
		if (!loaded) load();
		return ambientOcclusion;
	}

	public static void setAmbientOcclusion(AmbientOcclusionMode mode) {
		if (!loaded) load();
		if (ambientOcclusion == mode) return;

		ambientOcclusion = mode;
		save();
	}

	public static int occlusionStrength() {
		if (!loaded) load();
		return occlusionStrength;
	}

	public static void setOcclusionStrength(int value) {
		if (!loaded) load();

		final int clamped = clampOcclusion(value);
		if (clamped == occlusionStrength) return;

		occlusionStrength = clamped;
		save();
	}

	public static int edgeHighlight() {
		if (!loaded) load();
		return edgeHighlight;
	}

	public static void setEdgeHighlight(int value) {
		if (!loaded) load();

		final int clamped = Mth.clamp(value, EdgeHighlightOption.MIN, EdgeHighlightOption.MAX);
		if (clamped == edgeHighlight) return;

		edgeHighlight = clamped;
		save();
	}

	public static void setBloom(int value) {
		if (!loaded) load();

		final int clamped = Mth.clamp(value, BloomOption.MIN, BloomOption.MAX);
		if (clamped == bloom) return;

		bloom = clamped;
		bloomEnabled = bloom > 0;
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
			edgeHighlight = Mth.clamp(GsonHelper.getAsInt(json, EDGE_HIGHLIGHT_KEY, DEFAULT_EDGE_HIGHLIGHT), EdgeHighlightOption.MIN, EdgeHighlightOption.MAX);
			occlusionStrength = readOcclusionStrength(json);
			ambientOcclusion = AmbientOcclusionMode.byId(GsonHelper.getAsString(json, AMBIENT_OCCLUSION_KEY, AmbientOcclusionMode.DEFAULT.id()));
			bloomEnabled = bloom > 0;
			shading = readShading(json);
			colouredLighting = readColouredLighting(json);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to read {}", path, exception);
		}
	}

	private static int readOcclusionStrength(JsonObject json) {
		if (json.has(OCCLUSION_SCALE_KEY)) {
			return clampOcclusion(GsonHelper.getAsInt(json, OCCLUSION_SCALE_KEY, DEFAULT_OCCLUSION_STRENGTH));
		}
		if (!json.has(LEGACY_OCCLUSION_DEPTH_KEY)) return DEFAULT_OCCLUSION_STRENGTH;

		final float depth = GsonHelper.getAsInt(json, LEGACY_OCCLUSION_DEPTH_KEY, DEFAULT_OCCLUSION_STRENGTH)
			/ (float) OcclusionStrengthOption.MAX;
		return clampOcclusion(Math.round(depth / OcclusionStrengthOption.VANILLA_DEPTH * OcclusionStrengthOption.VANILLA));
	}

	private static int clampOcclusion(int value) {
		return Mth.clamp(value, OcclusionStrengthOption.MIN, OcclusionStrengthOption.MAX);
	}

	private static ColouredLightingMode readColouredLighting(JsonObject json) {
		final String name = GsonHelper.getAsString(json, COLOURED_LIGHTING_KEY, ColouredLightingMode.SUBTLE.name());
		try {
			return ColouredLightingMode.valueOf(name.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return ColouredLightingMode.SUBTLE;
		}
	}

	private static void save() {
		final Path path = path();

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				final JsonObject json = new JsonObject();
				json.addProperty(BLOOM_KEY, bloom);
			json.addProperty(EDGE_HIGHLIGHT_KEY, edgeHighlight);
			json.addProperty(OCCLUSION_SCALE_KEY, occlusionStrength);
			json.addProperty(AMBIENT_OCCLUSION_KEY, ambientOcclusion.id());
				json.addProperty(SHADING_KEY, shading.id());
			json.addProperty(COLOURED_LIGHTING_KEY, colouredLighting.name().toLowerCase(java.util.Locale.ROOT));
				writer.write(json.toString());
			}
		} catch (IOException exception) {
			LOGGER.error("Failed to write {}", path, exception);
		}
	}
}
