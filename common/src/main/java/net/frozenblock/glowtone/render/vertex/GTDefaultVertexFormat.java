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

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Set;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

// TODO: self emission to offset tint with
@ClientOnly
public final class GTDefaultVertexFormat {
	public static final String CHROMA_SEMANTIC_NAME = "GlowtoneChroma";
	public static final String PIVOT_SEMANTIC_NAME = "GlowtonePivot";
	public static final String SKY_CHROMA_SEMANTIC_NAME = "GlowtoneSkyChroma";
	public static final String EDGE_SEMANTIC_NAME = "GlowtoneEdge";
	public static final String EDGE_MASK_SEMANTIC_NAME = "GlowtoneEdgeMask";
	public static final String CONTACT0_SEMANTIC_NAME = "GlowtoneContact0";
	public static final String CONTACT1_SEMANTIC_NAME = "GlowtoneContact1";
	public static final String CONTACT2_SEMANTIC_NAME = "GlowtoneContact2";
	public static final String CONTACT3_SEMANTIC_NAME = "GlowtoneContact3";

	public static final GlowtoneVertexLayout.Names NAMES = new GlowtoneVertexLayout.Names(
		CHROMA_SEMANTIC_NAME, SKY_CHROMA_SEMANTIC_NAME,
		EDGE_SEMANTIC_NAME, EDGE_MASK_SEMANTIC_NAME,
		CONTACT0_SEMANTIC_NAME, CONTACT1_SEMANTIC_NAME, CONTACT2_SEMANTIC_NAME, CONTACT3_SEMANTIC_NAME,
		null,
		PIVOT_SEMANTIC_NAME
	);
	private static final Set<String> GLOWTONE_NAMES = Set.of(
		CHROMA_SEMANTIC_NAME, SKY_CHROMA_SEMANTIC_NAME,
		EDGE_SEMANTIC_NAME, EDGE_MASK_SEMANTIC_NAME,
		CONTACT0_SEMANTIC_NAME, CONTACT1_SEMANTIC_NAME, CONTACT2_SEMANTIC_NAME, CONTACT3_SEMANTIC_NAME
	);

	private static volatile VertexFormat tinted = buildTinted(GlowtoneVertexFeatures.startup());

	public static VertexFormat tinted() {
		return tinted;
	}

	public static VertexFormat.Builder appendBlockAttributes(VertexFormat.Builder builder, GlowtoneVertexFeatures features) {
		appendChroma(builder, features);
		if (features.edges()) {
			builder.addAttribute(EDGE_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(EDGE_MASK_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT0_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT1_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT2_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT3_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		}
		return builder;
	}

	public static VertexFormat.Builder appendEntityAttributes(VertexFormat.Builder builder, GlowtoneVertexFeatures features) {
		return appendChroma(builder, features);
	}

	private static VertexFormat.Builder appendChroma(VertexFormat.Builder builder, GlowtoneVertexFeatures features) {
		if (features.chroma()) {
			builder.addAttribute(CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(SKY_CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		}
		if (features.pivot()) builder.addAttribute(PIVOT_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		return builder;
	}

	static VertexFormat rebuildBlock(VertexFormat current, GlowtoneVertexFeatures features) {
		final VertexFormat rebuilt = appendBlockAttributes(GlowtoneVertexFormats.base(current, GLOWTONE_NAMES), features).build();
		return GlowtoneVertexFormats.verified(current, rebuilt, GLOWTONE_NAMES);
	}

	static VertexFormat rebuildEntity(VertexFormat current, GlowtoneVertexFeatures features) {
		final VertexFormat rebuilt = appendEntityAttributes(GlowtoneVertexFormats.base(current, GLOWTONE_NAMES), features).build();
		return GlowtoneVertexFormats.verified(current, rebuilt, GLOWTONE_NAMES);
	}

	static VertexFormat rebuildTinted(GlowtoneVertexFeatures features) {
		final VertexFormat rebuilt = buildTinted(features);
		tinted = rebuilt;
		return rebuilt;
	}

	private static VertexFormat buildTinted(GlowtoneVertexFeatures features) {
		final VertexFormat.Builder builder = VertexFormat.builder(0)
			.addAttribute("Position", GpuFormat.RGB32_FLOAT)
			.addAttribute("Color", GpuFormat.RGBA8_UNORM)
			.addAttribute("UV2", GpuFormat.RG16_SINT);
		return appendChroma(builder, features).build();
	}

	public static GlowtoneVertexLayout layoutOf(VertexFormat format) {
		return GlowtoneVertexLayout.of(format, NAMES);
	}

	private GTDefaultVertexFormat() {}
}
