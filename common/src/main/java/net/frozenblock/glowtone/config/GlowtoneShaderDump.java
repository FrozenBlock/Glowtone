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

package net.frozenblock.glowtone.config;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@ClientOnly
public final class GlowtoneShaderDump {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final boolean ENABLED = Boolean.getBoolean("glowtone.dumpShaders")
		|| "true".equalsIgnoreCase(System.getenv("GLOWTONE_DUMP_SHADERS"));
	private static final Path DIRECTORY = Path.of("glowtone-shader-dump");
	private static boolean failed;

	public static boolean enabled() {
		return ENABLED;
	}

	public static String record(Identifier id, ShaderType type, String original, String patched) {
		if (!ENABLED || failed || patched.equals(original)) return patched;

		final String name = id.toString().replace(':', '_').replace('/', '_')
			+ (type == ShaderType.VERTEX ? ".vertex" : ".fragment");

		try {
			Files.createDirectories(DIRECTORY);
			Files.writeString(DIRECTORY.resolve(name), patched, StandardCharsets.UTF_8);
		} catch (Exception failure) {
			failed = true;
			LOGGER.warn("Glowtone could not write the shader dump, disabling it for this session", failure);
		}

		return patched;
	}

	private GlowtoneShaderDump() {}
}
