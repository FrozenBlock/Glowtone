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
import net.frozenblock.lib.event.api.events.client.ClientTickEvents;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneDebugCapture {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int TICKS = ticks();
	private static int elapsed;
	private static boolean captured;

	private static int ticks() {
		final String configured = System.getenv("GLOWTONE_DEBUG_CAPTURE");
		if (configured == null) return 0;

		try {
			return Math.max(1, Integer.parseInt(configured.trim()));
		} catch (NumberFormatException malformed) {
			return 200;
		}
	}

	public static void register() {
		if (TICKS <= 0) return;

		LOGGER.info("Glowtone debug capture armed for {} ticks after entering a world", TICKS);
		ClientTickEvents.END_CLIENT_TICK.register(GlowtoneDebugCapture::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (captured || minecraft.level == null || minecraft.player == null) return;
		if (++elapsed < TICKS) return;

		captured = true;
		LOGGER.info(
			"Glowtone debug capture: {} sections grouped by material so far"
		);
		Screenshot.grab(minecraft, false);
		LOGGER.info("Glowtone debug capture: screenshot written to the run screenshots directory");
	}

	private GlowtoneDebugCapture() {}
}
