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

package net.frozenblock.glowtone.render.sodium.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.GpuFormat;

// TODO: self emission to offset tint with
@Environment(EnvType.CLIENT)
public final class GTSodiumVertexFormat {
	public static final String CHROMA_SEMANTIC_NAME = "a_GlowtoneChroma";
	public static final String SKY_CHROMA_SEMANTIC_NAME = "a_GlowtoneSkyChroma";
	public static final String EDGE_SEMANTIC_NAME = "a_GlowtoneEdge";
	public static final String EDGE_MASK_SEMANTIC_NAME = "a_GlowtoneEdgeMask";
	public static final String CONTACT0_SEMANTIC_NAME = "a_GlowtoneContact0";
	public static final String CONTACT1_SEMANTIC_NAME = "a_GlowtoneContact1";
	public static final String CONTACT2_SEMANTIC_NAME = "a_GlowtoneContact2";
	public static final String CONTACT3_SEMANTIC_NAME = "a_GlowtoneContact3";
	public static final String FLAGS_SEMANTIC_NAME = "a_GlowtoneFlags";

	public static long POSITION_OFFSET;
	public static long CHROMA_OFFSET;
	public static long SKY_CHROMA_OFFSET;
	public static long EDGE_OFFSET;
	public static long EDGE_MASK_OFFSET;
	public static long CONTACT0_OFFSET;
	public static long CONTACT1_OFFSET;
	public static long CONTACT2_OFFSET;
	public static long CONTACT3_OFFSET;
	public static long FLAGS_OFFSET;

	public static VertexFormat.Builder appendTerrainAttributes(VertexFormat.Builder builder) {
		builder.addAttribute(CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(SKY_CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(EDGE_MASK_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT0_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT1_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT2_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(CONTACT3_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		builder.addAttribute(FLAGS_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		return builder;
	}

	public static void setupOffsets(VertexFormat format) {
		POSITION_OFFSET = format.getElement("a_Position").offset();
		CHROMA_OFFSET = format.getElement(CHROMA_SEMANTIC_NAME).offset();
		SKY_CHROMA_OFFSET = format.getElement(SKY_CHROMA_SEMANTIC_NAME).offset();
		EDGE_OFFSET = format.getElement(EDGE_SEMANTIC_NAME).offset();
		EDGE_MASK_OFFSET = format.getElement(EDGE_MASK_SEMANTIC_NAME).offset();
		CONTACT0_OFFSET = format.getElement(CONTACT0_SEMANTIC_NAME).offset();
		CONTACT1_OFFSET = format.getElement(CONTACT1_SEMANTIC_NAME).offset();
		CONTACT2_OFFSET = format.getElement(CONTACT2_SEMANTIC_NAME).offset();
		CONTACT3_OFFSET = format.getElement(CONTACT3_SEMANTIC_NAME).offset();
		FLAGS_OFFSET = format.getElement(FLAGS_SEMANTIC_NAME).offset();
	}

	private GTSodiumVertexFormat() {}
}
