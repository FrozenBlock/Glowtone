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
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public record ColoredLightingProfile(
	Optional<Float> saturation,
	Optional<Float> skyStrength,
	Optional<Float> equalise,
	Optional<Float> targetLuma,
	Optional<Integer> fullTintLevel,
	Optional<Float> strength,
	Optional<Float> brightness,
	Optional<Integer> tint
) {
	public static final int NEUTRAL_TINT = 0xFFFFFF;
	public static final ColoredLightingProfile NONE = new ColoredLightingProfile(
		Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
		Optional.empty(), Optional.empty(), Optional.empty()
	);

	public static final Codec<ColoredLightingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.floatRange(0F, 1F).optionalFieldOf("saturation").forGetter(ColoredLightingProfile::saturation),
		Codec.floatRange(0F, 1F).optionalFieldOf("sky_strength").forGetter(ColoredLightingProfile::skyStrength),
		Codec.floatRange(0F, 1F).optionalFieldOf("equalise").forGetter(ColoredLightingProfile::equalise),
		Codec.floatRange(0F, 1F).optionalFieldOf("target_luma").forGetter(ColoredLightingProfile::targetLuma),
		Codec.intRange(1, 15).optionalFieldOf("full_tint_level").forGetter(ColoredLightingProfile::fullTintLevel),
		Codec.floatRange(0F, 4F).optionalFieldOf("strength").forGetter(ColoredLightingProfile::strength),
		Codec.floatRange(0F, 4F).optionalFieldOf("brightness").forGetter(ColoredLightingProfile::brightness),
		LightingColor.RGB_CODEC.optionalFieldOf("tint").forGetter(ColoredLightingProfile::tint)
	).apply(instance, ColoredLightingProfile::new));

	public boolean isEmpty() {
		return this.saturation.isEmpty() && this.skyStrength.isEmpty() && this.equalise.isEmpty()
			&& this.targetLuma.isEmpty() && this.fullTintLevel.isEmpty()
			&& this.strength.isEmpty() && this.brightness.isEmpty() && this.tint.isEmpty();
	}

	public ChromaBlender.Tone toTone() {
		final ChromaBlender.Tone defaults = ChromaBlender.Tone.DEFAULT;
		final float scale = this.brightness.orElse(1F);
		final int tint = this.tint.orElse(NEUTRAL_TINT);
		return new ChromaBlender.Tone(
			this.saturation.orElse(defaults.saturation()),
			this.skyStrength.orElse(defaults.skyStrength()),
			this.equalise.orElse(defaults.equalise()),
			this.targetLuma.orElse(defaults.targetLuma()),
			this.fullTintLevel.orElse(defaults.fullTintLevel()),
			this.strength.orElse(defaults.strength()),
			scale * (((tint >> 16) & 0xFF) / 255F),
			scale * (((tint >> 8) & 0xFF) / 255F),
			scale * ((tint & 0xFF) / 255F)
		);
	}

	ColoredLightingProfile mergedOver(ColoredLightingProfile under) {
		return new ColoredLightingProfile(
			this.saturation.or(under::saturation),
			this.skyStrength.or(under::skyStrength),
			this.equalise.or(under::equalise),
			this.targetLuma.or(under::targetLuma),
			this.fullTintLevel.or(under::fullTintLevel),
			this.strength.or(under::strength),
			this.brightness.or(under::brightness),
			this.tint.or(under::tint)
		);
	}
}
