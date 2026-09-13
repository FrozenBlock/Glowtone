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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ClientOnly
public final class WorldLightCurves {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int POINTS = 8;
	private static final int BLOCK = 0;
	private static final int SKY = 1;
	private static final int GROUPS = 2;
	private static final int PER_GROUP = (POINTS + 2) / 3;
	private static final int SCALE = 0xFF;
	private static final int STALE_FRAMES = 2;

	private static final int STEPS = LightCoordsUtil.MAX_SMOOTH_LIGHT_LEVEL;
	private static final int LEVELS = GlowtoneLighting.LIGHT_LEVELS;
	private static final int[] POINT_AT = pointAtTable();
	private static final int[] TO_SMOOTH = toSmoothTable();

	public static final long IDENTITY = sampleCurve(GlowtoneLighting.Applied.identityCurve());
	public static final ColorResolver[] RESOLVERS = new ColorResolver[GROUPS * PER_GROUP];

	static {
		for (int group = 0; group < GROUPS; group++) {
			for (int triple = 0; triple < PER_GROUP; triple++) {
				final int channel = group;
				final int base = triple * 3;
				RESOLVERS[group * PER_GROUP + triple] = (biome, x, z) -> {
					final long points = pointsFor(channel, biome);
					return pointOf(points, base) << 16 | pointOf(points, base + 1) << 8 | pointOf(points, base + 2);
				};
			}
		}
	}

	private static final class Curve {
		private final List<Identifier> biomes = new ArrayList<>();
		private final List<TagKey<Biome>> tags = new ArrayList<>();
		private final LightmapProfile lightmap;
		private final boolean varies;
		private volatile long block = IDENTITY;
		private volatile long sky = IDENTITY;

		private Curve(LightmapProfile lightmap) {
			this.lightmap = lightmap;
			this.varies = varies(lightmap.blockLightCurve()) || varies(lightmap.skyLightCurve());
		}
	}

	private static final class Memo {
		private long pos = Long.MIN_VALUE;
		private long generation = -1L;
		private long block = IDENTITY;
		private long sky = IDENTITY;
	}

	private static final ThreadLocal<Memo> MEMO = ThreadLocal.withInitial(Memo::new);
	private static final List<Curve> PENDING = new ArrayList<>();
	private static volatile Curve[] curves = new Curve[0];
	private static volatile int groups;
	private static volatile boolean varying;
	private static volatile @Nullable Map<Biome, Curve> lookup;
	private static volatile BlockTintCache @Nullable [] caches;
	private static volatile long generation;
	private static int staleFrames;

	public static boolean any() {
		return curves.length > 0;
	}

	public static boolean varying() {
		return varying;
	}

	static void begin() {
		PENDING.clear();
	}

	static boolean offer(LightmapProfile lightmap, Object owner) {
		if (lightmap.blockLightCurve().isEmpty() && lightmap.skyLightCurve().isEmpty()) return false;

		Curve curve = null;
		for (Curve candidate : PENDING) {
			if (candidate.lightmap.blockLightCurve().equals(lightmap.blockLightCurve())
				&& candidate.lightmap.skyLightCurve().equals(lightmap.skyLightCurve())
			) {
				curve = candidate;
				break;
			}
		}

		if (curve == null) {
			curve = new Curve(lightmap);
			PENDING.add(curve);
		}

		if (owner instanceof Identifier biome) {
			curve.biomes.add(biome);
		} else if (owner instanceof TagKey<?> tag) {
			@SuppressWarnings("unchecked")
			final TagKey<Biome> biomeTag = (TagKey<Biome>) tag;
			curve.tags.add(biomeTag);
		}
		return true;
	}

