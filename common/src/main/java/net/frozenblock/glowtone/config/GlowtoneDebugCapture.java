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

import com.mojang.logging.LogUtils;
import net.frozenblock.glowtone.light.color.render.ColorProbe;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.frozenblock.lib.event.api.events.client.ClientTickEvents;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneDebugCapture {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int[] TICKS = ticks();
	private static final boolean EXIT = Boolean.parseBoolean(System.getenv("GLOWTONE_DEBUG_CAPTURE_EXIT"));
	private static int elapsed;
	private static int taken;
	private static final int[] PROBE = numbers(System.getenv("GLOWTONE_DEBUG_PROBE"));
	private static int quitDelay;

	private static int[] ticks() {
		final int[] parsed = numbers(System.getenv("GLOWTONE_DEBUG_CAPTURE"));
		for (int index = 0; index < parsed.length; index++) parsed[index] = Math.max(1, parsed[index]);
		java.util.Arrays.sort(parsed);
		return parsed;
	}

	private static int[] numbers(String configured) {
		if (configured == null) return new int[0];

		final String[] parts = configured.split(",");
		final int[] parsed = new int[parts.length];
		for (int index = 0; index < parts.length; index++) {
			try {
				parsed[index] = Integer.parseInt(parts[index].trim());
			} catch (NumberFormatException malformed) {
				return new int[0];
			}
		}
		return parsed;
	}

	public static void register() {
		if (TICKS.length == 0) return;

		LOGGER.info("Glowtone debug capture armed at ticks {} after entering a world", java.util.Arrays.toString(TICKS));
		ClientTickEvents.END_CLIENT_TICK.register(GlowtoneDebugCapture::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null) return;

		if (taken >= TICKS.length) {
			if (EXIT && ++quitDelay >= 20) minecraft.stop();
			return;
		}

		if (++elapsed < TICKS[taken]) return;

		final int at = TICKS[taken++];
		Screenshot.grab(minecraft, false);
		LOGGER.info("Glowtone debug capture {} of {} taken at tick {}", taken, TICKS.length, at);
		probe(minecraft);
	}

	private static void probe(Minecraft minecraft) {
		if (PROBE.length != 7) return;

		final int[] sources = GlowtoneDynamicLights.get().snapshot();
		LOGGER.info("GLOWTONE_SOURCES count={}", sources.length / GlowtoneDynamicLights.STRIDE);
		for (int at = 0; at < sources.length; at += GlowtoneDynamicLights.STRIDE) {
			LOGGER.info(
				"GLOWTONE_SOURCE block=({},{},{}) exact=({},{},{}) luminance={} rgb={}",
				sources[at], sources[at + 1], sources[at + 2],
				Float.intBitsToFloat(sources[at + 5]), Float.intBitsToFloat(sources[at + 6]), Float.intBitsToFloat(sources[at + 7]),
				sources[at + 3], Integer.toHexString(sources[at + 4])
			);
		}

		for (int step = 0; step < PROBE[6]; step++) {
			final int x = PROBE[0] + PROBE[3] * step;
			final int y = PROBE[1] + PROBE[4] * step;
			final int z = PROBE[2] + PROBE[5] * step;
			final BlockPos pos = new BlockPos(x, y, z);
			LOGGER.info(
				"GLOWTONE_PROBE {} {} {} packed={} vanillaBlock={} litBlock={}",
				x, y, z,
				ColorProbe.get().getPackedLevels(x, y, z),
				minecraft.level.getBrightness(LightLayer.BLOCK, pos),
				LightCoordsUtil.block(LightCoordsUtil.getLightCoords(minecraft.level, pos))
			);
		}
	}

	private GlowtoneDebugCapture() {}
}
