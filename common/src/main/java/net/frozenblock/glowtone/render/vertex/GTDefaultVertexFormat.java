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

package net.frozenblock.glowtone.render.vertex;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import com.mojang.blaze3d.GpuFormat;

// TODO: self emission to offset tint with
@ClientOnly
public final class GTDefaultVertexFormat {
	public static final String CHROMA_SEMANTIC_NAME = "GlowtoneChroma";
	public static final String SKY_CHROMA_SEMANTIC_NAME = "GlowtoneSkyChroma";
	public static final String EDGE_SEMANTIC_NAME = "GlowtoneEdge";
	public static final String EDGE_MASK_SEMANTIC_NAME = "GlowtoneEdgeMask";
	public static final String CONTACT0_SEMANTIC_NAME = "GlowtoneContact0";
	public static final String CONTACT1_SEMANTIC_NAME = "GlowtoneContact1";
	public static final String CONTACT2_SEMANTIC_NAME = "GlowtoneContact2";
	public static final String CONTACT3_SEMANTIC_NAME = "GlowtoneContact3";

	public static long POSITION_OFFSET_BLOCK;
	public static long CHROMA_OFFSET_BLOCK;
	public static long SKY_CHROMA_OFFSET_BLOCK;
	public static long EDGE_OFFSET_BLOCK;
	public static long EDGE_MASK_OFFSET_BLOCK;
	public static long CONTACT0_OFFSET_BLOCK;
	public static long CONTACT1_OFFSET_BLOCK;
	public static long CONTACT2_OFFSET_BLOCK;
	public static long CONTACT3_OFFSET_BLOCK;

	public static long POSITION_OFFSET_ENTITY;
	public static long CHROMA_OFFSET_ENTITY;
	public static long SKY_CHROMA_OFFSET_ENTITY;

	public static VertexFormat.Builder appendBlockAttributes(VertexFormat.Builder builder) {
		builder.addAttribute(CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(SKY_CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_MASK_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT0_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT1_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT2_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT3_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		return builder;
	}

	public static void setupBlockOffsets(VertexFormat format) {
		POSITION_OFFSET_BLOCK = format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
		CHROMA_OFFSET_BLOCK = format.getElement(CHROMA_SEMANTIC_NAME).offset();
		SKY_CHROMA_OFFSET_BLOCK = format.getElement(SKY_CHROMA_SEMANTIC_NAME).offset();
		EDGE_OFFSET_BLOCK = format.getElement(EDGE_SEMANTIC_NAME).offset();
		EDGE_MASK_OFFSET_BLOCK = format.getElement(EDGE_MASK_SEMANTIC_NAME).offset();
		CONTACT0_OFFSET_BLOCK = format.getElement(CONTACT0_SEMANTIC_NAME).offset();
		CONTACT1_OFFSET_BLOCK = format.getElement(CONTACT1_SEMANTIC_NAME).offset();
		CONTACT2_OFFSET_BLOCK = format.getElement(CONTACT2_SEMANTIC_NAME).offset();
		CONTACT3_OFFSET_BLOCK = format.getElement(CONTACT3_SEMANTIC_NAME).offset();
	}

	public static VertexFormat.Builder appendEntityAttributes(VertexFormat.Builder builder) {
		builder.addAttribute(CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(SKY_CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		return builder;
	}

	public static void setupEntityOffsets(VertexFormat format) {
		POSITION_OFFSET_ENTITY = format.getElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME).offset();
		CHROMA_OFFSET_ENTITY = format.getElement(CHROMA_SEMANTIC_NAME).offset();
		SKY_CHROMA_OFFSET_ENTITY = format.getElement(SKY_CHROMA_SEMANTIC_NAME).offset();
	}

	private GTDefaultVertexFormat() {}
}
