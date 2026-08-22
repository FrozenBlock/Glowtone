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

package net.frozenblock.glowtone.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class GlowtoneVertexFormats {
	public static final String CHROMA_ELEMENT = "GlowtoneChroma";
	public static final String SKY_CHROMA_ELEMENT = "GlowtoneSkyChroma";

	public static final VertexFormat EXTENDED_BLOCK = buildExtendedBlock();

	public static final long POSITION_OFFSET = EXTENDED_BLOCK.getElement("Position").offset();
	public static final long CHROMA_OFFSET = EXTENDED_BLOCK.getElement(CHROMA_ELEMENT).offset();
	public static final long SKY_CHROMA_OFFSET = EXTENDED_BLOCK.getElement(SKY_CHROMA_ELEMENT).offset();

	private static VertexFormat buildExtendedBlock() {
		var builder = VertexFormat.builder(DefaultVertexFormat.BLOCK.getStepRate());
		for (var element : DefaultVertexFormat.BLOCK.getElements()) {
			builder.addAttribute(element.name(), element.format());
		}
		builder.addAttribute(CHROMA_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(SKY_CHROMA_ELEMENT, GpuFormat.RGBA8_UNORM);
		return builder.build();
	}

	private GlowtoneVertexFormats() {
		throw new UnsupportedOperationException("GlowtoneVertexFormats only contains static definitions.");
	}
}
