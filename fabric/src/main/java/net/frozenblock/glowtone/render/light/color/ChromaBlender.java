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

package net.frozenblock.glowtone.render.light.color;

import net.frozenblock.glowtone.config.option.color.ColoredLightingMode;

public final class ChromaBlender {
	public static final long EMPTY = 0L;
	public static final int NEUTRAL_ARGB = 0xFFFFFFFF;
	public static final int NEUTRAL_TERRAIN_ARGB = 0xFF808080;
	public static final float CHROMA_SCALE = 2F;
	private static final float SUBTLE_SATURATION = 0.85F;
	private static final float SUBTLE_SKY_STRENGTH = 0.5F;
	private static final float SUBTLE_EQUALISE = 0.4F;

	private static volatile ColoredLightingMode mode = ColoredLightingMode.SUBTLE;

	public static void setMode(ColoredLightingMode value) {
		mode = value;
	}

	public static boolean isEnabled() {
		return mode.enabled();
	}

	public static int skyTintArgb(int rgb) {
		final ColoredLightingMode current = mode;
		if (current == ColoredLightingMode.OFF) return NEUTRAL_ARGB;
		if (current == ColoredLightingMode.INTENSE) return 0xFF000000 | rgb;

		return 0xFF000000
			| (towardWhite((rgb >> 16) & 0xFF) << 16)
			| (towardWhite((rgb >> 8) & 0xFF) << 8)
			| towardWhite(rgb & 0xFF);
	}

	private static int towardWhite(int channel) {
		return Math.round(255F - (255 - channel) * SUBTLE_SKY_STRENGTH);
	}

	public static ColoredLightingMode mode() {
		return mode;
	}

	private static final int FULL_TINT_LEVEL = 12;
	private static final float TARGET_LUMA = 0.8F;

	private static final float LUMA_RED = 0.2126F;
	private static final float LUMA_GREEN = 0.7152F;
	private static final float LUMA_BLUE = 0.0722F;

	private static final int SUM_BITS = 16;
	private static final long SUM_MASK = 0xFFFFL;
	private static final int STRONGEST_SHIFT = 48;
	private static final long SUMS_MASK = (1L << STRONGEST_SHIFT) - 1L;

	public static final int WEIGHT_ONE = 256;

	public static long addWeighted(long accumulator, int packedLevels, int weight) {
		if (packedLevels == 0 || weight <= 0) return accumulator;

		final int level = packedLevels & 0xF;
		if (level == 0) return accumulator;

		final int hue = (packedLevels >>> 4) & 0xFFF;
		final int scale = level * weight;
		final long red = (long) ((hue >> 8) & 0xF) * scale / WEIGHT_ONE;
		final long green = (long) ((hue >> 4) & 0xF) * scale / WEIGHT_ONE;
		final long blue = (long) (hue & 0xF) * scale / WEIGHT_ONE;
		if ((red | green | blue) == 0L) return accumulator;

		final long sums = (accumulator & SUMS_MASK)
			+ red
			+ (green << SUM_BITS)
			+ (blue << (SUM_BITS * 2));
		final long strongest = Math.max(accumulator >>> STRONGEST_SHIFT, level);
		return sums | (strongest << STRONGEST_SHIFT);
	}

	public static long add(long accumulator, int packedLevels) {
		if (packedLevels == 0) return accumulator;

		final int level = packedLevels & 0xF;
		if (level == 0) return accumulator;

		final int hue = (packedLevels >>> 4) & 0xFFF;
		final int red = ((hue >> 8) & 0xF) * level;
		final int green = ((hue >> 4) & 0xF) * level;
		final int blue = (hue & 0xF) * level;

		final long sums = (accumulator & SUMS_MASK)
			+ red
			+ ((long) green << SUM_BITS)
			+ ((long) blue << (SUM_BITS * 2));
		final long strongest = Math.max(accumulator >>> STRONGEST_SHIFT, level);
		return sums | (strongest << STRONGEST_SHIFT);
	}

	public static boolean isEmpty(long accumulator) {
		return (accumulator >>> STRONGEST_SHIFT) == 0L;
	}

	public static int toArgb(long accumulator) {
		return encode(accumulator, CHROMA_SCALE);
	}

	public static int toEntityArgb(long accumulator) {
		return encode(accumulator, 0F);
	}

	private static int encode(long accumulator, float headroom) {
		final int strongest = (int) (accumulator >>> STRONGEST_SHIFT);
		if (strongest == 0) return headroom > 0F ? NEUTRAL_TERRAIN_ARGB : NEUTRAL_ARGB;

		final int redSum = (int) (accumulator & SUM_MASK);
		final int greenSum = (int) ((accumulator >>> SUM_BITS) & SUM_MASK);
		final int blueSum = (int) ((accumulator >>> (SUM_BITS * 2)) & SUM_MASK);
		final float brightestSum = Math.max(redSum, Math.max(greenSum, blueSum));
		if (brightestSum <= 0F) return headroom > 0F ? NEUTRAL_TERRAIN_ARGB : NEUTRAL_ARGB;

		float red = redSum / brightestSum;
		float green = greenSum / brightestSum;
		float blue = blueSum / brightestSum;

		final float fade = Math.min(1F, strongest / (float) FULL_TINT_LEVEL);
		final boolean intense = mode == ColoredLightingMode.INTENSE;

		if (intense) {
			final float luma = LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue;
			if (luma > 0.001F) {
				float limit = headroom > 0F ? headroom : 1F;
				float scale = Math.min(TARGET_LUMA / luma, limit);
				red *= scale;
				green *= scale;
				blue *= scale;
			}
		} else {
			red = 1F + (red - 1F) * SUBTLE_SATURATION;
			green = 1F + (green - 1F) * SUBTLE_SATURATION;
			blue = 1F + (blue - 1F) * SUBTLE_SATURATION;

			final float luma = LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue;
			final float target = luma + (TARGET_LUMA - luma) * SUBTLE_EQUALISE;
			if (luma > target) {
				float scale = target / luma;
				red *= scale;
				green *= scale;
				blue *= scale;
			} else if (luma > 0.001F) {
				float mix = (target - luma) / (1F - luma);
				red += (1F - red) * mix;
				green += (1F - green) * mix;
				blue += (1F - blue) * mix;
			}
		}

		red = 1F + (red - 1F) * fade;
		green = 1F + (green - 1F) * fade;
		blue = 1F + (blue - 1F) * fade;

		if (headroom > 0F) {
			return 0xFF000000
				| (channelToByte(red / headroom) << 16)
				| (channelToByte(green / headroom) << 8)
				| channelToByte(blue / headroom);
		}

		final float brightest = Math.max(red, Math.max(green, blue));
		if (brightest > 1F) {
			red /= brightest;
			green /= brightest;
			blue /= brightest;
		}
		return 0xFF000000 | (channelToByte(red) << 16) | (channelToByte(green) << 8) | channelToByte(blue);
	}

	private static int channelToByte(float value) {
		return Math.clamp(Math.round(value * 255F), 0, 255);
	}

	private ChromaBlender() {}
}
