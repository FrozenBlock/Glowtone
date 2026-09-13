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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public record BloomProfile(Optional<LightingValue> intensity, Optional<LightingValue> radius) {
	public static final BloomProfile NONE = new BloomProfile(Optional.empty(), Optional.empty());

	public static final Codec<BloomProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		LightingValue.CODEC.optionalFieldOf("intensity").forGetter(BloomProfile::intensity),
		LightingValue.CODEC.optionalFieldOf("radius").forGetter(BloomProfile::radius)
	).apply(instance, BloomProfile::new));

	public boolean isEmpty() {
		return this.intensity.isEmpty() && this.radius.isEmpty();
	}

	BloomProfile mergedOver(BloomProfile under) {
		return new BloomProfile(this.intensity.or(under::intensity), this.radius.or(under::radius));
	}
}
