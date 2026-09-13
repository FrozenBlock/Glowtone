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
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public record LightingProfile(Optional<Identifier> parent, ColoredLightingProfile coloredLighting, LightmapProfile lightmap, BloomProfile bloom) {
	public static final LightingProfile NONE = new LightingProfile(Optional.empty(), ColoredLightingProfile.NONE, LightmapProfile.NONE, BloomProfile.NONE);

	public static final MapCodec<LightingProfile> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.optionalFieldOf("parent").forGetter(LightingProfile::parent),
		ColoredLightingProfile.CODEC.optionalFieldOf("colored_lighting", ColoredLightingProfile.NONE).forGetter(LightingProfile::coloredLighting),
		LightmapProfile.CODEC.optionalFieldOf("lightmap", LightmapProfile.NONE).forGetter(LightingProfile::lightmap),
		BloomProfile.CODEC.optionalFieldOf("bloom", BloomProfile.NONE).forGetter(LightingProfile::bloom)
	).apply(instance, LightingProfile::new));

	public static final Codec<LightingProfile> CODEC = MAP_CODEC.codec();

	public boolean isEmpty() {
		return this.coloredLighting.isEmpty() && this.lightmap.isEmpty() && this.bloom.isEmpty();
	}

	LightingProfile withoutCurves() {
		return new LightingProfile(this.parent, this.coloredLighting, this.lightmap.withoutCurves(), this.bloom);
	}

	public LightingProfile mergedOver(LightingProfile under) {
		return new LightingProfile(
			this.parent.or(under::parent),
			this.coloredLighting.mergedOver(under.coloredLighting),
			this.lightmap.mergedOver(under.lightmap),
			this.bloom.mergedOver(under.bloom)
		);
	}

	public LightingProfile inheriting(LightingProfile parent) {
		return new LightingProfile(
			parent.parent,
			this.coloredLighting.mergedOver(parent.coloredLighting),
			this.lightmap.mergedOver(parent.lightmap),
			this.bloom.mergedOver(parent.bloom)
		);
	}
}
