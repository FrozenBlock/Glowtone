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

package net.frozenblock.glowtone.lighting;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LightEngine;
import org.slf4j.Logger;

@ClientOnly
public final class LightingFactors {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Identifier SKY_LIGHT_FACTOR = glowtone("sky_light_factor");
	public static final Identifier SUN_ANGLE = glowtone("sun_angle");
	public static final Identifier MOON_PHASE = glowtone("moon_phase");
	public static final Identifier MOON_BRIGHTNESS = glowtone("moon_brightness");
	public static final Identifier STAR_BRIGHTNESS = glowtone("star_brightness");
	public static final Identifier RAIN = glowtone("rain");
	public static final Identifier THUNDER = glowtone("thunder");
	public static final Identifier CAMERA_SKY_LIGHT = glowtone("camera_sky_light");
	public static final Identifier CAMERA_BLOCK_LIGHT = glowtone("camera_block_light");
	public static final Identifier CAMERA_HEIGHT = glowtone("camera_height");
	public static final Identifier LIGHT_LEVEL = glowtone("light_level");

	private static final Map<Identifier, DoubleSupplier> SUPPLIERS = new ConcurrentHashMap<>();
	private static final Set<Identifier> UNKNOWN = ConcurrentHashMap.newKeySet();

	private final Object2FloatOpenHashMap<Identifier> sampled = new Object2FloatOpenHashMap<>();

	public LightingFactors() {
		this.sampled.defaultReturnValue(Float.NaN);
	}

	private static Identifier glowtone(String path) {
		return Identifier.fromNamespaceAndPath("glowtone", path);
	}

	public static void register(Identifier id, DoubleSupplier supplier) {
		SUPPLIERS.put(id, supplier);
		UNKNOWN.remove(id);
	}

	public static void unregister(Identifier id) {
		SUPPLIERS.remove(id);
	}

	public static boolean provided(Identifier id) {
		return SUPPLIERS.containsKey(id) || id.getNamespace().equals("glowtone");
	}

	void sample(ClientLevel level, Camera camera, float partialTick) {
		final EnvironmentAttributeProbe probe = camera.attributeProbe();
		final float skyLightFactor = probe.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTick);
		final float sunAngle = probe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTick);
		final MoonPhase moonPhase = probe.getValue(EnvironmentAttributes.MOON_PHASE, partialTick);
		final float starBrightness = probe.getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partialTick);

		this.sampled.put(SKY_LIGHT_FACTOR, skyLightFactor);
		this.sampled.put(SUN_ANGLE, ((sunAngle / 360F) % 1F + 1F) % 1F);
		this.sampled.put(MOON_PHASE, moonPhase.ordinal());
		this.sampled.put(MOON_BRIGHTNESS, DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonPhase.ordinal() % DimensionType.MOON_BRIGHTNESS_PER_PHASE.length]);
		this.sampled.put(STAR_BRIGHTNESS, starBrightness);
		this.sampled.put(RAIN, level.getRainLevel(partialTick));
		this.sampled.put(THUNDER, level.getThunderLevel(partialTick));
		this.sampled.put(CAMERA_SKY_LIGHT, level.getBrightness(LightLayer.SKY, camera.blockPosition()) / (float) LightEngine.MAX_LEVEL);
		this.sampled.put(CAMERA_BLOCK_LIGHT, level.getBrightness(LightLayer.BLOCK, camera.blockPosition()) / (float) LightEngine.MAX_LEVEL);
		this.sampled.put(CAMERA_HEIGHT, (float) camera.position().y);
	}

	void setLightLevel(float level) {
		this.sampled.put(LIGHT_LEVEL, level);
	}

	void clearLightLevel() {
		this.sampled.removeFloat(LIGHT_LEVEL);
	}

	public float get(Identifier id) {
		final float sampled = this.sampled.getFloat(id);
		if (!Float.isNaN(sampled)) return sampled;

		final DoubleSupplier supplier = SUPPLIERS.get(id);
		if (supplier != null) return (float) supplier.getAsDouble();

		if (UNKNOWN.add(id)) LOGGER.warn("A Glowtone lighting profile reads the factor '{}', which nothing provides; reading it as 0", id);
		return 0F;
	}
}
