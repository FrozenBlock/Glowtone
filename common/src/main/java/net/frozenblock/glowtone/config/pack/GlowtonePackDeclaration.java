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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.frozenblock.glowtone.config.pack.GlowtonePackCondition.Reference;
import net.frozenblock.glowtone.config.pack.GlowtonePackCondition.Values;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.jspecify.annotations.Nullable;

@ClientOnly
public record GlowtonePackDeclaration(List<Group> groups, List<SubpackRule> subpacks, List<Setting> settings, Map<String, Setting> byId) {
	public static final int MAX_BUTTON_VALUES = 8;
	public static final int MAX_SLIDER_STEPS = 128;
	private static final String CATEGORY = "category";
	private static final String ENTRIES = "entries";
	private static final Pattern ID = Pattern.compile("[a-z0-9_]+");
	private static final Pattern DIRECTORY = Pattern.compile("[-_a-zA-Z0-9.]+");
	private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> ID.matcher(id).matches()
		? DataResult.success(id)
		: DataResult.error(() -> "'" + id + "' is not a valid id, use lowercase letters, digits and underscores"));
	private static final Codec<String> DIRECTORY_CODEC = Codec.STRING.validate(directory -> DIRECTORY.matcher(directory).matches()
		? DataResult.success(directory)
		: DataResult.error(() -> "'" + directory + "' is not a valid subpack directory name"));
	private static final Map<String, MapCodec<? extends Body>> TYPES = Map.of(
		"button", Button.CODEC,
		"slider", Slider.CODEC
	);
	private static final Codec<String> TYPE_CODEC = Codec.STRING.validate(type -> TYPES.containsKey(type)
		? DataResult.success(type)
		: DataResult.error(() -> "unknown setting type '" + type + "', expected one of " + TYPES.keySet()));
	private static final Codec<Body> BODY_CODEC = Codec.lazyInitialized(() -> TYPE_CODEC.dispatch("type", Body::type, TYPES::get));

	public static final Codec<GlowtonePackDeclaration> CODEC = RecordCodecBuilder.<GlowtonePackDeclaration>create(instance -> instance.group(
		Group.CODEC.listOf().optionalFieldOf("settings", List.of()).forGetter(GlowtonePackDeclaration::groups),
		SubpackRule.CODEC.listOf().optionalFieldOf("subpacks", List.of()).forGetter(GlowtonePackDeclaration::subpacks)
	).apply(instance, GlowtonePackDeclaration::new)).validate(GlowtonePackDeclaration::validate);

	public static final MetadataSectionType<GlowtonePackDeclaration> TYPE = new MetadataSectionType<>("glowtone", CODEC);

	private static DataResult<GlowtonePackDeclaration> validate(GlowtonePackDeclaration declaration) {
		final Map<String, Setting> declared = new LinkedHashMap<>();
		for (Setting setting : declaration.settings()) {
			final String owner = "setting '" + setting.id() + "'";
			final DataResult<?> condition = setting.requires().validate(declared, owner);
			if (condition.isError()) return DataResult.error(condition.error().orElseThrow()::message);

			if (declared.putIfAbsent(setting.id(), setting) != null) {
				return DataResult.error(() -> "setting id '" + setting.id() + "' is declared twice");
			}
		}

		for (SubpackRule rule : declaration.subpacks) {
			final DataResult<?> condition = rule.validate(declared);
			if (condition.isError()) return DataResult.error(condition.error().orElseThrow()::message);
		}
		return DataResult.success(declaration);
	}

	public GlowtonePackDeclaration(List<Group> groups, List<SubpackRule> subpacks) {
		this(groups, subpacks, flatten(groups));
	}

	private GlowtonePackDeclaration(List<Group> groups, List<SubpackRule> subpacks, List<Setting> settings) {
		this(groups, subpacks, settings, index(settings));
	}

	private static List<Setting> flatten(List<Group> groups) {
		final List<Setting> settings = new ArrayList<>();
		for (Group group : groups) settings.addAll(group.settings());
		return List.copyOf(settings);
	}

	private static Map<String, Setting> index(List<Setting> settings) {
		final Map<String, Setting> byId = new LinkedHashMap<>(settings.size());
		for (Setting setting : settings) byId.putIfAbsent(setting.id(), setting);
		return Map.copyOf(byId);
	}

	public @Nullable Setting setting(String id) {
		return this.byId.get(id);
	}

	public Map<String, String> active(Function<Setting, String> value, Environment environment) {
		final Map<String, String> active = new LinkedHashMap<>();
		final Values values = values(active, environment);
		for (Setting setting : this.settings()) {
			if (setting.requires().test(values)) active.put(setting.id(), value.apply(setting));
		}
		return active;
	}

	public boolean satisfied(Setting setting, Function<Setting, String> value, Environment environment) {
		final Set<String> relevant = new HashSet<>(this.dependencies(setting));
		final Map<String, String> active = new LinkedHashMap<>();
		final Values values = values(active, environment);
		for (Setting other : this.settings()) {
			if (other.id().equals(setting.id())) break;
			if (!relevant.contains(other.id())) continue;

			if (other.requires().test(values)) active.put(other.id(), value.apply(other));
		}
		return setting.requires().test(values);
	}

	public List<String> activeSubpacks(Map<String, String> active, Environment environment) {
		final List<String> directories = new ArrayList<>();
		final Values values = values(active, environment);
		for (SubpackRule rule : this.subpacks) {
			rule.directory(values).ifPresent(directory -> {
				if (!directories.contains(directory)) directories.add(directory);
			});
		}
		return directories;
	}

	public List<String> dependencies(Setting setting) {
		final List<String> ids = new ArrayList<>();
		final Deque<String> pending = new ArrayDeque<>();
		local(setting, pending);
		while (!pending.isEmpty()) {
			final String id = pending.poll();
			if (ids.contains(id)) continue;

			ids.add(id);
			final Setting referenced = this.setting(id);
			if (referenced != null) local(referenced, pending);
		}
		return ids;
	}

	public boolean external(Setting setting) {
		if (setting.requires().external()) return true;

		for (String id : this.dependencies(setting)) {
			final Setting referenced = this.setting(id);
			if (referenced != null && referenced.requires().external()) return true;
		}
		return false;
	}

	public List<Reference> foreign(Setting setting) {
		final List<Reference> references = new ArrayList<>();
		final List<String> visited = new ArrayList<>(this.dependencies(setting));
		visited.add(setting.id());
		for (String id : visited) {
			final Setting referenced = this.setting(id);
			if (referenced == null) continue;

			referenced.requires().references(reference -> {
				if (reference.pack().isPresent() && !references.contains(reference)) references.add(reference);
			});
		}
		return references;
	}

	private static void local(Setting setting, Deque<String> pending) {
		setting.requires().references(reference -> {
			if (reference.pack().isEmpty()) pending.add(reference.setting());
		});
	}

	private static Values values(Map<String, String> active, Environment environment) {
		return new Values() {
			@Override
			public @Nullable String setting(String id) {
				return active.get(id);
			}

			@Override
			public @Nullable String setting(String packId, String id) {
				return environment.stored(packId, id);
			}

			@Override
			public boolean enabled(String packId) {
				return environment.enabled(packId);
			}
		};
	}

	static Component fallbackName(Optional<String> key, String id) {
		final String pretty = pretty(id);
		return key.map(k -> Component.translatableWithFallback(k, pretty)).orElseGet(() -> Component.literal(pretty));
	}

	private static String pretty(String id) {
		final StringBuilder builder = new StringBuilder(id.length());
		boolean capitalise = true;
		for (char character : id.toCharArray()) {
			if (character == '_') {
				builder.append(' ');
				capitalise = true;
				continue;
			}
			builder.append(capitalise ? Character.toUpperCase(character) : character);
			capitalise = false;
		}
		return builder.toString();
	}

	public interface Environment {
		@Nullable String stored(String packId, String settingId);

		boolean enabled(String packId);
	}

	public record Group(Optional<String> category, List<Setting> settings) {
		static final Codec<Group> CODEC = Codec.lazyInitialized(() -> new Codec<>() {
			@Override
			public <T> DataResult<Pair<Group, T>> decode(DynamicOps<T> ops, T input) {
				return ops.getMap(input).flatMap(map -> decodeGroup(ops, map)).map(group -> Pair.of(group, ops.empty()));
			}

			@Override
			public <T> DataResult<T> encode(Group input, DynamicOps<T> ops, T prefix) {
				final Map<T, T> entries = new LinkedHashMap<>();
				for (Setting setting : input.settings()) {
					final DataResult<T> body = BODY_CODEC.encodeStart(ops, setting.body());
					if (body.isError()) return DataResult.error(body.error().orElseThrow()::message);

					entries.put(ops.createString(setting.id()), body.getOrThrow());
				}

				final Map<T, T> fields = new LinkedHashMap<>();
				input.category().ifPresent(name -> fields.put(ops.createString(CATEGORY), ops.createString(name)));
				fields.put(ops.createString(ENTRIES), ops.createMap(entries));
				return DataResult.success(ops.createMap(fields));
			}
		});

		public Optional<Component> name() {
			return this.category.map(Component::translatable);
		}
	}

	private static <T> DataResult<Group> decodeGroup(DynamicOps<T> ops, MapLike<T> map) {
		final T entries = map.get(ENTRIES);
		if (entries == null) {
			return DataResult.error(() -> "a settings block needs an '" + ENTRIES + "' object holding its settings by id");
		}

		final DataResult<Optional<String>> category = map.get(CATEGORY) == null
			? DataResult.success(Optional.empty())
			: ops.getStringValue(map.get(CATEGORY)).map(Optional::of);
		if (category.isError()) return DataResult.error(() -> "'" + CATEGORY + "' must be a translation key");

		return ops.getMap(entries)
			.flatMap(fields -> decodeEntries(ops, fields))
			.flatMap(settings -> category.map(name -> new Group(name, settings)));
	}

	private static <T> DataResult<List<Setting>> decodeEntries(DynamicOps<T> ops, MapLike<T> entries) {
		final List<Setting> settings = new ArrayList<>();
		final List<String> errors = new ArrayList<>();

		entries.entries().forEach(entry -> {
			final DataResult<String> key = ops.getStringValue(entry.getFirst());
			if (key.isError()) {
				errors.add("a setting id must be a string");
				return;
			}

			final String id = key.getOrThrow();
			if (!ID.matcher(id).matches()) {
				errors.add("'" + id + "' is not a valid setting id, use lowercase letters, digits and underscores");
				return;
			}

			final DataResult<MapLike<T>> body = ops.getMap(entry.getSecond());
			if (body.isError() || body.getOrThrow().get("type") == null) {
				errors.add("setting '" + id + "' needs a 'type', one of " + TYPES.keySet());
				return;
			}

			BODY_CODEC.parse(ops, entry.getSecond())
				.ifSuccess(parsed -> settings.add(new Setting(id, parsed)))
				.ifError(error -> errors.add("setting '" + id + "': " + error.message()));
		});

		if (!errors.isEmpty()) return DataResult.error(() -> String.join("; ", errors));
		if (settings.isEmpty()) return DataResult.error(() -> "a settings block declares no settings");

		return DataResult.success(List.copyOf(settings));
	}

	public record Range(double min, double max) {}

	public record Value(Optional<String> name, Optional<String> tooltip) {
		static final Value EMPTY = new Value(Optional.empty(), Optional.empty());
		static final Codec<Value> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").forGetter(Value::name),
			Codec.STRING.optionalFieldOf("tooltip").forGetter(Value::tooltip)
		).apply(instance, Value::new));
	}

	public record Mount(String directory, GlowtonePackCondition when) {
		static final Codec<Mount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			DIRECTORY_CODEC.fieldOf("directory").forGetter(Mount::directory),
			GlowtonePackCondition.CODEC.optionalFieldOf("when", GlowtonePackCondition.ALWAYS).forGetter(Mount::when)
		).apply(instance, Mount::new));
	}

	public record SubpackRule(List<Mount> options) {
		static final Codec<SubpackRule> CODEC = Codec.either(
			RecordCodecBuilder.<SubpackRule>create(instance -> instance.group(
				Mount.CODEC.listOf().fieldOf("one_of").forGetter(SubpackRule::options)
			).apply(instance, SubpackRule::new)),
			Mount.CODEC.xmap(mount -> new SubpackRule(List.of(mount)), rule -> rule.options().getFirst())
		).xmap(
			either -> either.map(Function.identity(), Function.identity()),
			rule -> rule.options().size() == 1 ? Either.right(rule) : Either.left(rule)
		);

		public Optional<String> directory(Values values) {
			for (Mount mount : this.options) {
				if (mount.when().test(values)) return Optional.of(mount.directory());
			}
			return Optional.empty();
		}

		DataResult<?> validate(Map<String, Setting> declared) {
			for (Mount mount : this.options) {
				final DataResult<?> result = mount.when().validate(declared, "subpack '" + mount.directory() + "'");
				if (result.isError()) return result;
			}
			return DataResult.success(this);
		}
	}

	public record Setting(String id, Body body) {
		public Optional<String> name() {
			return this.body.name();
		}

		public Optional<String> tooltip() {
			return this.body.tooltip();
		}

		public List<String> valueIds() {
			return this.body.valueIds();
		}

		public String defaultValue() {
			return this.body.defaultValue();
		}

		public Optional<GlowtonePackImpact> impact() {
			return this.body.impact();
		}

		public GlowtonePackCondition requires() {
			return this.body.requires();
		}

		public boolean accepts(String value) {
			return this.body.accepts(value);
		}

		public boolean isToggle() {
			return this.body instanceof Button button && button.values().isEmpty();
		}

		public Component displayName() {
			return fallbackName(this.name(), this.id);
		}

		public Optional<String> valueKey(String value) {
			return this.body.values().getOrDefault(value, Value.EMPTY).name();
		}

		public Optional<String> tooltipKey(String value) {
			return this.body.values().getOrDefault(value, Value.EMPTY).tooltip().or(this::tooltip);
		}
	}

	public sealed interface Body permits Button, Slider {
		Optional<String> name();

		Optional<String> tooltip();

		Map<String, Value> values();

		Optional<GlowtonePackImpact> impact();

		GlowtonePackCondition requires();

		String type();

		String defaultValue();

		List<String> valueIds();

		boolean accepts(String value);
	}

	public record Button(
		Optional<String> name,
		Optional<String> tooltip,
		Map<String, Value> values,
		String defaultValue,
		Optional<GlowtonePackImpact> impact,
		GlowtonePackCondition requires
	) implements Body {
		private static final List<String> STATES = List.of("true", "false");
		private static final Codec<String> DEFAULT = Codec.either(Codec.BOOL, Codec.STRING)
			.xmap(either -> either.map(String::valueOf, value -> value), Either::right);

		static final MapCodec<Button> CODEC = RecordCodecBuilder.<Button>mapCodec(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").forGetter(Button::name),
			Codec.STRING.optionalFieldOf("tooltip").forGetter(Button::tooltip),
			Codec.unboundedMap(ID_CODEC, Value.CODEC).optionalFieldOf("values", Map.of()).forGetter(Button::values),
			DEFAULT.optionalFieldOf("default", "true").forGetter(Button::defaultValue),
			GlowtonePackImpact.CODEC.optionalFieldOf("impact").forGetter(Button::impact),
			GlowtonePackCondition.CODEC.optionalFieldOf("requires", GlowtonePackCondition.ALWAYS).forGetter(Button::requires)
		).apply(instance, Button::new)).validate(Button::validate);

		private static DataResult<Button> validate(Button button) {
			if (button.values.size() == 1) {
				return DataResult.error(() -> "a button needs at least two values, or none at all to be an on/off switch");
			}
			if (button.values.size() > MAX_BUTTON_VALUES) {
				return DataResult.error(() -> "a button cycles through at most " + MAX_BUTTON_VALUES
					+ " values, this one has " + button.values.size());
			}
			if (!button.valueIds().contains(button.defaultValue)) {
				return DataResult.error(() -> "the default '" + button.defaultValue + "' is not one of " + button.valueIds());
			}
			return DataResult.success(button);
		}

		@Override
		public String type() {
			return "button";
		}

		@Override
		public List<String> valueIds() {
			return this.values.isEmpty() ? STATES : List.copyOf(this.values.keySet());
		}

		@Override
		public boolean accepts(String value) {
			return this.values.isEmpty() ? STATES.contains(value) : this.values.containsKey(value);
		}
	}

	public record Slider(
		Optional<String> name,
		Optional<String> tooltip,
		Map<String, Value> values,
		int steps,
		double min,
		double max,
		double defaultNumber,
		Optional<GlowtonePackImpact> impact,
		GlowtonePackCondition requires
	) implements Body {
		private static final Codec<Range> RANGE = RecordCodecBuilder.create(instance -> instance.group(
			Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
			Codec.DOUBLE.fieldOf("max").forGetter(Range::max)
		).apply(instance, Range::new));

		static final MapCodec<Slider> CODEC = RecordCodecBuilder.<Slider>mapCodec(instance -> instance.group(
			Codec.STRING.optionalFieldOf("name").forGetter(Slider::name),
			Codec.STRING.optionalFieldOf("tooltip").forGetter(Slider::tooltip),
			Codec.unboundedMap(Codec.STRING, Value.CODEC).optionalFieldOf("values", Map.of()).forGetter(Slider::values),
			Codec.INT.fieldOf("steps").forGetter(Slider::steps),
			RANGE.fieldOf("range").forGetter(slider -> new Range(slider.min(), slider.max())),
			Codec.DOUBLE.fieldOf("default").forGetter(Slider::defaultNumber),
			GlowtonePackImpact.CODEC.optionalFieldOf("impact").forGetter(Slider::impact),
			GlowtonePackCondition.CODEC.optionalFieldOf("requires", GlowtonePackCondition.ALWAYS).forGetter(Slider::requires)
		).apply(instance, (name, tooltip, values, steps, range, fallback, impact, requires) ->
			new Slider(name, tooltip, values, steps, range.min(), range.max(), fallback, impact, requires)
		)).validate(Slider::validate);

		private static DataResult<Slider> validate(Slider slider) {
			if (slider.steps < 2 || slider.steps > MAX_SLIDER_STEPS) {
				return DataResult.error(() -> "a slider needs between 2 and " + MAX_SLIDER_STEPS
					+ " steps, this one has " + slider.steps);
			}
			if (slider.max <= slider.min) {
				return DataResult.error(() -> "a slider needs a range whose max is above its min, this one is "
					+ slider.min + " to " + slider.max);
			}
			for (String value : slider.values.keySet()) {
				if (!slider.valueIds().contains(value)) {
					return DataResult.error(() -> "the slider describes '" + value + "', which is not one of its steps "
						+ slider.valueIds());
				}
			}
			final double offset = (slider.defaultNumber - slider.min) * (slider.steps - 1) / (slider.max - slider.min);
			if (offset < 0D || offset > slider.steps - 1 || Math.abs(offset - Math.rint(offset)) > 1.0E-6D) {
				return DataResult.error(() -> "the default " + slider.defaultNumber + " is not one of the steps "
					+ slider.valueIds());
			}
			return DataResult.success(slider);
		}

		public String value(int step) {
			return format(this.min + (this.max - this.min) * step / (this.steps - 1));
		}

		public int step(String value) {
			try {
				return this.nearest(Double.parseDouble(value));
			} catch (NumberFormatException notANumber) {
				return 0;
			}
		}

		private int nearest(double number) {
			final double offset = (number - this.min) * (this.steps - 1) / (this.max - this.min);
			return (int) Math.clamp(Math.rint(offset), 0D, this.steps - 1);
		}

		@Override
		public String type() {
			return "slider";
		}

		@Override
		public String defaultValue() {
			return this.value(this.nearest(this.defaultNumber));
		}

		@Override
		public List<String> valueIds() {
			final List<String> ids = new ArrayList<>(this.steps);
			for (int step = 0; step < this.steps; step++) ids.add(this.value(step));
			return ids;
		}

		@Override
		public boolean accepts(String value) {
			final double number;
			try {
				number = Double.parseDouble(value);
			} catch (NumberFormatException notANumber) {
				return false;
			}

			final double offset = (number - this.min) * (this.steps - 1) / (this.max - this.min);
			return offset >= 0D && offset <= this.steps - 1
				&& Math.abs(offset - Math.rint(offset)) < 1.0E-6D
				&& this.value((int) Math.rint(offset)).equals(value);
		}

		private static String format(double value) {
			final double rounded = Math.round(value * 1000D) / 1000D;
			return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
		}
	}
}