	static void end() {
		curves = PENDING.toArray(Curve[]::new);
		PENDING.clear();
		lookup = null;
		staleFrames = 0;

		int present = 0;
		boolean anyVaries = false;
		int biomes = 0;
		int tags = 0;
		for (Curve curve : curves) {
			if (curve.lightmap.blockLightCurve().isPresent()) present |= 1 << BLOCK;
			if (curve.lightmap.skyLightCurve().isPresent()) present |= 1 << SKY;
			anyVaries |= curve.varies;
			biomes += curve.biomes.size();
			tags += curve.tags.size();
			derive(curve, GlowtoneLighting.FACTORS);
		}
		groups = present;
		varying = anyVaries;
		generation++;
		if (curves.length == 0) return;

		invalidate();
		LOGGER.info(
			"Glowtone applies {} distinct world-space falloff curves across {} biomes and {} biome tags",
			curves.length, biomes, tags
		);
	}

	static void refresh(LightingFactors factors) {
		if (!varying) return;

		boolean changed = false;
		for (Curve curve : curves) {
			if (curve.varies) changed |= derive(curve, factors);
		}
		if (changed) staleFrames = STALE_FRAMES;
		if (staleFrames > 0) {
			staleFrames--;
			invalidate();
		}
	}

	private static boolean derive(Curve curve, LightingFactors factors) {
		final long block = resolve(curve.lightmap.blockLightCurve(), factors);
		final long sky = resolve(curve.lightmap.skyLightCurve(), factors);
		if (block == curve.block && sky == curve.sky) return false;

		curve.block = block;
		curve.sky = sky;
		return true;
	}

	private static long resolve(Optional<LightingValue> spec, LightingFactors factors) {
		return spec.isEmpty() ? IDENTITY : sampleCurve(GlowtoneLighting.resolveCurve(spec, factors));
	}

	private static boolean varies(Optional<LightingValue> spec) {
		if (spec.isEmpty()) return false;

		for (LightingValue.Step step : spec.get().steps()) {
			if (step.factor().filter(factor -> !factor.equals(LightingFactors.LIGHT_LEVEL)).isPresent()) return true;
		}
		return false;
	}

	public static BlockTintCache[] newCaches(ClientLevel level) {
		final BlockTintCache[] created = new BlockTintCache[RESOLVERS.length];
		for (int index = 0; index < RESOLVERS.length; index++) {
			final ColorResolver resolver = RESOLVERS[index];
			created[index] = new BlockTintCache(pos -> level.calculateBlockTint(pos, resolver));
		}
		caches = created;
		return created;
	}

	private static void invalidate() {
		generation++;

		final BlockTintCache[] current = caches;
		if (current == null) return;
		for (BlockTintCache cache : current) cache.invalidateAll();
	}

