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
import java.util.Optional;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public record LightingValue(List<Step> steps) {

	public enum Mode implements StringRepresentable {
		OVERRIDE("override"),
		MULTIPLY("multiply"),
		ADD("add"),
		MIN("min"),
		MAX("max");

		public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
		private final String name;

		Mode(String name) {
			this.name = name;
		}

		public float apply(float current, float value) {
			return switch (this) {
				case OVERRIDE -> value;
				case MULTIPLY -> current * value;
				case ADD -> current + value;
				case MIN -> Math.min(current, value);
				case MAX -> Math.max(current, value);
			};
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public record Step(Mode mode, Optional<Identifier> factor, float[] inputs, float[] outputs, float constant) {
		private static final Codec<List<List<Float>>> POINTS = Codec.FLOAT.listOf().listOf();

		public static final Codec<Step> CODEC = RecordCodecBuilder.<Step>create(instance -> instance.group(
			Mode.CODEC.optionalFieldOf("mode", Mode.OVERRIDE).forGetter(Step::mode),
			Identifier.CODEC.optionalFieldOf("factor").forGetter(Step::factor),
			POINTS.optionalFieldOf("points", List.of()).forGetter(Step::pointList),
			Codec.FLOAT.optionalFieldOf("value", 0F).forGetter(Step::constant)
		).apply(instance, Step::fromJson)).flatXmap(Step::validate, DataResult::success);

		public static Step constant(Mode mode, float value) {
			return new Step(mode, Optional.empty(), new float[0], new float[0], value);
		}

		private static Step fromJson(Mode mode, Optional<Identifier> factor, List<List<Float>> points, float constant) {
			final int count = points.size();
			final float[] inputs = new float[count];
			final float[] outputs = new float[count];
			for (int i = 0; i < count; i++) {
				final List<Float> point = points.get(i);
				inputs[i] = point.isEmpty() ? Float.NaN : point.get(0);
				outputs[i] = point.size() < 2 ? Float.NaN : point.get(1);
			}
			return new Step(mode, factor, inputs, outputs, constant);
		}

		private static DataResult<Step> validate(Step step) {
			if (step.factor.isEmpty()) {
				return step.inputs.length == 0
					? DataResult.success(step)
					: DataResult.error(() -> "points need a factor to read them against");
			}
			if (step.inputs.length == 0) return DataResult.error(() -> "a factor needs at least one [input, output] point");
			for (int i = 0; i < step.inputs.length; i++) {
				if (Float.isNaN(step.inputs[i]) || Float.isNaN(step.outputs[i])) {
					return DataResult.error(() -> "every point must be an [input, output] pair");
				}
				if (i > 0 && step.inputs[i] < step.inputs[i - 1]) return DataResult.error(() -> "points must be ordered by input");
			}
			return DataResult.success(step);
		}

		private List<List<Float>> pointList() {
			final List<List<Float>> points = new ArrayList<>(this.inputs.length);
			for (int i = 0; i < this.inputs.length; i++) points.add(List.of(this.inputs[i], this.outputs[i]));
			return points;
		}

		public float evaluate(LightingFactors factors) {
			if (this.factor.isEmpty()) return this.constant;
			return sample(this.inputs, this.outputs, factors.get(this.factor.get()));
		}
	}

	public static final Codec<LightingValue> CODEC = Codec.either(Codec.FLOAT, Codec.either(Step.CODEC, Step.CODEC.listOf())).xmap(
		either -> either.map(
			value -> new LightingValue(List.of(Step.constant(Mode.OVERRIDE, value))),
			steps -> new LightingValue(steps.map(List::of, List::copyOf))
		),
		value -> value.steps.size() == 1 && value.steps.getFirst().factor().isEmpty() && value.steps.getFirst().mode() == Mode.OVERRIDE
			? Either.left(value.steps.getFirst().constant())
			: Either.right(Either.right(value.steps))
	);

	public float resolve(float base, LightingFactors factors) {
		float current = base;
		for (Step step : this.steps) current = step.mode.apply(current, step.evaluate(factors));
		return current;
	}

	static float sample(float[] inputs, float[] outputs, float at) {
		final int last = inputs.length - 1;
		if (at <= inputs[0]) return outputs[0];
		if (at >= inputs[last]) return outputs[last];

		int index = 1;
		while (inputs[index] < at) index++;
		final float span = inputs[index] - inputs[index - 1];
		if (span <= 0F) return outputs[index];
		final float fraction = (at - inputs[index - 1]) / span;
		return outputs[index - 1] + (outputs[index] - outputs[index - 1]) * fraction;
	}
}
