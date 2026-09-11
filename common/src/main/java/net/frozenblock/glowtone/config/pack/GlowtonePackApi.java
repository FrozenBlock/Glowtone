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

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtonePackApi {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Pattern REFERENCE = Pattern.compile("%([a-z0-9_]+)%");
	private static final List<Runnable> LISTENERS = new ArrayList<>();

	public static Optional<String> value(String packId, String settingId) {
		return Optional.ofNullable(values(packId).get(settingId));
	}

	public static boolean flag(String packId, String settingId) {
		return value(packId, settingId).filter("true"::equals).isPresent();
	}

	public static OptionalDouble number(String packId, String settingId) {
		final Optional<String> value = value(packId, settingId);
		if (value.isEmpty()) return OptionalDouble.empty();

		try {
			return OptionalDouble.of(Double.parseDouble(value.get()));
		} catch (NumberFormatException failure) {
			return OptionalDouble.empty();
		}
	}

	public static Map<String, String> values(String packId) {
		final GlowtonePackDeclaration declaration = GlowtonePackOptions.declaration(packId);
		if (declaration == null || !GlowtonePackOptions.selected(packId)) return Map.of();

		return GlowtonePackOptions.active(packId, declaration);
	}

	public static Map<String, String> defines(String packId) {
		final Map<String, String> defines = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : values(packId).entrySet()) {
			defines.put("GLOWTONE_PACK_" + entry.getKey().toUpperCase(Locale.ROOT), define(entry.getValue()));
		}
		return defines;
	}

	public static String resolve(String packId, String value) {
		if (value.indexOf('%') < 0) return value;

		final Matcher matcher = REFERENCE.matcher(value);
		final StringBuilder resolved = new StringBuilder();
		while (matcher.find()) {
			final String settingId = matcher.group(1);
			String current = GlowtonePackOptions.stored(packId, settingId);
			if (current == null) current = GlowtonePackOptions.anywhere(settingId);
			if (current == null) {
				LOGGER.error("Pack '{}' refers to the setting '{}', which no loaded pack declares, reading it as 0", packId, settingId);
				current = "0";
			}

			matcher.appendReplacement(resolved, Matcher.quoteReplacement(current));
		}
		matcher.appendTail(resolved);
		return resolved.toString();
	}

	public static Map<String, String> resolve(String packId, Map<String, String> values) {
		Map<String, String> resolved = null;
		for (Map.Entry<String, String> entry : values.entrySet()) {
			final String value = resolve(packId, entry.getValue());
			if (value.equals(entry.getValue())) continue;

			if (resolved == null) resolved = new LinkedHashMap<>(values);
			resolved.put(entry.getKey(), value);
		}
		return resolved == null ? values : Map.copyOf(resolved);
	}

	public static void onChange(Runnable listener) {
		LISTENERS.add(listener);
	}

	static void changed() {
		GlowtonePackOptions.invalidate();
		for (Runnable listener : LISTENERS) listener.run();
	}

	private static String define(String value) {
		if (value.equals("true")) return "1";
		if (value.equals("false")) return "0";

		try {
			Double.parseDouble(value);
			return value;
		} catch (NumberFormatException failure) {
			return value.toUpperCase(Locale.ROOT);
		}
	}

	private GlowtonePackApi() {}
}
