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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneLighting {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float MAX_FRAME_SECONDS = 0.1F;
	private static final float IDENTITY_EPSILON = 1.0e-4F;
	private static final Vector3fc WHITE = new Vector3f(1F, 1F, 1F);

	private static volatile Map<Identifier, LightingProfile> profiles = Map.of();
	private static volatile LightingSelection selection = LightingSelection.NONE;
	private static volatile Map<Identifier, LightingProfile> dimensionProfiles = Map.of();
	private static volatile Map<Identifier, LightingProfile> biomeProfiles = Map.of();
	private static volatile List<TagSelector> biomeTags = List.of();
	private static volatile @Nullable LightingProfile fallbackProfile;
	private static volatile @Nullable Identifier override;
	private static final Set<Identifier> MISSING = ConcurrentHashMap.newKeySet();
	private static final List<Runnable> LISTENERS = new ArrayList<>();

	static final LightingFactors FACTORS = new LightingFactors();
	private static final Vector3f SCRATCH = new Vector3f();
	private static @Nullable Identifier activeId;
	private static @Nullable Applied previous;
	private static @Nullable Applied lastApplied;
	private static @Nullable Applied lastTarget;
	private static float blend = 1F;
	private static long lastMillis;
	private static boolean gradingActive;

	private record TagSelector(TagKey<Biome> tag, Identifier key, LightingProfile profile) {}

	private record Selected(Identifier key, LightingProfile profile) {}

	public static final int LIGHT_LEVELS = 16;

	public record Applied(
		float skyFactor, float blockFactor, float brightness,
		Vector3f skyLightColor, Vector3f ambientColor, Vector3f blockLightTint,
		float exposure, float contrast, float saturation, Vector3f lift, Vector3f gain,
		float[] blockCurve, float[] skyCurve, float bloomIntensity, float bloomRadius
	) {
		static float[] identityCurve() {
			final float[] curve = new float[LIGHT_LEVELS];
			for (int level = 0; level < LIGHT_LEVELS; level++) curve[level] = level / (float) (LIGHT_LEVELS - 1);
			return curve;
		}

		static Applied of(LightmapRenderState state) {
			return new Applied(
				state.skyFactor, state.blockFactor, state.brightness,
				copy(state.skyLightColor), copy(state.ambientColor), copy(state.blockLightTint),
				1F, 1F, 1F, new Vector3f(), new Vector3f(WHITE),
				identityCurve(), identityCurve(), GlowtonePackSettings.bloomIntensity(), GlowtonePackSettings.bloomRadius()
			);
		}

		private static Vector3f copy(@Nullable Vector3fc color) {
			return color == null ? new Vector3f(WHITE) : new Vector3f(color);
		}

		static Applied lerp(Applied from, Applied to, float t) {
			return new Applied(
				lerp(from.skyFactor, to.skyFactor, t), lerp(from.blockFactor, to.blockFactor, t), lerp(from.brightness, to.brightness, t),
				new Vector3f(from.skyLightColor).lerp(to.skyLightColor, t),
				new Vector3f(from.ambientColor).lerp(to.ambientColor, t),
				new Vector3f(from.blockLightTint).lerp(to.blockLightTint, t),
				lerp(from.exposure, to.exposure, t), lerp(from.contrast, to.contrast, t), lerp(from.saturation, to.saturation, t),
				new Vector3f(from.lift).lerp(to.lift, t),
				new Vector3f(from.gain).lerp(to.gain, t),
				lerp(from.blockCurve, to.blockCurve, t), lerp(from.skyCurve, to.skyCurve, t),
				lerp(from.bloomIntensity, to.bloomIntensity, t), lerp(from.bloomRadius, to.bloomRadius, t)
			);
		}

		private static float[] lerp(float[] from, float[] to, float t) {
			final float[] curve = new float[LIGHT_LEVELS];
			for (int level = 0; level < LIGHT_LEVELS; level++) curve[level] = lerp(from[level], to[level], t);
			return curve;
		}

		private static float lerp(float from, float to, float t) {
			return from + (to - from) * t;
		}

		boolean gradesIdentically() {
			return near(this.exposure, 1F) && near(this.contrast, 1F) && near(this.saturation, 1F)
				&& near(this.lift.x, 0F) && near(this.lift.y, 0F) && near(this.lift.z, 0F)
				&& near(this.gain.x, 1F) && near(this.gain.y, 1F) && near(this.gain.z, 1F)
				&& isIdentity(this.blockCurve) && isIdentity(this.skyCurve);
		}

		private static boolean isIdentity(float[] curve) {
			for (int level = 0; level < LIGHT_LEVELS; level++) {
				if (!near(curve[level], level / (float) (LIGHT_LEVELS - 1))) return false;
			}
			return true;
		}

		private static boolean near(float value, float target) {
			return Math.abs(value - target) < IDENTITY_EPSILON;
		}
	}

	public static void load(
		Map<Identifier, LightingProfile> fileProfiles,
		Map<Identifier, LightingAssignment> dimensionFiles,
		Map<Identifier, LightingAssignment> biomeFiles,
		LightingSettings settings
	) {
		final Map<Identifier, LightingProfile> merged = LightingSettings.mergeProfiles(settings.profiles(), fileProfiles);
		final Map<Identifier, LightingProfile> resolved = new HashMap<>(merged.size());
		merged.forEach((id, profile) -> resolved.put(id, inherit(id, profile, merged, 0)));
		MISSING.clear();

		final Map<Identifier, LightingProfile> dimensions = new HashMap<>();
		settings.selection().dimensions().forEach((dimension, id) -> {
			final LightingProfile profile = resolved.get(id);
			if (profile == null) {
				reportMissingProfile(id, dimension);
				return;
			}
			dimensions.put(dimension, profile);
		});
		dimensionFiles.forEach((dimension, assignment) -> dimensions.put(dimension, assignment.resolve(resolved, dimension)));

		final Map<Identifier, LightingProfile> biomes = new HashMap<>();
		final List<TagSelector> tags = new ArrayList<>();
		settings.selection().biomes().forEach((key, id) -> {
			final LightingProfile profile = resolved.get(id);
			if (profile == null) {
				reportMissingProfile(id, null);
				return;
			}
			if (key.startsWith("#")) {
				final Identifier tag = Identifier.tryParse(key.substring(1));
				if (tag == null) {
					LOGGER.error("Glowtone lighting selection has an unreadable biome tag '{}'", key);
					return;
				}
				tags.add(new TagSelector(TagKey.create(Registries.BIOME, tag), id, profile));
				return;
			}
			final Identifier biome = Identifier.tryParse(key);
			if (biome == null) {
				LOGGER.error("Glowtone lighting selection has an unreadable biome id '{}'", key);
				return;
			}
			biomes.put(biome, profile);
		});
		biomeFiles.forEach((biome, assignment) -> {
			if (!assignment.overrides().coloredLighting().isEmpty()) {
				LOGGER.warn(
					"Glowtone lighting for the biome {} sets colored_lighting, which is baked into chunk meshes and so only follows the dimension; it is being ignored here",
					biome
				);
			}
			biomes.put(biome, assignment.resolve(resolved, biome));
		});

		WorldLightCurves.begin();
		final Map<Identifier, LightingProfile> worldBiomes = new HashMap<>(biomes.size());
		biomes.forEach((biome, profile) ->
			worldBiomes.put(biome, WorldLightCurves.offer(profile.lightmap(), biome) ? profile.withoutCurves() : profile));
		final List<TagSelector> worldTags = new ArrayList<>(tags.size());
		for (TagSelector tag : tags) {
			worldTags.add(WorldLightCurves.offer(tag.profile().lightmap(), tag.tag())
				? new TagSelector(tag.tag(), tag.key(), tag.profile().withoutCurves())
				: tag);
		}
		WorldLightCurves.end();

		profiles = Map.copyOf(resolved);
		selection = settings.selection();
		dimensionProfiles = Map.copyOf(dimensions);
		biomeProfiles = Map.copyOf(worldBiomes);
		biomeTags = List.copyOf(worldTags);
		fallbackProfile = settings.selection().fallback().map(resolved::get).orElse(null);

		if (!resolved.isEmpty() || !dimensions.isEmpty() || !biomes.isEmpty() || !tags.isEmpty()) {
			LOGGER.info("Glowtone lighting: {} profiles, default {}, {} dimensions and {} biomes",
				resolved.size(), settings.selection().fallback().map(Identifier::toString).orElse("vanilla"),
				dimensions.size(), biomes.size() + tags.size());
		}

		final Minecraft minecraft = Minecraft.getInstance();
		applyColoredLighting(minecraft == null ? null : minecraft.level, true);
		for (Runnable listener : LISTENERS) listener.run();
	}

	static void reportMissingProfile(Identifier id, @Nullable Identifier user) {
		if (!MISSING.add(id)) return;

		if (user == null) {
			LOGGER.error("Glowtone lighting selects the profile {}, which no pack defines", id);
		} else {
			LOGGER.error("Glowtone lighting for {} names the profile {}, which no pack defines", user, id);
		}
	}

	private static LightingProfile inherit(Identifier id, LightingProfile profile, Map<Identifier, LightingProfile> all, int depth) {
		if (profile.parent().isEmpty()) return profile;
		if (depth > 8) {
			LOGGER.error("Glowtone lighting profile {} has a parent chain deeper than 8 or a cycle; ignoring its parent", id);
			return profile;
		}

		final Identifier parentId = profile.parent().get();
		final LightingProfile parent = all.get(parentId);
		if (parent == null) {
			LOGGER.error("Glowtone lighting profile {} names the parent {}, which no pack defines", id, parentId);
			return profile;
		}
		return profile.inheriting(inherit(parentId, parent, all, depth + 1));
	}

	public static void onLevelChanged(@Nullable ClientLevel level) {
		WorldLightCurves.onLevelChanged();
		activeId = null;
		previous = null;
		lastApplied = null;
		lastTarget = null;
		blend = 1F;
		gradingActive = false;
		applyColoredLighting(level, false);
	}

	private static void applyColoredLighting(@Nullable ClientLevel level, boolean rebuild) {
		ChromaBlender.Tone tone = ChromaBlender.Tone.DEFAULT;
		if (level != null) {
			final Selected selected = dimensionSelection(level);
			if (selected != null) tone = selected.profile().coloredLighting().toTone();
		}

		if (!ChromaBlender.setTone(tone) || !rebuild) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.level != null) minecraft.levelExtractor.allChanged();
	}

	private static @Nullable Selected dimensionSelection(ClientLevel level) {
		final Selected forced = forced();
		if (forced != null) return forced;

		final Identifier dimension = level.dimension().identifier();
		final LightingProfile profile = dimensionProfiles.get(dimension);
		if (profile != null) return new Selected(dimension, profile);

		final LightingProfile fallback = fallbackProfile;
		return fallback == null ? null : new Selected(selection.fallback().orElse(dimension), fallback);
	}

	private static @Nullable Selected forced() {
		final Identifier id = override;
		if (id == null) return null;

		final LightingProfile profile = profiles.get(id);
		if (profile == null) {
			reportMissingProfile(id, null);
			return null;
		}
		return new Selected(id, profile);
	}

	private static @Nullable Selected select(ClientLevel level, Camera camera) {
		final Selected forced = forced();
		if (forced != null) return forced;

		final Map<Identifier, LightingProfile> biomes = biomeProfiles;
		final List<TagSelector> tags = biomeTags;
		if (!biomes.isEmpty() || !tags.isEmpty()) {
			final Holder<Biome> biome = level.getBiome(camera.blockPosition());
			if (!biomes.isEmpty()) {
				final Optional<Identifier> biomeId = biome.unwrapKey().map(key -> key.identifier());
				if (biomeId.isPresent()) {
					final LightingProfile profile = biomes.get(biomeId.get());
					if (profile != null) return new Selected(biomeId.get(), profile);
				}
			}
			for (TagSelector selector : tags) {
				if (biome.is(selector.tag())) return new Selected(selector.key(), selector.profile());
			}
		}
		return dimensionSelection(level);
	}

	public static void apply(LightmapRenderState state, float partialTick) {
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientLevel level = minecraft.level;
		if (level == null) return;

		final boolean worldCurves = WorldLightCurves.any();
		final boolean idle = !worldCurves && override == null && dimensionProfiles.isEmpty() && biomeProfiles.isEmpty()
			&& biomeTags.isEmpty() && fallbackProfile == null;
		if (idle && lastApplied == null) return;

		final Camera camera = minecraft.gameRenderer.mainCamera();
		final Selected selected = idle ? null : select(level, camera);
		final Applied base = Applied.of(state);
		Applied target = base;
		final boolean needsFactors = WorldLightCurves.varying() || (selected != null && !selected.profile().isEmpty());
		if (needsFactors) FACTORS.sample(level, camera, partialTick);
		if (needsFactors) WorldLightCurves.refresh(FACTORS);
		if (selected != null && !selected.profile().isEmpty()) {
			target = resolve(selected.profile().lightmap(), selected.profile().bloom(), base);
		}

		final long now = Util.getMillis();
		final float seconds = Math.min(MAX_FRAME_SECONDS, Math.max(0F, (now - lastMillis) / 1000F));
		lastMillis = now;

		final Identifier selectedId = selected == null ? null : selected.key();
		if (!Objects.equals(selectedId, activeId)) {
			previous = lastApplied != null ? lastApplied : base;
			blend = lastApplied == null ? 1F : 0F;
			activeId = selectedId;
		}

		final float transition = selection.transitionSeconds();
		if (blend < 1F) blend = transition <= 0F ? 1F : Math.min(1F, blend + seconds / transition);

		final Applied applied = blend >= 1F || previous == null ? target : Applied.lerp(previous, target, blend);
		lastApplied = applied;
		lastTarget = target;

		state.skyFactor = applied.skyFactor();
		state.blockFactor = applied.blockFactor();
		state.brightness = applied.brightness();
		state.skyLightColor = applied.skyLightColor();
		state.ambientColor = applied.ambientColor();
		state.blockLightTint = applied.blockLightTint();
		gradingActive = !applied.gradesIdentically();

		if (idle && blend >= 1F) {
			lastApplied = null;
			previous = null;
		}
	}

	private static Applied resolve(LightmapProfile lightmap, BloomProfile bloom, Applied base) {
		final Vector3f skyLightColor = new Vector3f(base.skyLightColor());
		final Vector3f ambientColor = new Vector3f(base.ambientColor());
		final Vector3f blockLightTint = new Vector3f(base.blockLightTint());
		lightmap.skyLightColor().ifPresent(color -> color.resolve(skyLightColor, FACTORS, SCRATCH));
		lightmap.ambientColor().ifPresent(color -> color.resolve(ambientColor, FACTORS, SCRATCH));
		lightmap.blockLightTint().ifPresent(color -> color.resolve(blockLightTint, FACTORS, SCRATCH));

		final LightmapProfile.Grading grading = lightmap.grading();
		final Vector3f lift = new Vector3f();
		final Vector3f gain = new Vector3f(WHITE);
		grading.lift().ifPresent(color -> color.resolve(lift, FACTORS, SCRATCH));
		grading.gain().ifPresent(color -> color.resolve(gain, FACTORS, SCRATCH));

		return new Applied(
			Math.max(0F, value(lightmap.skyLightFactor(), base.skyFactor())),
			Math.max(0F, value(lightmap.blockLightFactor(), base.blockFactor())),
			Math.clamp(value(lightmap.brightness(), base.brightness()), 0F, 1F),
			skyLightColor, ambientColor, blockLightTint,
			Math.max(0F, value(grading.exposure(), 1F)),
			Math.max(0F, value(grading.contrast(), 1F)),
			Math.max(0F, value(grading.saturation(), 1F)),
			lift, gain,
			resolveCurve(lightmap.blockLightCurve()), resolveCurve(lightmap.skyLightCurve()),
			Math.max(0F, value(bloom.intensity(), base.bloomIntensity())),
			Math.max(0F, value(bloom.radius(), base.bloomRadius()))
		);
	}

	static float[] resolveCurve(Optional<LightingValue> spec) {
		return resolveCurve(spec, FACTORS);
	}

	static float[] resolveCurve(Optional<LightingValue> spec, LightingFactors factors) {
		final float[] curve = Applied.identityCurve();
		if (spec.isEmpty()) return curve;

		for (int level = 0; level < LIGHT_LEVELS; level++) {
			factors.setLightLevel(curve[level]);
			curve[level] = Math.clamp(spec.get().resolve(curve[level], factors), 0F, 1F);
		}
		factors.clearLightLevel();
		return curve;
	}

	private static float value(Optional<LightingValue> spec, float base) {
		return spec.isEmpty() ? base : spec.get().resolve(base, FACTORS);
	}

	public static boolean gradingActive() {
		return gradingActive && lastApplied != null;
	}

	public static float bloomIntensity(float packDefault) {
		final Applied applied = lastApplied;
		return applied == null ? packDefault : applied.bloomIntensity();
	}

	public static float bloomRadius(float packDefault) {
		final Applied applied = lastApplied;
		return applied == null ? packDefault : applied.bloomRadius();
	}

	public static void renderGradedLightmap(LightmapRenderState state) {
		final Applied applied = lastApplied;
		if (applied != null) GlowtoneLightmap.render(state, applied);
	}

	public static @Nullable Identifier activeProfile() {
		return activeId;
	}

	public static Set<Identifier> profileIds() {
		return profiles.keySet();
	}

	static @Nullable LightingProfile profileFor(Identifier id) {
		return profiles.get(id);
	}

	static @Nullable LightingProfile dimensionProfile(Identifier dimension) {
		return dimensionProfiles.get(dimension);
	}

	static @Nullable LightingProfile biomeProfile(Identifier biome) {
		return biomeProfiles.get(biome);
	}

	public static void overrideProfile(@Nullable Identifier id) {
		override = id;
		final Minecraft minecraft = Minecraft.getInstance();
		applyColoredLighting(minecraft == null ? null : minecraft.level, true);
	}

	public static void onChange(Runnable listener) {
		LISTENERS.add(listener);
	}

	static @Nullable Applied lastTarget() {
		return lastTarget;
	}

	private GlowtoneLighting() {}
}
