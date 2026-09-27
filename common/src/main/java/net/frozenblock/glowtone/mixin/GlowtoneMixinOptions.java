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

package net.frozenblock.glowtone.mixin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import net.frozenblock.lib.platform.ModLoader;

final class GlowtoneMixinOptions {
	private static final String FILE_NAME = "glowtone-mixins.properties";
	private static final String PREFIX = "mixin.";
	private static final String PACKAGE = "net.frozenblock.glowtone.mixin.";
	private static final String LINE = System.lineSeparator();
	private static final String[] GROUPS = {
		"block",
		"client.animation",
		"client.ao_edge",
		"client.block",
		"client.bloom",
		"client.color",
		"client.debug",
		"client.emissive",
		"client.material",
		"client.options",
		"client.sodium",
		"client.vertex"
	};

	private static final Map<String, Boolean> OVERRIDES = load();

	static boolean enabled(String mixinClassName) {
		if (OVERRIDES.isEmpty()) return true;

		String path = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
		while (true) {
			final Boolean override = OVERRIDES.get(path);
			if (override != null) return override;

			final int dot = path.lastIndexOf('.');
			if (dot < 0) return true;

			path = path.substring(0, dot);
		}
	}

	private static Map<String, Boolean> load() {
		try {
			final Path path = ModLoader.getConfigDir().resolve(FILE_NAME);
			if (!Files.exists(path)) {
				write(path);
				return Map.of();
			}

			final Properties properties = new Properties();
			try (Reader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			}

			return read(properties);
		} catch (IOException | RuntimeException failure) {
			log(System.Logger.Level.WARNING, "Glowtone could not read " + FILE_NAME + ", applying every mixin: " + failure);
			return Map.of();
		}
	}

	private static Map<String, Boolean> read(Properties properties) {
		final Map<String, Boolean> overrides = new LinkedHashMap<>();
		for (String key : properties.stringPropertyNames()) {
			final String value = properties.getProperty(key).trim();
			if (!key.startsWith(PREFIX)) {
				log(System.Logger.Level.WARNING, "Glowtone ignored '" + key + "' in " + FILE_NAME + ": keys start with '" + PREFIX + "'");
				continue;
			}
			if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
				log(System.Logger.Level.WARNING, "Glowtone ignored '" + key + "' in " + FILE_NAME + ": '" + value + "' is not true or false");
				continue;
			}

			overrides.put(key.substring(PREFIX.length()), Boolean.parseBoolean(value));
		}

		overrides.forEach((key, enabled) -> {
			if (!enabled) log(System.Logger.Level.INFO, "Glowtone is not applying its " + key + " mixins, disabled in " + FILE_NAME);
		});
		return overrides;
	}

	private static void write(Path path) throws IOException {
		final Path parent = path.getParent();
		if (parent != null) Files.createDirectories(parent);

		try (Writer writer = Files.newBufferedWriter(path)) {
			writer.write("# Glowtone mixin overrides." + LINE);
			writer.write("#" + LINE);
			writer.write("# Set an entry to false to stop Glowtone applying that group of mixins." + LINE);
			writer.write("# This is for working around potential conflicts with other mods." + LINE);
			writer.write(LINE);
			for (String group : GROUPS) writer.write("#" + PREFIX + group + "=true" + LINE);
		}
	}

	private static void log(System.Logger.Level level, String message) {
		System.getLogger("Glowtone").log(level, message);
	}

	private GlowtoneMixinOptions() {}
}
