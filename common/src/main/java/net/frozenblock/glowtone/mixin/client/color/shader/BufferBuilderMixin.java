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

package net.frozenblock.glowtone.mixin.client.color.shader;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.vertex.GTDefaultVertexFormat;
import net.frozenblock.glowtone.render.vertex.GlowtoneBufferBuilder;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexLayout;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;

// TODO: self emission to offset tint with
@ClientOnly
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements GlowtoneBufferBuilder {
	@Unique
	private Kind glowtone$kind;
	@Unique
	private GlowtoneVertexLayout glowtone$layout;

	@Inject(
		method = "<init>(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/PrimitiveTopology;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
		at = @At("RETURN")
	)
	private void glowtone$snapshotLayout(ByteBufferBuilder buffer, PrimitiveTopology topology, VertexFormat format, CallbackInfo info) {
		final Kind kind;
		if (format == DefaultVertexFormat.BLOCK) {
			kind = Kind.BLOCK;
		} else if (format == DefaultVertexFormat.ENTITY) {
			kind = Kind.ENTITY;
		} else if (format == GTDefaultVertexFormat.tinted()) {
			kind = Kind.TINTED;
		} else {
			kind = Kind.NONE;
		}
		final GlowtoneVertexLayout layout = kind == Kind.NONE ? GlowtoneVertexLayout.NONE : GTDefaultVertexFormat.layoutOf(format);
		this.glowtone$layout = layout;
		this.glowtone$kind = layout.isEmpty() ? Kind.NONE : kind;
	}

	@Override
	public Kind glowtone$kind() {
		final Kind kind = this.glowtone$kind;
		return kind != null ? kind : Kind.NONE;
	}

	@Override
	public GlowtoneVertexLayout glowtone$layout() {
		final GlowtoneVertexLayout layout = this.glowtone$layout;
		return layout != null ? layout : GlowtoneVertexLayout.NONE;
	}

	@ModifyExpressionValue(
		method = "addVertex(FFFIFFIIFFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;beginVertex()J"
		)
	)
	private long glowtone$writeBlockOrEntityExtensions(
		long original,
		float x, float y, float z,
		int color,
		float u, float v,
		int overlayCoords,
		int lightCoords,
		float nx, float ny, float nz
	) {
		switch (this.glowtone$kind()) {
			case BLOCK -> glowtone$writeBlockExtensions(original, this.glowtone$layout, x, y, z);
			case ENTITY -> glowtone$writeEntityChromaExtension(original, this.glowtone$layout);
			default -> { }
		}

		return original;
	}

	@ModifyExpressionValue(
		method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;beginVertex()J",
			ordinal = 0
		)
	)
	private long glowtone$writeChromaAny(
		long original,
		float x, float y, float z
	) {
		switch (this.glowtone$kind()) {
			case BLOCK -> glowtone$writeBlockExtensions(original, this.glowtone$layout, x, y, z);
			case ENTITY -> glowtone$writeEntityChromaExtension(original, this.glowtone$layout);
			case TINTED -> glowtone$writePositionColorLightmapTintedChromaExtension(original, this.glowtone$layout);
			default -> { }
		}
		return original;
	}

	@Unique
	private static void glowtone$writeBlockExtensions(long pointer, GlowtoneVertexLayout layout, float x, float y, float z) {
		final ChromaBaker.SectionState state = ChromaBaker.state();

		if (layout.hasChroma()) {
			if (state.building() && ChromaBlender.isEnabled()) {
				state.rotateFlatPins();
				glowtone$writeARGB(pointer + layout.chroma(), state.sample(x, y, z));
				glowtone$writeARGB(pointer + layout.skyChroma(), state.sampleSky(x, y, z));
			} else {
				glowtone$writeARGB(pointer + layout.chroma(), ChromaBaker.NEUTRAL_ARGB);
				glowtone$writeARGB(pointer + layout.skyChroma(), ChromaBaker.NEUTRAL_SKY_ARGB);
			}

			if (state.building() && MaterialShaderPatcher.anyQuadOffset()) {
				final int corner = state.nextQuadOffset();
				if (corner >= 0) {
					MemoryUtil.memPutByte(pointer + layout.chroma() + 3L, MaterialShaderPatcher.encodeQuadOffset(state.quadOffsetX(corner)));
					MemoryUtil.memPutByte(pointer + layout.skyChroma() + 3L, MaterialShaderPatcher.encodeQuadOffset(state.quadOffsetZ(corner)));
					if (layout.hasPivot()) {
						MemoryUtil.memPutByte(pointer + layout.pivot(), MaterialShaderPatcher.encodeQuadOffset(state.quadOffsetY(corner)));
					}
				}
			}
		}

		if (layout.hasEdges()) glowtone$writeBlockEdgesExtension(pointer, layout, state, x, y, z);
	}

	@Unique
	private static void glowtone$writeBlockEdgesExtension(long pointer, GlowtoneVertexLayout layout, ChromaBaker.SectionState state, float x, float y, float z) {
		final QuadEdges edges = state.pendingEdges();
		final boolean fluid = state.fluidQuad();
		final int index = fluid ? edges.indexOf(x, y, z) : state.nextEdgeVertex();

		glowtone$writeRaw(pointer + layout.edge(), index < 0 ? QuadEdges.NO_EDGES : edges.get(index));
		glowtone$writeRaw(pointer + layout.edgeMask(), index < 0 ? 0 : edges.mask(index));
		for (int contact = 0; contact < 4; contact++) {
			glowtone$writeRaw(pointer + layout.contact(contact), index < 0 ? GlowtoneContactRects.NONE[contact] : edges.contact(contact));
		}
	}

	@Unique
	private static void glowtone$writeEntityChromaExtension(long pointer, GlowtoneVertexLayout layout) {
		if (!layout.hasChroma()) return;

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(pointer + layout.chroma(), ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(pointer + layout.skyChroma(), ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		glowtone$writeARGB(pointer + layout.chroma(), ChromaFold.modelTintColor());
		glowtone$writeARGB(pointer + layout.skyChroma(), ChromaFold.modelSkyTintColor());
	}

	@Unique
	private static void glowtone$writePositionColorLightmapTintedChromaExtension(long pointer, GlowtoneVertexLayout layout) {
		if (!layout.hasChroma()) return;

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(pointer + layout.chroma(), ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(pointer + layout.skyChroma(), ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		glowtone$writeARGB(pointer + layout.chroma(), ChromaFold.shaderChroma(ChromaFold.currentSubmitTint()));
		glowtone$writeARGB(pointer + layout.skyChroma(), ChromaBaker.NEUTRAL_SKY_ARGB);
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
