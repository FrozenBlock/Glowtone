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
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.slf4j.Logger;
import java.util.Arrays;

@ClientOnly
public final class GlowtoneFrameProbe {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int FRAMES = frames();
	private static final long WARMUP_NANOS = 15_000_000_000L;

	private static long[] durations;
	private static int recorded;
	private static long firstFrame;
	private static long previous;
	private static boolean reported;

	private static int frames() {
		final String configured = System.getenv("GLOWTONE_FPS_PROBE");
		if (configured == null) return 0;

		try {
			return Math.max(30, Integer.parseInt(configured.trim()));
		} catch (NumberFormatException malformed) {
			return 600;
		}
	}

	public static boolean enabled() {
		return FRAMES > 0;
	}

	public static void frame() {
		if (FRAMES <= 0 || reported) return;

		final long now = System.nanoTime();
		final long last = previous;
		previous = now;
		if (last == 0L) return;

		if (firstFrame == 0L) firstFrame = now;
		if (now - firstFrame < WARMUP_NANOS) return;

		if (durations == null) durations = new long[FRAMES];
		if (recorded < FRAMES) {
			durations[recorded++] = now - last;
			return;
		}

		reported = true;
		report();
	}

	private static void report() {
		final long[] sorted = Arrays.copyOf(durations, recorded);
		Arrays.sort(sorted);

		final double median = sorted[sorted.length / 2] / 1_000_000.0;
		final double p95 = sorted[(int) (sorted.length * 0.95)] / 1_000_000.0;
		final double worst = sorted[sorted.length - 1] / 1_000_000.0;

		LOGGER.info(
			"Glowtone frame probe over {} frames: median {} ms ({} fps), p95 {} ms, worst {} ms; "
				+ "materials loaded {}",
			recorded,
			"%.3f".formatted(median),
			"%.0f".formatted(1000.0 / median),
			"%.3f".formatted(p95),
			"%.3f".formatted(worst),
			BlockMaterialRenderer.anyShaders()
		);
	}

	private GlowtoneFrameProbe() {}
}
