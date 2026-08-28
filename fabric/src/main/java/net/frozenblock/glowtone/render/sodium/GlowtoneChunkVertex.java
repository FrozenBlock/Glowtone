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

package net.frozenblock.glowtone.render.sodium;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.frozenblock.glowtone.render.light.color.ChromaBaker;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.light.color.ChromaBlender;
import net.frozenblock.glowtone.render.light.edge.QuadEdges;
import org.lwjgl.system.MemoryUtil;

public final class GlowtoneChunkVertex implements ChunkVertexType {
	public static final int SODIUM_STRIDE = 20;
	public static final int CHROMA_OFFSET = 20;
	public static final int SKY_CHROMA_OFFSET = 24;
	public static final int EDGE_OFFSET = 28;
	public static final int EDGE_MASK_OFFSET = 32;
	public static final int CONTACT_OFFSET = 36;
	public static final int FLAGS_OFFSET = 52;
	public static final int STRIDE = 56;

	private static final int QUAD = 4;
	private static final int CONTACTS = 4;

	private static final VertexFormat FORMAT = VertexFormat.builder(0)
		.addAttribute("a_Position", GpuFormat.RG32_UINT)
		.addAttribute("a_Color", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_TexCoord", GpuFormat.RG16_UINT)
		.addAttribute("a_LightAndData", GpuFormat.RGBA8_UINT)
		.addAttribute("a_GlowtoneChroma", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneSkyChroma", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneEdge", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneEdgeMask", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneContact0", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneContact1", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneContact2", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneContact3", GpuFormat.RGBA8_UNORM)
		.addAttribute("a_GlowtoneFlags", GpuFormat.RGBA8_UNORM)
		.build();

	private static final int SKY_CHROMA_ABGR = toAbgr(ChromaBlender.NEUTRAL_ARGB);
	private static final int NO_EDGES_LE = Integer.reverseBytes(QuadEdges.NO_EDGES);
	private static final int[] NO_CONTACT_LE = noContact();

	private static int[] noContact() {
		final int[] packed = new int[CONTACTS];
		for (int contact = 0; contact < CONTACTS; contact++) {
			packed[contact] = Integer.reverseBytes(GlowtoneContactRects.NONE[contact]);
		}
		return packed;
	}

	private final ChunkVertexType delegate;

	public GlowtoneChunkVertex(ChunkVertexType delegate) {
		this.delegate = delegate;
	}

	@Override
	public VertexFormat getVertexFormat() {
		return FORMAT;
	}

	@Override
	public ChunkVertexEncoder getEncoder() {
		final ChunkVertexEncoder inner = this.delegate.getEncoder();

		return (pointer, material, vertices, sectionIndex) -> {
			final ChromaBaker.SectionState state = ChromaBaker.state();
			final long scratch = state.scratch(SODIUM_STRIDE * QUAD);
			inner.write(scratch, material, vertices, sectionIndex);

			final QuadEdges edges = state.pendingEdges();
			final boolean fluid = state.fluidQuad();
			final int flags = state.emissiveQuad() ? 0x000000FF : 0;

			long out = pointer;
			for (int vertex = 0; vertex < QUAD; vertex++) {
				final ChunkVertexEncoder.Vertex source = vertices[vertex];

				MemoryUtil.memCopy(scratch + (long) vertex * SODIUM_STRIDE, out, SODIUM_STRIDE);
				MemoryUtil.memPutInt(
					out + CHROMA_OFFSET, toAbgr(state.sample(source.x, source.y, source.z)));
				MemoryUtil.memPutInt(out + SKY_CHROMA_OFFSET, SKY_CHROMA_ABGR);
				MemoryUtil.memPutInt(out + FLAGS_OFFSET, flags);

				final int edgeIndex =
					fluid ? edges.indexOf(source.x, source.y, source.z) : state.nextEdgeVertex();
				writeEdges(out, edges, edgeIndex);

				out += STRIDE;
			}

			return out;
		};
	}

	private static void writeEdges(long at, QuadEdges edges, int index) {
		if (index < 0) {
			MemoryUtil.memPutInt(at + EDGE_OFFSET, NO_EDGES_LE);
			MemoryUtil.memPutInt(at + EDGE_MASK_OFFSET, 0);
			for (int contact = 0; contact < CONTACTS; contact++) {
				MemoryUtil.memPutInt(at + CONTACT_OFFSET + contact * 4L, NO_CONTACT_LE[contact]);
			}
			return;
		}

		MemoryUtil.memPutInt(at + EDGE_OFFSET, Integer.reverseBytes(edges.get(index)));
		MemoryUtil.memPutInt(at + EDGE_MASK_OFFSET, Integer.reverseBytes(edges.mask(index)));
		for (int contact = 0; contact < CONTACTS; contact++) {
			MemoryUtil.memPutInt(
				at + CONTACT_OFFSET + contact * 4L, Integer.reverseBytes(edges.contact(contact)));
		}
	}

	private static int toAbgr(int argb) {
		return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
	}
}
