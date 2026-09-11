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

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.frozenblock.glowtone.config.pack.GlowtonePackDeclaration.Setting;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

@ClientOnly
public sealed interface GlowtonePackCondition {
	Codec<GlowtonePackCondition> CODEC = Codec.lazyInitialized(Filters::new);
	GlowtonePackCondition ALWAYS = new AllOf(List.of());

	boolean test(Values values);

	void references(Consumer<Reference> references);

	boolean external();

	DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner);

	record Reference(Optional<String> pack, String setting) {}

	interface Values {
		@Nullable String setting(String id);

		@Nullable String setting(String packId, String id);

		boolean enabled(String packId);
	}

	enum Operator implements StringRepresentable {
		EQUALS("=="),
		NOT_EQUALS("!=");

		private static final Map<String, Operator> BY_NAME = Map.of(
			"==", EQUALS,
			"equals", EQUALS,
			"=", EQUALS,
			"!=", NOT_EQUALS,
			"not", NOT_EQUALS,
			"<>", NOT_EQUALS
		);

		private final String name;

		Operator(String name) {
			this.name = name;
		}

		static Optional<Operator> of(String name) {
			return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
		}

		boolean holds(boolean matched) {
			return this == EQUALS ? matched : !matched;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	record SettingTest(Optional<String> pack, String setting, Operator operator, List<String> values) implements GlowtonePackCondition {
		@Override
		public boolean test(Values values) {
			final String current = this.pack.isPresent()
				? values.setting(this.pack.get(), this.setting)
				: values.setting(this.setting);
			return current != null && this.operator.holds(this.values.contains(current));
		}

		@Override
		public void references(Consumer<Reference> references) {
			references.accept(new Reference(this.pack, this.setting));
		}

		@Override
		public boolean external() {
			return this.pack.isPresent();
		}

		@Override
		public DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner) {
			if (this.pack.isPresent()) return DataResult.success(this);

			final Setting referenced = declared.get(this.setting);
			if (referenced == null) {
				return DataResult.error(() -> owner + " reads '" + this.setting
					+ "', which is not a setting declared before it");
			}
			for (String value : this.values) {
				if (!referenced.accepts(value)) {
					return DataResult.error(() -> owner + " compares '" + this.setting + "' with '" + value
						+ "', which is not one of its values");
				}
			}
			return DataResult.success(this);
		}
	}

	record PackTest(String pack, Operator operator, boolean value) implements GlowtonePackCondition {
		@Override
		public boolean test(Values values) {
			return this.operator.holds(values.enabled(this.pack) == this.value);
		}

		@Override
		public void references(Consumer<Reference> references) {}

		@Override
		public boolean external() {
			return true;
		}

		@Override
		public DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner) {
			return DataResult.success(this);
		}
	}

	record AllOf(List<GlowtonePackCondition> conditions) implements GlowtonePackCondition {
		@Override
		public boolean test(Values values) {
			for (GlowtonePackCondition condition : this.conditions) {
				if (!condition.test(values)) return false;
			}
			return true;
		}

		@Override
		public void references(Consumer<Reference> references) {
			for (GlowtonePackCondition condition : this.conditions) condition.references(references);
		}

		@Override
		public boolean external() {
			for (GlowtonePackCondition condition : this.conditions) {
				if (condition.external()) return true;
			}
			return false;
		}

		@Override
		public DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner) {
			return Filters.validateEach(this, this.conditions, declared, owner);
		}
	}

	record AnyOf(List<GlowtonePackCondition> conditions) implements GlowtonePackCondition {
		@Override
		public boolean test(Values values) {
			for (GlowtonePackCondition condition : this.conditions) {
				if (condition.test(values)) return true;
			}
			return false;
		}

		@Override
		public void references(Consumer<Reference> references) {
			for (GlowtonePackCondition condition : this.conditions) condition.references(references);
		}

		@Override
		public boolean external() {
			for (GlowtonePackCondition condition : this.conditions) {
				if (condition.external()) return true;
			}
			return false;
		}

		@Override
		public DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner) {
			return Filters.validateEach(this, this.conditions, declared, owner);
		}
	}

	record NoneOf(List<GlowtonePackCondition> conditions) implements GlowtonePackCondition {
		@Override
		public boolean test(Values values) {
			for (GlowtonePackCondition condition : this.conditions) {
				if (condition.test(values)) return false;
			}
			return true;
		}

		@Override
		public void references(Consumer<Reference> references) {
			for (GlowtonePackCondition condition : this.conditions) condition.references(references);
		}

		@Override
		public boolean external() {
			for (GlowtonePackCondition condition : this.conditions) {
				if (condition.external()) return true;
			}
			return false;
		}

		@Override
		public DataResult<GlowtonePackCondition> validate(Map<String, Setting> declared, String owner) {
			return Filters.validateEach(this, this.conditions, declared, owner);
		}
	}

	final class Filters implements Codec<GlowtonePackCondition> {
		private static final Codec<List<String>> VALUES = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
			either -> either.map(List::of, list -> list),
			list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list)
		);
		private static final List<String> GROUPS = List.of("all_of", "any_of", "none_of");

		static DataResult<GlowtonePackCondition> validateEach(
			GlowtonePackCondition owner,
			List<GlowtonePackCondition> conditions,
			Map<String, Setting> declared,
			String description
		) {
			for (GlowtonePackCondition condition : conditions) {
				final DataResult<GlowtonePackCondition> result = condition.validate(declared, description);
				if (result.isError()) return result;
			}
			return DataResult.success(owner);
		}

		@Override
		public <T> DataResult<Pair<GlowtonePackCondition, T>> decode(DynamicOps<T> ops, T input) {
			if (ops.getList(input).result().isPresent()) {
				return this.conditions(ops, input).map(list -> Pair.of(new AllOf(list), ops.empty()));
			}
			return ops.getMap(input)
				.flatMap(map -> this.decodeMap(ops, map))
				.map(condition -> Pair.of(condition, ops.empty()));
		}

		private <T> DataResult<GlowtonePackCondition> decodeMap(DynamicOps<T> ops, MapLike<T> map) {
			final T all = map.get("all_of");
			if (all != null) return this.conditions(ops, all).map(AllOf::new);

			final T any = map.get("any_of");
			if (any != null) return this.conditions(ops, any).map(AnyOf::new);

			final T none = map.get("none_of");
			if (none != null) return this.conditions(ops, none).map(NoneOf::new);

			final DataResult<Operator> operator = this.operator(ops, map);
			if (operator.isError()) return DataResult.error(operator.error().orElseThrow()::message);

			final T value = map.get("value");
			final T pack = map.get("pack");
			final T setting = map.get("setting");
			if (setting != null) {
				final DataResult<List<String>> values = value == null
					? DataResult.success(List.of("true"))
					: VALUES.parse(ops, value);
				final DataResult<Optional<String>> owner = pack == null
					? DataResult.success(Optional.empty())
					: ops.getStringValue(pack).map(Optional::of);
				return ops.getStringValue(setting).flatMap(id -> values.flatMap(list -> owner.flatMap(in ->
					operator.map(comparison -> new SettingTest(in, id, comparison, list)))));
			}

			if (pack != null) {
				final DataResult<Boolean> state = value == null
					? DataResult.success(true)
					: ops.getBooleanValue(value);
				return ops.getStringValue(pack).flatMap(id -> state.flatMap(enabled ->
					operator.map(comparison -> new PackTest(id, comparison, enabled))));
			}

			return DataResult.error(() -> "a condition needs 'setting' or 'pack', or one of " + GROUPS);
		}

		private <T> DataResult<Operator> operator(DynamicOps<T> ops, MapLike<T> map) {
			final T operator = map.get("operator");
			if (operator == null) return DataResult.success(Operator.EQUALS);

			return ops.getStringValue(operator).flatMap(name -> Operator.of(name)
				.map(DataResult::success)
				.orElseGet(() -> DataResult.error(() -> "unknown operator '" + name + "', expected == or !=")));
		}

		private <T> DataResult<List<GlowtonePackCondition>> conditions(DynamicOps<T> ops, T input) {
			return ops.getList(input).flatMap(entries -> {
				final List<GlowtonePackCondition> conditions = new ArrayList<>();
				final List<String> errors = new ArrayList<>();
				entries.accept(entry -> this.decode(ops, entry)
					.ifSuccess(pair -> conditions.add(pair.getFirst()))
					.ifError(error -> errors.add(error.message())));
				return errors.isEmpty()
					? DataResult.success(List.copyOf(conditions))
					: DataResult.error(() -> String.join("; ", errors));
			});
		}

		@Override
		public <T> DataResult<T> encode(GlowtonePackCondition input, DynamicOps<T> ops, T prefix) {
			return switch (input) {
				case AllOf all -> this.group(ops, "all_of", all.conditions());
				case AnyOf any -> this.group(ops, "any_of", any.conditions());
				case NoneOf none -> this.group(ops, "none_of", none.conditions());
				case PackTest pack -> DataResult.success(ops.createMap(Map.of(
					ops.createString("pack"), ops.createString(pack.pack()),
					ops.createString("operator"), ops.createString(pack.operator().getSerializedName()),
					ops.createString("value"), ops.createBoolean(pack.value())
				)));
				case SettingTest setting -> VALUES.encodeStart(ops, setting.values()).map(values -> {
					final Map<T, T> fields = new LinkedHashMap<>();
					setting.pack().ifPresent(pack -> fields.put(ops.createString("pack"), ops.createString(pack)));
					fields.put(ops.createString("setting"), ops.createString(setting.setting()));
					fields.put(ops.createString("operator"), ops.createString(setting.operator().getSerializedName()));
					fields.put(ops.createString("value"), values);
					return ops.createMap(fields);
				});
			};
		}

		private <T> DataResult<T> group(DynamicOps<T> ops, String key, List<GlowtonePackCondition> conditions) {
			return CODEC.listOf().encodeStart(ops, conditions)
				.map(list -> ops.createMap(Map.of(ops.createString(key), list)));
		}
	}
}
