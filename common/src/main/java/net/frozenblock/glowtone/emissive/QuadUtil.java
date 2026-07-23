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

package net.frozenblock.glowtone.emissive;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class QuadUtil {
	public static final int STRIDE = 8;
	public static final int U_OFFSET = 4;
	public static final int V_OFFSET = 5;

	public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to, boolean shade) {
		final int[] vertices = quad.getVertices().clone();
		for (int vertex = 0; vertex < 4; vertex++) {
			final int uIndex = vertex * STRIDE + U_OFFSET;
			final int vIndex = vertex * STRIDE + V_OFFSET;
			final float u = Float.intBitsToFloat(vertices[uIndex]);
			final float v = Float.intBitsToFloat(vertices[vIndex]);
			vertices[uIndex] = Float.floatToRawIntBits(to.getU(from.getUOffset(u)));
			vertices[vIndex] = Float.floatToRawIntBits(to.getV(from.getVOffset(v)));
		}
		return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), to, shade);
	}

	public static BakedQuad withShade(BakedQuad quad, boolean shade) {
		if (quad.isShade() == shade) return quad;
		return new BakedQuad(quad.getVertices(), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), shade);
	}

	private QuadUtil() {}
}
