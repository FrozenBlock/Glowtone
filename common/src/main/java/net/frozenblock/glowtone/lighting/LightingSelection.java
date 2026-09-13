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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public record LightingSelection(
	Optional<Identifier> fallback,
	Map<Identifier, Identifier> dimensions,
	Map<String, Identifier> biomes,
	Optional<Float> transition
) {
	public static final float DEFAULT_TRANSITION = 1F;
	public static final LightingSelection NONE = new LightingSelection(Optional.empty(), Map.of(), Map.of(), Optional.empty());

	public static final MapCodec<LightingSelection> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.optionalFieldOf("default").forGetter(LightingSelection::fallback),
		Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("dimensions", Map.of()).forGetter(LightingSelection::dimensions),
		Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("biomes", Map.of()).forGetter(LightingSelection::biomes),
		Codec.floatRange(0F, 60F).optionalFieldOf("transition").forGetter(LightingSelection::transition)
	).apply(instance, LightingSelection::new));

	public boolean isEmpty() {
		return this.fallback.isEmpty() && this.dimensions.isEmpty() && this.biomes.isEmpty();
	}

	public float transitionSeconds() {
		return this.transition.orElse(DEFAULT_TRANSITION);
	}

	public LightingSelection mergedOver(LightingSelection under) {
		final Map<Identifier, Identifier> dimensions = new LinkedHashMap<>(under.dimensions);
		dimensions.putAll(this.dimensions);
		final Map<String, Identifier> biomes = new LinkedHashMap<>(under.biomes);
		biomes.putAll(this.biomes);
		return new LightingSelection(
			this.fallback.or(under::fallback),
			Collections.unmodifiableMap(dimensions),
			Collections.unmodifiableMap(biomes),
			this.transition.or(under::transition)
		);
	}
}
