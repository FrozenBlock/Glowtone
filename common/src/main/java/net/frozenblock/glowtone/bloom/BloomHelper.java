/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.bloom;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.lighting.LightEngine;

@ClientOnly
public final class BloomHelper {
	public static final int EMISSIVE_MARKER = 0x1000;
	public static final int LIGHT_COORDS_CHANNEL_MASK = 0xFF;

	public static boolean isEmissiveQuad(BakedQuad quad) {
		final BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
		return isEmissiveLevel(materialInfo.lightEmission());
	}

	public static boolean isEmissiveLevel(int lightEmission) {
		return lightEmission >= LightEngine.MAX_LEVEL;
	}

	public static int unmark(int lightCoords) {
		return lightCoords & ~EMISSIVE_MARKER;
	}

	public static int mark(int lightCoords) {
		return lightCoords | EMISSIVE_MARKER;
	}

	private BloomHelper() {}
}
