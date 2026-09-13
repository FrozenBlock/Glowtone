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
public record LightmapProfile(
	Optional<LightingValue> skyLightFactor,
	Optional<LightingValue> blockLightFactor,
	Optional<LightingValue> brightness,
	Optional<LightingValue> blockLightCurve,
	Optional<LightingValue> skyLightCurve,
	Optional<LightingColor> skyLightColor,
	Optional<LightingColor> ambientColor,
	Optional<LightingColor> blockLightTint,
	Grading grading
) {
	public record Grading(
		Optional<LightingValue> exposure,
		Optional<LightingValue> contrast,
		Optional<LightingValue> saturation,
		Optional<LightingColor> lift,
		Optional<LightingColor> gain
	) {
		public static final Grading NONE = new Grading(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

		public static final Codec<Grading> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LightingValue.CODEC.optionalFieldOf("exposure").forGetter(Grading::exposure),
			LightingValue.CODEC.optionalFieldOf("contrast").forGetter(Grading::contrast),
			LightingValue.CODEC.optionalFieldOf("saturation").forGetter(Grading::saturation),
			LightingColor.CODEC.optionalFieldOf("lift").forGetter(Grading::lift),
			LightingColor.CODEC.optionalFieldOf("gain").forGetter(Grading::gain)
		).apply(instance, Grading::new));

		public boolean isEmpty() {
			return this.exposure.isEmpty() && this.contrast.isEmpty() && this.saturation.isEmpty() && this.lift.isEmpty() && this.gain.isEmpty();
		}

		Grading mergedOver(Grading under) {
			return new Grading(
				this.exposure.or(under::exposure),
				this.contrast.or(under::contrast),
				this.saturation.or(under::saturation),
				this.lift.or(under::lift),
				this.gain.or(under::gain)
			);
		}
	}

	public static final LightmapProfile NONE = new LightmapProfile(
		Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
		Optional.empty(), Optional.empty(), Optional.empty(), Grading.NONE
	);

	public static final Codec<LightmapProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		LightingValue.CODEC.optionalFieldOf("sky_light_factor").forGetter(LightmapProfile::skyLightFactor),
		LightingValue.CODEC.optionalFieldOf("block_light_factor").forGetter(LightmapProfile::blockLightFactor),
		LightingValue.CODEC.optionalFieldOf("brightness").forGetter(LightmapProfile::brightness),
		LightingValue.CODEC.optionalFieldOf("block_light_curve").forGetter(LightmapProfile::blockLightCurve),
		LightingValue.CODEC.optionalFieldOf("sky_light_curve").forGetter(LightmapProfile::skyLightCurve),
		LightingColor.CODEC.optionalFieldOf("sky_light_color").forGetter(LightmapProfile::skyLightColor),
		LightingColor.CODEC.optionalFieldOf("ambient_color").forGetter(LightmapProfile::ambientColor),
		LightingColor.CODEC.optionalFieldOf("block_light_tint").forGetter(LightmapProfile::blockLightTint),
		Grading.CODEC.optionalFieldOf("grading", Grading.NONE).forGetter(LightmapProfile::grading)
	).apply(instance, LightmapProfile::new));

	public boolean isEmpty() {
		return this.skyLightFactor.isEmpty() && this.blockLightFactor.isEmpty() && this.brightness.isEmpty()
			&& this.blockLightCurve.isEmpty() && this.skyLightCurve.isEmpty()
			&& this.skyLightColor.isEmpty() && this.ambientColor.isEmpty() && this.blockLightTint.isEmpty() && this.grading.isEmpty();
	}

	LightmapProfile withoutCurves() {
		return new LightmapProfile(
			this.skyLightFactor, this.blockLightFactor, this.brightness,
			Optional.empty(), Optional.empty(),
			this.skyLightColor, this.ambientColor, this.blockLightTint, this.grading
		);
	}

	LightmapProfile mergedOver(LightmapProfile under) {
		return new LightmapProfile(
			this.skyLightFactor.or(under::skyLightFactor),
			this.blockLightFactor.or(under::blockLightFactor),
			this.brightness.or(under::brightness),
			this.blockLightCurve.or(under::blockLightCurve),
			this.skyLightCurve.or(under::skyLightCurve),
			this.skyLightColor.or(under::skyLightColor),
			this.ambientColor.or(under::ambientColor),
			this.blockLightTint.or(under::blockLightTint),
			this.grading.mergedOver(under.grading)
		);
	}
}
