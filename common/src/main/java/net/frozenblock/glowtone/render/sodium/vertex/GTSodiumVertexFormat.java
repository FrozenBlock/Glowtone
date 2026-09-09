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

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.Set;
import net.frozenblock.glowtone.mixin.client.sodium.vertex.CompactChunkVertexAccessor;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFeatures;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFormats;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexLayout;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

// TODO: self emission to offset tint with
@ClientOnly
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

	public static final GlowtoneVertexLayout.Names NAMES = new GlowtoneVertexLayout.Names(
		CHROMA_SEMANTIC_NAME, SKY_CHROMA_SEMANTIC_NAME,
		EDGE_SEMANTIC_NAME, EDGE_MASK_SEMANTIC_NAME,
		CONTACT0_SEMANTIC_NAME, CONTACT1_SEMANTIC_NAME, CONTACT2_SEMANTIC_NAME, CONTACT3_SEMANTIC_NAME,
		FLAGS_SEMANTIC_NAME
	);
	private static final Set<String> GLOWTONE_NAMES = Set.of(
		CHROMA_SEMANTIC_NAME, SKY_CHROMA_SEMANTIC_NAME,
		EDGE_SEMANTIC_NAME, EDGE_MASK_SEMANTIC_NAME,
		CONTACT0_SEMANTIC_NAME, CONTACT1_SEMANTIC_NAME, CONTACT2_SEMANTIC_NAME, CONTACT3_SEMANTIC_NAME,
		FLAGS_SEMANTIC_NAME
	);

	private static volatile GlowtoneVertexLayout layout = GlowtoneVertexLayout.NONE;

	public static VertexFormat.Builder appendTerrainAttributes(VertexFormat.Builder builder, GlowtoneVertexFeatures features) {
		if (features.chroma()) {
			builder.addAttribute(CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(SKY_CHROMA_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		}
		if (features.edges()) {
			builder.addAttribute(EDGE_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(EDGE_MASK_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT0_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT1_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT2_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
			builder.addAttribute(CONTACT3_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		}
		if (features.flags()) builder.addAttribute(FLAGS_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM);
		return builder;
	}

	public static void setup(VertexFormat format) {
		layout = layoutOf(format);
	}

	public static GlowtoneVertexLayout currentLayout() {
		return layout;
	}

	public static GlowtoneVertexLayout layoutOf(VertexFormat format) {
		return GlowtoneVertexLayout.of(format, NAMES);
	}

	public static VertexFormat rebuild(GlowtoneVertexFeatures features, Map<VertexFormat, VertexFormat> replaced) {
		final VertexFormat current = CompactChunkVertexAccessor.glowtone$vertexFormat();
		final VertexFormat rebuilt = GlowtoneVertexFormats.verified(
			current,
			appendTerrainAttributes(GlowtoneVertexFormats.base(current, GLOWTONE_NAMES), features).build(),
			GLOWTONE_NAMES
		);
		CompactChunkVertexAccessor.glowtone$setVertexFormat(rebuilt);
		setup(rebuilt);
		replaced.put(current, rebuilt);
		return current;
	}

	private GTSodiumVertexFormat() {}
}
