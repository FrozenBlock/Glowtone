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

package net.frozenblock.glowtone.mixin.client.colour.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.vertex.GTDefaultVertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: self emission to offset tint with
@ClientOnly
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
	@Shadow
	@Final
	private VertexFormat format;

	@Shadow
	private long vertexPointer;

	@Inject(method = "endLastVertex", at = @At("HEAD"))
	private void glowtone$writeChromaBlock(CallbackInfo info) {
		if (this.format != DefaultVertexFormat.BLOCK || this.vertexPointer == -1L) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.rotateFlatPins();

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.CHROMA_OFFSET_BLOCK, ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_BLOCK, ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		final float x = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_BLOCK);
		final float y = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_BLOCK + 4L);
		final float z = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_BLOCK + 8L);

		glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.CHROMA_OFFSET_BLOCK, state.sample(x, y, z));
		glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_BLOCK, state.sampleSky(x, y, z));
	}

	@Inject(method = "endLastVertex", at = @At("HEAD"))
	private void glowtone$writeChromaModel(CallbackInfo info) {
		if (this.format != DefaultVertexFormat.ENTITY || this.vertexPointer == -1L) return;

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		final float x = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_ENTITY);
		final float y = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_ENTITY + 4L);
		final float z = MemoryUtil.memGetFloat(this.vertexPointer + GTDefaultVertexFormat.POSITION_OFFSET_ENTITY + 8L);

		// TODO: EntityState? Or something else?
		glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.CHROMA_OFFSET_ENTITY, ChromaFold.modelTintColor());
		// TODO: EntityState? Or something else?
		glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_SKY_ARGB);
		//glowtone$writeARGB(this.vertexPointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_ENTITY, state.sampleSky(x, y, z));
	}

	@Inject(method = "addVertex(FFFIFFIIFFF)V", at = @At("RETURN"))
	private void glowtone$writeEdges(
		float x, float y, float z,
		int color,
		float u, float v,
		int overlayCoords,
		int lightCoords,
		float nx, float ny, float nz,
		CallbackInfo info
	) {
		if (this.format != DefaultVertexFormat.BLOCK || this.vertexPointer == -1L) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		final QuadEdges edges = state.pendingEdges();
		final boolean fluid = state.fluidQuad();
		final int index = fluid ? edges.indexOf(x, y, z) : state.nextEdgeVertex();

		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.EDGE_OFFSET_BLOCK, index < 0 ? QuadEdges.NO_EDGES : edges.get(index));
		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.EDGE_MASK_OFFSET_BLOCK, index < 0 ? 0 : edges.mask(index));
		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.CONTACT0_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[0] : edges.contact(0));
		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.CONTACT1_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[1] : edges.contact(1));
		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.CONTACT2_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[2] : edges.contact(2));
		glowtone$writeRaw(this.vertexPointer + GTDefaultVertexFormat.CONTACT3_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[3] : edges.contact(3));

	}

	@Unique
	private static void glowtone$writeRaw(long at, int packed) {
		MemoryUtil.memPutByte(at, (byte) (packed >> 24));
		MemoryUtil.memPutByte(at + 1L, (byte) (packed >> 16));
		MemoryUtil.memPutByte(at + 2L, (byte) (packed >> 8));
		MemoryUtil.memPutByte(at + 3L, (byte) packed);
	}

	@Unique
	private static void glowtone$writeARGB(long at, int argb) {
		MemoryUtil.memPutByte(at, (byte) (argb >> 16));
		MemoryUtil.memPutByte(at + 1L, (byte) (argb >> 8));
		MemoryUtil.memPutByte(at + 2L, (byte) argb);
		MemoryUtil.memPutByte(at + 3L, (byte) (argb >> 24));
	}
}
