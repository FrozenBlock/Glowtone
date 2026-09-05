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

package net.frozenblock.glowtone.config.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public record GlowtonePackSettings(Highlight highlight, Water water, Bloom bloom) {

	public enum Style implements StringRepresentable {
		SMOOTH("smooth"),
		HARD("hard");

		public static final Style DEFAULT = SMOOTH;
		public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

		private final String name;

		Style(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public enum Source implements StringRepresentable {
		OVERLAY("overlay"),
		POST("post");

		public static final Source DEFAULT = OVERLAY;
		public static final Codec<Source> CODEC = StringRepresentable.fromEnum(Source::values);

		private final String name;

		Source(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public enum Corners implements StringRepresentable {
		MITRE("mitre"),
		NUB("nub");

		public static final Corners DEFAULT = MITRE;
		public static final Codec<Corners> CODEC = StringRepresentable.fromEnum(Corners::values);

		private final String name;

		Corners(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public record Highlight(
		Optional<Style> style,
		Optional<Source> source,
		Optional<Corners> corners,
		Optional<Float> size,
		Optional<Float> strength,
		Optional<Float> distance
	) {
		public static final Highlight NONE = new Highlight(
			Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty());

		public static final Codec<Highlight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Style.CODEC.optionalFieldOf("style").forGetter(Highlight::style),
			Source.CODEC.optionalFieldOf("source").forGetter(Highlight::source),
			Corners.CODEC.optionalFieldOf("corners").forGetter(Highlight::corners),
			Codec.FLOAT.optionalFieldOf("size").forGetter(Highlight::size),
			Codec.FLOAT.optionalFieldOf("strength").forGetter(Highlight::strength),
			Codec.FLOAT.optionalFieldOf("distance").forGetter(Highlight::distance)
		).apply(instance, Highlight::new));

		Highlight mergedOver(Highlight under) {
			return new Highlight(
				this.style.or(under::style),
				this.source.or(under::source),
				this.corners.or(under::corners),
				this.size.or(under::size),
				this.strength.or(under::strength),
				this.distance.or(under::distance)
			);
		}
	}

	public record Water(
		Optional<Style> style,
		Optional<Float> size,
		Optional<Float> strength,
		Optional<Float> floor,
		Optional<Float> whiten,
		Optional<Float> lift,
		Optional<Float> opacity,
		Optional<Float> distance
	) {
		public static final Water NONE = new Water(
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
		);

		public static final Codec<Water> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Style.CODEC.optionalFieldOf("style").forGetter(Water::style),
			Codec.FLOAT.optionalFieldOf("size").forGetter(Water::size),
			Codec.FLOAT.optionalFieldOf("strength").forGetter(Water::strength),
			Codec.FLOAT.optionalFieldOf("floor").forGetter(Water::floor),
			Codec.FLOAT.optionalFieldOf("whiten").forGetter(Water::whiten),
			Codec.FLOAT.optionalFieldOf("lift").forGetter(Water::lift),
			Codec.FLOAT.optionalFieldOf("opacity").forGetter(Water::opacity),
			Codec.FLOAT.optionalFieldOf("distance").forGetter(Water::distance)
		).apply(instance, Water::new));

		Water mergedOver(Water under) {
			return new Water(
				this.style.or(under::style),
				this.size.or(under::size),
				this.strength.or(under::strength),
				this.floor.or(under::floor),
				this.whiten.or(under::whiten),
				this.lift.or(under::lift),
				this.opacity.or(under::opacity),
				this.distance.or(under::distance)
			);
		}
	}

	public record Bloom(Optional<Float> intensity, Optional<Float> radius) {
		public static final Bloom NONE = new Bloom(Optional.empty(), Optional.empty());

		public static final Codec<Bloom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.optionalFieldOf("intensity").forGetter(Bloom::intensity),
			Codec.FLOAT.optionalFieldOf("radius").forGetter(Bloom::radius)
		).apply(instance, Bloom::new));

		Bloom mergedOver(Bloom under) {
			return new Bloom(
				this.intensity.or(under::intensity),
				this.radius.or(under::radius)
			);
		}
	}

	public static final GlowtonePackSettings NONE =
		new GlowtonePackSettings(Highlight.NONE, Water.NONE, Bloom.NONE);

	public static final Codec<GlowtonePackSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Highlight.CODEC.optionalFieldOf("highlight", Highlight.NONE).forGetter(GlowtonePackSettings::highlight),
		Water.CODEC.optionalFieldOf("water_highlight", Water.NONE).forGetter(GlowtonePackSettings::water),
		Bloom.CODEC.optionalFieldOf("bloom", Bloom.NONE).forGetter(GlowtonePackSettings::bloom)
	).apply(instance, GlowtonePackSettings::new));

	public static final Style DEFAULT_HIGHLIGHT_STYLE = Style.HARD;
	public static final float DEFAULT_HIGHLIGHT_SIZE = 1F;
	public static final float DEFAULT_HIGHLIGHT_STRENGTH = 1F;
	public static final float DEFAULT_WATER_SIZE = 0.5F;
	public static final float DEFAULT_WATER_STRENGTH = 5F;
	public static final float DEFAULT_BLOOM_INTENSITY = 2F;
	public static final float DEFAULT_BLOOM_RADIUS = 12F;
	public static final float DEFAULT_HIGHLIGHT_DISTANCE = 1024F;
	public static final float DEFAULT_WATER_FLOOR = 0.6F;
	public static final float DEFAULT_WATER_WHITEN = 0.8F;
	public static final float DEFAULT_WATER_LIFT = 1.7F;
	public static final float DEFAULT_WATER_OPACITY = 0.9F;

	private static volatile GlowtonePackSettings current = NONE;

	private static float fraction(float value) {
		return Math.clamp(value, 0F, 1F);
	}

	public String describe() {
		return "highlight[style=" + this.highlight.style().orElse(DEFAULT_HIGHLIGHT_STYLE).getSerializedName()
			+ " source=" + this.highlight.source().orElse(Source.DEFAULT).getSerializedName()
			+ " corners=" + this.highlight.corners().orElse(Corners.DEFAULT).getSerializedName()
			+ " size=" + this.highlight.size().orElse(DEFAULT_HIGHLIGHT_SIZE)
			+ " strength=" + this.highlight.strength().orElse(DEFAULT_HIGHLIGHT_STRENGTH)
			+ " distance=" + this.highlight.distance().orElse(DEFAULT_HIGHLIGHT_DISTANCE)
			+ "] water[style=" + this.water.style().orElse(Style.DEFAULT).getSerializedName()
			+ " size=" + this.water.size().orElse(DEFAULT_WATER_SIZE)
			+ " strength=" + this.water.strength().orElse(DEFAULT_WATER_STRENGTH)
			+ " distance=" + this.water.distance().orElse(this.highlight.distance().orElse(DEFAULT_HIGHLIGHT_DISTANCE))
			+ "] bloom[intensity=" + this.bloom.intensity().orElse(DEFAULT_BLOOM_INTENSITY)
			+ " radius=" + this.bloom.radius().orElse(DEFAULT_BLOOM_RADIUS)
			+ "]";
	}

	public GlowtonePackSettings mergedOver(GlowtonePackSettings under) {
		return new GlowtonePackSettings(
			this.highlight.mergedOver(under.highlight),
			this.water.mergedOver(under.water),
			this.bloom.mergedOver(under.bloom)
		);
	}

	static boolean apply(GlowtonePackSettings settings) {
		final GlowtonePackSettings previous = current;
		current = settings;
		return !previous.highlight.equals(settings.highlight) || !previous.water.equals(settings.water);
	}

	public static Style highlightStyle() {
		return current.highlight.style.orElse(DEFAULT_HIGHLIGHT_STYLE);
	}

	public static Corners highlightCorners() {
		return current.highlight.corners().orElse(Corners.DEFAULT);
	}

	public static Source highlightSource() {
		return current.highlight.source.orElse(Source.DEFAULT);
	}

	public static Style waterStyle() {
		return current.water.style().orElse(Style.DEFAULT);
	}

	public static float highlightDistance() {
		return current.highlight.distance.orElse(DEFAULT_HIGHLIGHT_DISTANCE);
	}

	public static float highlightStrength() {
		return current.highlight.strength().orElse(DEFAULT_HIGHLIGHT_STRENGTH);
	}

	public static float highlightSize() {
		return current.highlight.size.orElse(DEFAULT_HIGHLIGHT_SIZE);
	}

	public static float waterSize() {
		return current.water.size().orElse(DEFAULT_WATER_SIZE);
	}

	public static float waterStrength() {
		return current.water.strength().orElse(DEFAULT_WATER_STRENGTH);
	}

	public static float waterFloor() {
		return fraction(current.water.floor().orElse(DEFAULT_WATER_FLOOR));
	}

	public static float waterWhiten() {
		return fraction(current.water.whiten().orElse(DEFAULT_WATER_WHITEN));
	}

	public static float waterLift() {
		return current.water.lift().orElse(DEFAULT_WATER_LIFT);
	}

	public static float waterOpacity() {
		return fraction(current.water.opacity().orElse(DEFAULT_WATER_OPACITY));
	}

	public static float waterDistance() {
		return current.water.distance().orElse(highlightDistance());
	}

	public static float bloomIntensity() {
		return current.bloom.intensity.orElse(DEFAULT_BLOOM_INTENSITY);
	}

	public static float bloomRadius() {
		return current.bloom.radius.orElse(DEFAULT_BLOOM_RADIUS);
	}
}
