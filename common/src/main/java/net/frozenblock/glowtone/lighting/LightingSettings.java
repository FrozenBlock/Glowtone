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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public record LightingSettings(Map<Identifier, LightingProfile> profiles, LightingSelection selection) {
	public static final LightingSettings NONE = new LightingSettings(Map.of(), LightingSelection.NONE);

	public static final Codec<LightingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(Identifier.CODEC, LightingProfile.CODEC).optionalFieldOf("profiles", Map.of()).forGetter(LightingSettings::profiles),
		LightingSelection.MAP_CODEC.forGetter(LightingSettings::selection)
	).apply(instance, LightingSettings::new));

	public boolean isEmpty() {
		return this.profiles.isEmpty() && this.selection.isEmpty();
	}

	public LightingSettings mergedOver(LightingSettings under) {
		return new LightingSettings(mergeProfiles(this.profiles, under.profiles), this.selection.mergedOver(under.selection));
	}

	public static Map<Identifier, LightingProfile> mergeProfiles(Map<Identifier, LightingProfile> over, Map<Identifier, LightingProfile> under) {
		if (over.isEmpty()) return under;
		if (under.isEmpty()) return over;

		final Map<Identifier, LightingProfile> merged = new LinkedHashMap<>(under);
		over.forEach((id, profile) -> {
			final LightingProfile below = merged.get(id);
			merged.put(id, below == null ? profile : profile.mergedOver(below));
		});
		return Collections.unmodifiableMap(merged);
	}
}
