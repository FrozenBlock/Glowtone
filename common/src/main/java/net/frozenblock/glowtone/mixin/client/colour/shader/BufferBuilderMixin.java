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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.vertex.GTDefaultVertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// TODO: self emission to offset tint with
@ClientOnly
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
	@Shadow
	@Final
	private VertexFormat format;

	@Shadow
	@Final
	private boolean blockFormat;

	@Shadow
	@Final
	private boolean entityFormat;

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
		if (this.blockFormat) {
			glowtone$writeBlockChromaExtension(original, x, y, z);
			glowtone$writeBlockEdgesExtension(original, x, y, z);
		} else if (this.entityFormat) {
			glowtone$writeEntityChromaExtension(original);
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
		if (this.format == DefaultVertexFormat.BLOCK) {
			glowtone$writeBlockChromaExtension(original, x, y, z);
			glowtone$writeBlockEdgesExtension(original, x, y, z);
		} else if (this.format == DefaultVertexFormat.ENTITY) {
			glowtone$writeEntityChromaExtension(original);
		} else if (this.format == GTDefaultVertexFormat.POSITION_COLOR_LIGHTMAP_TINTED) {
			glowtone$writePositionColorLightmapTintedChromaExtension(original);
		}
		return original;
	}

	@Unique
	private static void glowtone$writeBlockChromaExtension(long pointer, float x, float y, float z) {
		//if (pointer == -1L) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.rotateFlatPins();

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_BLOCK, ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_BLOCK, ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_BLOCK, state.sample(x, y, z));
		glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_BLOCK, state.sampleSky(x, y, z));
	}

	@Unique
	private static void glowtone$writeBlockEdgesExtension(long pointer, float x, float y, float z) {
		//if (pointer == -1L) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		final QuadEdges edges = state.pendingEdges();
		final boolean fluid = state.fluidQuad();
		final int index = fluid ? edges.indexOf(x, y, z) : state.nextEdgeVertex();

		glowtone$writeRaw(pointer + GTDefaultVertexFormat.EDGE_OFFSET_BLOCK, index < 0 ? QuadEdges.NO_EDGES : edges.get(index));
		glowtone$writeRaw(pointer + GTDefaultVertexFormat.EDGE_MASK_OFFSET_BLOCK, index < 0 ? 0 : edges.mask(index));
		glowtone$writeRaw(pointer + GTDefaultVertexFormat.CONTACT0_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[0] : edges.contact(0));
		glowtone$writeRaw(pointer + GTDefaultVertexFormat.CONTACT1_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[1] : edges.contact(1));
		glowtone$writeRaw(pointer + GTDefaultVertexFormat.CONTACT2_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[2] : edges.contact(2));
		glowtone$writeRaw(pointer + GTDefaultVertexFormat.CONTACT3_OFFSET_BLOCK, index < 0 ? GlowtoneContactRects.NONE[3] : edges.contact(3));
	}

	@Unique
	private static void glowtone$writeEntityChromaExtension(long pointer) {
		//if (pointer == -1L) return;

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_ENTITY, ChromaFold.modelTintColor());
		glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_ENTITY, ChromaBaker.NEUTRAL_SKY_ARGB);
	}

	@Unique
	private static void glowtone$writePositionColorLightmapTintedChromaExtension(long pointer) {
		//if (pointer == -1L) return;

		if (!ChromaBlender.isEnabled()) {
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_POSITION_COLOR_LIGHTMAP_TINTED, ChromaBaker.NEUTRAL_ARGB);
			glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_POSITION_COLOR_LIGHTMAP_TINTED, ChromaBaker.NEUTRAL_SKY_ARGB);
			return;
		}

		glowtone$writeARGB(pointer + GTDefaultVertexFormat.CHROMA_OFFSET_POSITION_COLOR_LIGHTMAP_TINTED, ChromaFold.currentSubmitTint());
		glowtone$writeARGB(pointer + GTDefaultVertexFormat.SKY_CHROMA_OFFSET_POSITION_COLOR_LIGHTMAP_TINTED, ChromaBaker.NEUTRAL_SKY_ARGB);
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
