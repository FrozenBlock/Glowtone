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

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.frozenblock.glowtone.lighting.LightingValue.Mode;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

@ClientOnly
public record LightingColor(List<Step> steps) {
	public static final Codec<Integer> RGB_CODEC = Codec.either(Codec.INT, Codec.STRING).flatXmap(
		either -> either.map(DataResult::success, LightingColor::parseHex),
		rgb -> DataResult.success(Either.right(String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF)))
	);

	public record Point(float at, int color) {
		public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.fieldOf("at").forGetter(Point::at),
			RGB_CODEC.fieldOf("color").forGetter(Point::color)
		).apply(instance, Point::new));
	}

	public record Step(Mode mode, Optional<Identifier> factor, float[] inputs, int[] colors, int constant) {
		public static final Codec<Step> CODEC = RecordCodecBuilder.<Step>create(instance -> instance.group(
			Mode.CODEC.optionalFieldOf("mode", Mode.OVERRIDE).forGetter(Step::mode),
			Identifier.CODEC.optionalFieldOf("factor").forGetter(Step::factor),
			Point.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(Step::pointList),
			RGB_CODEC.optionalFieldOf("color", 0xFFFFFF).forGetter(Step::constant)
		).apply(instance, Step::fromJson)).flatXmap(Step::validate, DataResult::success);

		public static Step constant(Mode mode, int rgb) {
			return new Step(mode, Optional.empty(), new float[0], new int[0], rgb);
		}

		private static Step fromJson(Mode mode, Optional<Identifier> factor, List<Point> points, int constant) {
			final float[] inputs = new float[points.size()];
			final int[] colors = new int[points.size()];
			for (int i = 0; i < points.size(); i++) {
				inputs[i] = points.get(i).at();
				colors[i] = points.get(i).color();
			}
			return new Step(mode, factor, inputs, colors, constant);
		}

		private static DataResult<Step> validate(Step step) {
			if (step.factor.isEmpty()) {
				return step.inputs.length == 0
					? DataResult.success(step)
					: DataResult.error(() -> "points need a factor to read them against");
			}
			if (step.inputs.length == 0) return DataResult.error(() -> "a factor needs at least one point");
			for (int i = 1; i < step.inputs.length; i++) {
				if (step.inputs[i] < step.inputs[i - 1]) return DataResult.error(() -> "points must be ordered by 'at'");
			}
			return DataResult.success(step);
		}

		private List<Point> pointList() {
			final List<Point> points = new ArrayList<>(this.inputs.length);
			for (int i = 0; i < this.inputs.length; i++) points.add(new Point(this.inputs[i], this.colors[i]));
			return points;
		}

		void evaluate(LightingFactors factors, Vector3f out) {
			if (this.factor.isEmpty()) {
				rgb(this.constant, out);
				return;
			}

			final float at = factors.get(this.factor.get());
			final int last = this.inputs.length - 1;
			if (at <= this.inputs[0]) {
				rgb(this.colors[0], out);
				return;
			}
			if (at >= this.inputs[last]) {
				rgb(this.colors[last], out);
				return;
			}

			int index = 1;
			while (this.inputs[index] < at) index++;
			final float span = this.inputs[index] - this.inputs[index - 1];
			final float fraction = span <= 0F ? 1F : (at - this.inputs[index - 1]) / span;
			out.set(
				channel(this.colors[index - 1], 16) + (channel(this.colors[index], 16) - channel(this.colors[index - 1], 16)) * fraction,
				channel(this.colors[index - 1], 8) + (channel(this.colors[index], 8) - channel(this.colors[index - 1], 8)) * fraction,
				channel(this.colors[index - 1], 0) + (channel(this.colors[index], 0) - channel(this.colors[index - 1], 0)) * fraction
			);
		}
	}

	public static final Codec<LightingColor> CODEC = Codec.either(RGB_CODEC, Codec.either(Step.CODEC, Step.CODEC.listOf())).xmap(
		either -> either.map(
			rgb -> new LightingColor(List.of(Step.constant(Mode.OVERRIDE, rgb))),
			steps -> new LightingColor(steps.map(List::of, List::copyOf))
		),
		color -> color.steps.size() == 1 && color.steps.getFirst().factor().isEmpty() && color.steps.getFirst().mode() == Mode.OVERRIDE
			? Either.left(color.steps.getFirst().constant())
			: Either.right(Either.right(color.steps))
	);

	public void resolve(Vector3f current, LightingFactors factors, Vector3f scratch) {
		for (Step step : this.steps) {
			step.evaluate(factors, scratch);
			current.set(
				step.mode.apply(current.x, scratch.x),
				step.mode.apply(current.y, scratch.y),
				step.mode.apply(current.z, scratch.z)
			);
		}
	}

	static void rgb(int rgb, Vector3f out) {
		out.set(channel(rgb, 16), channel(rgb, 8), channel(rgb, 0));
	}

	private static float channel(int rgb, int shift) {
		return ((rgb >> shift) & 0xFF) / 255F;
	}

	private static DataResult<Integer> parseHex(String text) {
		final String digits = text.startsWith("#") ? text.substring(1) : text;
		if (digits.length() != 6) return DataResult.error(() -> "expected a #RRGGBB color, got '" + text + "'");
		try {
			return DataResult.success(Integer.parseInt(digits, 16) & 0xFFFFFF);
		} catch (NumberFormatException failure) {
			return DataResult.error(() -> "expected a #RRGGBB color, got '" + text + "'");
		}
	}

	public static int pack(Vector3f color) {
		return ARGB.color(ARGB.as8BitChannel(clamp(color.x)), ARGB.as8BitChannel(clamp(color.y)), ARGB.as8BitChannel(clamp(color.z)));
	}

	private static float clamp(float value) {
		return Math.clamp(value, 0F, 1F);
	}
}
