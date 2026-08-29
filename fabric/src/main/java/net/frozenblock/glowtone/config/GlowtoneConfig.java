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
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionMode;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.frozenblock.glowtone.config.option.color.ColoredLightingMode;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.option.shade.ShadingMode;
import net.frozenblock.lib.platform.ModLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneConfig {
	public static final int DEFAULT_BLOOM = 25;
	public static final int DEFAULT_EDGE_HIGHLIGHT = 0;
	public static final int DEFAULT_OCCLUSION_STRENGTH = OcclusionStrengthOption.VANILLA;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String BLOOM_KEY = "bloom";
	private static final String SHADING_KEY = "shading";
	private static final String LEGACY_EMISSIVES_KEY = "emissives";
	private static final String EDGE_HIGHLIGHT_KEY = "edge_highlight";
	private static final String COLORED_LIGHTING_KEY = "colored_lighting";
	private static final String AMBIENT_OCCLUSION_KEY = "ambient_occlusion";
	private static final String OCCLUSION_SCALE_KEY = "occlusion_scale";
	private static final String LEGACY_OCCLUSION_DEPTH_KEY = "occlusion_strength";
	private static int bloom = DEFAULT_BLOOM;
	private static int edgeHighlight = DEFAULT_EDGE_HIGHLIGHT;
	private static boolean bloomEnabled = true;
	private static ShadingMode shading = ShadingMode.DEFAULT;
	private static ColoredLightingMode coloredLighting = ColoredLightingMode.SUBTLE;
	private static AmbientOcclusionMode ambientOcclusion = AmbientOcclusionMode.DEFAULT;
	private static int occlusionStrength = DEFAULT_OCCLUSION_STRENGTH;
	private static boolean loaded;

	public static int bloom() {
		if (!loaded) load();
		return bloom;
	}

	public static boolean bloomEnabled() {
		return bloomEnabled;
	}

	private static ShadingMode parseShadingMode(JsonObject json) {
		if (json.has(SHADING_KEY)) return ShadingMode.CODEC.byName(GsonHelper.getAsString(json, SHADING_KEY, ShadingMode.DEFAULT.getSerializedName()));

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

	public static ColoredLightingMode coloredLighting() {
		if (!loaded) load();
		return coloredLighting;
	}

	public static void setColoredLighting(ColoredLightingMode mode) {
		if (!loaded) load();
		if (coloredLighting == mode) return;

		coloredLighting = mode;
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
		return ModLoader.getConfigDir().resolve(GlowtoneConstants.MOD_ID + ".json");
	}

	private static void load() {
		loaded = true;
		final Path path = path();
		if (!Files.exists(path)) return;

		try (Reader reader = Files.newBufferedReader(path)) {
			final JsonObject json = GsonHelper.parse(reader);
			bloom = Mth.clamp(GsonHelper.getAsInt(json, BLOOM_KEY, DEFAULT_BLOOM), BloomOption.MIN, BloomOption.MAX);
			edgeHighlight = Mth.clamp(GsonHelper.getAsInt(json, EDGE_HIGHLIGHT_KEY, DEFAULT_EDGE_HIGHLIGHT), EdgeHighlightOption.MIN, EdgeHighlightOption.MAX);
			occlusionStrength = parseOcclusionStrength(json);
			ambientOcclusion = AmbientOcclusionMode.CODEC.byName(GsonHelper.getAsString(json, AMBIENT_OCCLUSION_KEY, AmbientOcclusionMode.DEFAULT.getSerializedName()));
			bloomEnabled = bloom > 0;
			shading = parseShadingMode(json);
			coloredLighting = parseColoredLightingMode(json);
		} catch (IOException | RuntimeException exception) {
			LOGGER.error("Failed to read {}", path, exception);
		}
	}

	private static int parseOcclusionStrength(JsonObject json) {
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

	private static ColoredLightingMode parseColoredLightingMode(JsonObject json) {
		final String name = GsonHelper.getAsString(json, COLORED_LIGHTING_KEY, ColoredLightingMode.SUBTLE.name());
		try {
			return ColoredLightingMode.valueOf(name.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return ColoredLightingMode.SUBTLE;
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
				json.addProperty(AMBIENT_OCCLUSION_KEY, ambientOcclusion.getSerializedName());
				json.addProperty(SHADING_KEY, shading.id());
				json.addProperty(COLORED_LIGHTING_KEY, coloredLighting.name().toLowerCase(java.util.Locale.ROOT));
				writer.write(json.toString());
			}
		} catch (IOException exception) {
			LOGGER.error("Failed to write {}", path, exception);
		}
	}

	private GlowtoneConfig() {}
}
