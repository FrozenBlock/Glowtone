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

package net.frozenblock.glowtone.mixin.client.colour;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.frozenblock.glowtone.render.GlowtoneChromaBake;
import net.frozenblock.glowtone.render.GlowtoneChromaBlend;
import net.frozenblock.glowtone.render.GlowtoneVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
	@Shadow
	@Final
	private VertexFormat format;

	@Shadow
	private long vertexPointer;

	@Inject(method = "endLastVertex", at = @At("HEAD"))
	private void glowtone$writeChroma(CallbackInfo info) {
		if (this.format != GlowtoneVertexFormats.EXTENDED_BLOCK || this.vertexPointer == -1L) return;

		if (!GlowtoneChromaBlend.isEnabled()) {
			writeArgb(this.vertexPointer + GlowtoneVertexFormats.CHROMA_OFFSET, GlowtoneChromaBake.NEUTRAL_ARGB);
			writeArgb(this.vertexPointer + GlowtoneVertexFormats.SKY_CHROMA_OFFSET, GlowtoneChromaBake.NEUTRAL_SKY_ARGB);
			return;
		}

		final GlowtoneChromaBake.SectionState state = GlowtoneChromaBake.state();
		state.rotateFlatPins();

		final float x = MemoryUtil.memGetFloat(this.vertexPointer + GlowtoneVertexFormats.POSITION_OFFSET);
		final float y = MemoryUtil.memGetFloat(this.vertexPointer + GlowtoneVertexFormats.POSITION_OFFSET + 4L);
		final float z = MemoryUtil.memGetFloat(this.vertexPointer + GlowtoneVertexFormats.POSITION_OFFSET + 8L);

		writeArgb(this.vertexPointer + GlowtoneVertexFormats.CHROMA_OFFSET, state.sample(x, y, z));
		writeArgb(this.vertexPointer + GlowtoneVertexFormats.SKY_CHROMA_OFFSET, state.sampleSky(x, y, z));
	}

	@Unique
	private static void writeArgb(long at, int argb) {
		MemoryUtil.memPutByte(at, (byte) (argb >> 16));
		MemoryUtil.memPutByte(at + 1L, (byte) (argb >> 8));
		MemoryUtil.memPutByte(at + 2L, (byte) argb);
		MemoryUtil.memPutByte(at + 3L, (byte) (argb >> 24));
	}
}
