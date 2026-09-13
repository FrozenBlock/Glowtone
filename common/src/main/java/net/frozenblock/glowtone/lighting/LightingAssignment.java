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
import java.util.Map;
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@ClientOnly
public record LightingAssignment(Optional<Identifier> profile, LightingProfile overrides) {
	public static final Codec<LightingAssignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.optionalFieldOf("profile").forGetter(LightingAssignment::profile),
		LightingProfile.MAP_CODEC.forGetter(LightingAssignment::overrides)
	).apply(instance, LightingAssignment::new));

	public LightingAssignment mergedOver(LightingAssignment under) {
		return new LightingAssignment(this.profile.or(under::profile), this.overrides.mergedOver(under.overrides));
	}

	public LightingProfile resolve(Map<Identifier, LightingProfile> profiles, @Nullable Identifier key) {
		if (this.profile.isEmpty()) return this.overrides;

		final LightingProfile preset = profiles.get(this.profile.get());
		if (preset != null) return this.overrides.inheriting(preset);

		GlowtoneLighting.reportMissingProfile(this.profile.get(), key);
		return this.overrides;
	}
}
