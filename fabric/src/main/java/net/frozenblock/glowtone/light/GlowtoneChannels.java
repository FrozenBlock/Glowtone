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

package net.frozenblock.glowtone.light;

final class GlowtoneChannels {
	static final int MAX_LEVEL = 15;
	static final int LEVEL_MASK = 0xFFFF;
	static final int WHITE_HUE = 0xFFF;

	private static final int HUE_SHIFT = 4;

	private GlowtoneChannels() {
		throw new UnsupportedOperationException("GlowtoneChannels is a static holder.");
	}

	static int pack(int level, int hue) {
		if (level <= 0) return 0;
		return (Math.min(level, MAX_LEVEL) & 0xF) | ((hue & WHITE_HUE) << HUE_SHIFT);
	}

	static int level(int packed) {
		return packed & 0xF;
	}

	static int hue(int packed) {
		return (packed >>> HUE_SHIFT) & WHITE_HUE;
	}

	static int max(int packed) {
		return level(packed);
	}

	static boolean anyGreater(int candidate, int current) {
		return level(candidate) > level(current);
	}

	static int subtract(int packed, int amount) {
		return pack(level(packed) - amount, hue(packed));
	}

	static int attenuate(int packed, int opacity, int filter) {
		int level = level(packed) - opacity;
		if (level <= 0) return 0;

		int hue = hue(packed);
		return pack(level, filter == WHITE_HUE ? hue : filterHue(hue, filter));
	}

	static int filterHue(int hue, int filter) {
		int red = ((hue >> 8) & 0xF) * ((filter >> 8) & 0xF) / MAX_LEVEL;
		int green = ((hue >> 4) & 0xF) * ((filter >> 4) & 0xF) / MAX_LEVEL;
		int blue = (hue & 0xF) * (filter & 0xF) / MAX_LEVEL;

		if ((red | green | blue) == 0) {
			return normalise((filter >> 8) & 0xF, (filter >> 4) & 0xF, filter & 0xF);
		}
		return normalise(red, green, blue);
	}

	static int emissionLevels(int emission, int rgb) {
		int level = Math.min(Math.max(emission, 0), MAX_LEVEL);
		if (level == 0) return 0;
		if (rgb < 0) return pack(level, WHITE_HUE);

		return pack(level, normalise(
				((rgb >> 16) & 0xFF) * MAX_LEVEL / 0xFF,
				((rgb >> 8) & 0xFF) * MAX_LEVEL / 0xFF,
				(rgb & 0xFF) * MAX_LEVEL / 0xFF
		));
	}

	static int merge(int a, int b) {
		int levelA = level(a);
		int levelB = level(b);
		if (levelB == 0) return a;
		if (levelA == 0) return b;

		int hueA = hue(a);
		int hueB = hue(b);
		int strongest = Math.max(levelA, levelB);
		if (hueA == hueB) return pack(strongest, hueA);

		return pack(strongest, hue(blendHues(a, b, levelA, levelB)));
	}

	static int blendHues(int a, int b, int weightA, int weightB) {
		int total = weightA + weightB;
		if (total <= 0) return a;

		int first = hue(a);
		int second = hue(b);
		return pack(level(a), normalise(
				((((first >> 8) & 0xF) * weightA) + (((second >> 8) & 0xF) * weightB)) / total,
				((((first >> 4) & 0xF) * weightA) + (((second >> 4) & 0xF) * weightB)) / total,
				(((first & 0xF) * weightA) + ((second & 0xF) * weightB)) / total
		));
	}

	static int toNormalisedRgb(int packed) {
		if (level(packed) == 0) return 0;

		int hue = hue(packed);
		return (expand((hue >> 8) & 0xF) << 16) | (expand((hue >> 4) & 0xF) << 8) | expand(hue & 0xF);
	}

	private static int normalise(int red, int green, int blue) {
		int brightest = Math.max(red, Math.max(green, blue));
		if (brightest <= 0) return WHITE_HUE;

		return ((red * MAX_LEVEL / brightest) << 8)
				| ((green * MAX_LEVEL / brightest) << 4)
				| (blue * MAX_LEVEL / brightest);
	}

	private static int expand(int channel) {
		return channel * 0x11;
	}
}