	public static void onLevelChanged() {
		lookup = null;
		staleFrames = 0;
		generation++;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.level != null) minecraft.level.clearTintCaches();
	}

	private static long pointsFor(int group, Biome biome) {
		final Map<Biome, Curve> members = members();
		final Curve curve = members == null ? null : members.get(biome);
		if (curve == null) return IDENTITY;
		return group == BLOCK ? curve.block : curve.sky;
	}

	private static @Nullable Map<Biome, Curve> members() {
		final Map<Biome, Curve> cached = lookup;
		if (cached != null) return cached;

		final Minecraft minecraft = Minecraft.getInstance();
		final ClientLevel level = minecraft == null ? null : minecraft.level;
		if (level == null) return null;

		final HolderLookup.RegistryLookup<Biome> registry = level.registryAccess().lookup(Registries.BIOME).orElse(null);
		if (registry == null) return null;

		final Map<Biome, Curve> resolved = new IdentityHashMap<>();
		for (Curve curve : curves) {
			for (TagKey<Biome> tag : curve.tags) {
				registry.get(tag).ifPresent(named -> named.forEach(holder -> resolved.put(holder.value(), curve)));
			}
		}
		for (Curve curve : curves) {
			for (Identifier id : curve.biomes) {
				registry.get(ResourceKey.create(Registries.BIOME, id)).ifPresent(holder -> resolved.put(holder.value(), curve));
			}
		}

		final Map<Biome, Curve> members = Collections.unmodifiableMap(resolved);
		lookup = members;
		return members;
	}

	public static long blockCurveAt(BlockAndTintGetter level, BlockPos pos) {
		return curves.length == 0 ? IDENTITY : memo(level, pos).block;
	}

	public static long skyCurveAt(BlockAndTintGetter level, BlockPos pos) {
		return curves.length == 0 ? IDENTITY : memo(level, pos).sky;
	}

	private static Memo memo(BlockAndTintGetter level, BlockPos pos) {
		final Memo memo = MEMO.get();
		final long packed = pos.asLong();
		final long current = generation;
		if (memo.pos == packed && memo.generation == current) return memo;

		memo.pos = packed;
		memo.generation = current;
		memo.block = curveAt(level, pos, BLOCK);
		memo.sky = curveAt(level, pos, SKY);
		return memo;
	}

	private static long curveAt(BlockAndTintGetter level, BlockPos pos, int group) {
		if ((groups & 1 << group) == 0) return IDENTITY;

		long packed = 0L;
		for (int triple = 0; triple < PER_GROUP; triple++) {
			final int tint = level.getBlockTint(pos, RESOLVERS[group * PER_GROUP + triple]);
			final int base = triple * 3;
			packed |= (long) (tint >> 16 & SCALE) << (base * 8);
			if (base + 1 < POINTS) packed |= (long) (tint >> 8 & SCALE) << ((base + 1) * 8);
			if (base + 2 < POINTS) packed |= (long) (tint & SCALE) << ((base + 2) * 8);
		}
		return packed;
	}

	public static int remap(int lightCoords, long blockCurve, long skyCurve) {
		if (blockCurve == IDENTITY && skyCurve == IDENTITY) return lightCoords;

		return LightCoordsUtil.smoothPack(
			bend(LightCoordsUtil.smoothBlock(lightCoords), blockCurve),
			bend(LightCoordsUtil.smoothSky(lightCoords), skyCurve)
		);
	}

	public static int remap(BlockAndTintGetter level, BlockPos pos, int lightCoords) {
		return remap(lightCoords, blockCurveAt(level, pos), skyCurveAt(level, pos));
	}

	static long packedCurve(Identifier biome, boolean block) {
		for (Curve curve : curves) {
			if (curve.biomes.contains(biome)) return block ? curve.block : curve.sky;
		}
		return IDENTITY;
	}

	private static int bend(int smoothLevel, long curve) {
		if (curve == IDENTITY) return smoothLevel;

		final int at = POINT_AT[smoothLevel & SCALE];
		final int low = at >> 8;
		final int start = pointOf(curve, low);
		return TO_SMOOTH[start + ((pointOf(curve, Math.min(POINTS - 1, low + 1)) - start) * (at & SCALE) >> 8)];
	}

	private static int[] pointAtTable() {
		final int[] table = new int[SCALE + 1];
		for (int smooth = 0; smooth <= SCALE; smooth++) table[smooth] = Math.min(smooth, STEPS) * (POINTS - 1) * (SCALE + 1) / STEPS;
		return table;
	}

	private static int[] toSmoothTable() {
		final int[] table = new int[SCALE + 1];
		for (int value = 0; value <= SCALE; value++) table[value] = Math.round(value * STEPS / (float) SCALE);
		return table;
	}

	private static int pointOf(long curve, int point) {
		return point < POINTS ? (int) (curve >>> (point * 8) & SCALE) : 0;
	}

	private static long sampleCurve(float[] table) {
		long packed = 0L;
		for (int point = 0; point < POINTS; point++) {
			final float scaled = point / (float) (POINTS - 1) * (LEVELS - 1);
			final int low = (int) scaled;
			final float start = table[low];
			final float value = start + (table[Math.min(LEVELS - 1, low + 1)] - start) * (scaled - low);
			packed |= (long) Math.round(Math.clamp(value, 0F, 1F) * SCALE) << (point * 8);
		}
		return packed;
	}

	private WorldLightCurves() {}
}
