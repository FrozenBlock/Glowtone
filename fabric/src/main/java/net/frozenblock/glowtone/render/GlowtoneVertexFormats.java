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

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.GpuFormat;

// TODO: self emission to offset tint with
@Environment(EnvType.CLIENT)
public final class GlowtoneVertexFormats {
	public static final String CHROMA_ELEMENT = "GlowtoneChroma";
	public static final String SKY_CHROMA_ELEMENT = "GlowtoneSkyChroma";
	public static final String EDGE_ELEMENT = "GlowtoneEdge";
	public static final String EDGE_MASK_ELEMENT = "GlowtoneEdgeMask";
	public static final String CONTACT0_ELEMENT = "GlowtoneContact0";
	public static final String CONTACT1_ELEMENT = "GlowtoneContact1";
	public static final String CONTACT2_ELEMENT = "GlowtoneContact2";
	public static final String CONTACT3_ELEMENT = "GlowtoneContact3";

	public static final VertexFormat EXTENDED_BLOCK = buildExtendedBlock();

	public static final long POSITION_OFFSET = EXTENDED_BLOCK.getElement("Position").offset();
	public static final long CHROMA_OFFSET = EXTENDED_BLOCK.getElement(CHROMA_ELEMENT).offset();
	public static final long SKY_CHROMA_OFFSET = EXTENDED_BLOCK.getElement(SKY_CHROMA_ELEMENT).offset();
	public static final long EDGE_OFFSET = EXTENDED_BLOCK.getElement(EDGE_ELEMENT).offset();
	public static final long EDGE_MASK_OFFSET = EXTENDED_BLOCK.getElement(EDGE_MASK_ELEMENT).offset();
	public static final long CONTACT0_OFFSET = EXTENDED_BLOCK.getElement(CONTACT0_ELEMENT).offset();
	public static final long CONTACT1_OFFSET = EXTENDED_BLOCK.getElement(CONTACT1_ELEMENT).offset();
	public static final long CONTACT2_OFFSET = EXTENDED_BLOCK.getElement(CONTACT2_ELEMENT).offset();
	public static final long CONTACT3_OFFSET = EXTENDED_BLOCK.getElement(CONTACT3_ELEMENT).offset();

	private static VertexFormat buildExtendedBlock() {
		final VertexFormat.Builder builder = VertexFormat.builder(DefaultVertexFormat.BLOCK.getStepRate());
		for (VertexFormatElement element : DefaultVertexFormat.BLOCK.getElements()) builder.addAttribute(element.name(), element.format());

		builder.addAttribute(CHROMA_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(SKY_CHROMA_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_MASK_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT0_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT1_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT2_ELEMENT, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT3_ELEMENT, GpuFormat.RGBA8_UNORM);
		return builder.build();
	}

	private GlowtoneVertexFormats() {}
}
