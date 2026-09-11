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

package net.frozenblock.glowtone.material;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@ClientOnly
public final class MaterialBlockTextures {
	public static final String TABLE = "GlowtoneBlockTexTable";
	private static final int TEXEL_BYTES = 16;

	public static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
		.withUniform(TABLE, UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
		.build();

	public record Variant(int materialCase, List<BlockTextureSlots.Slot> rectangles, float[] parameters) {
		public int texels() {
			return this.rectangles.size() + (this.parameters.length + 3) / 4;
		}
	}

	private static volatile List<Variant> variants = List.of();
	private static volatile @Nullable GpuBuffer buffer;

	public static void apply(List<Variant> assigned) {
		variants = List.copyOf(assigned);
		invalidate();
	}

	public static int variantCount(int materialCase) {
		int count = 0;
		for (Variant variant : variants) {
			if (variant.materialCase() == materialCase) count++;
		}

		return count;
	}

	public static String declarations() {
		return "uniform samplerBuffer " + TABLE + ";" + System.lineSeparator() + System.lineSeparator();
	}

	public static String fetch(String texel) {
		return "texelFetch(" + TABLE + ", " + texel + ")";
	}

	public static void invalidate() {
		final GpuBuffer stale = buffer;
		buffer = null;
		if (stale == null) return;

		if (RenderSystem.isOnRenderThread()) {
			stale.close();
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null) minecraft.execute(stale::close);
	}

	public static void bind(RenderPassBackend pass) {
		final GpuBuffer bound = buffer();
		if (bound != null) pass.setUniform(TABLE, bound);
	}

	private static @Nullable GpuBuffer buffer() {
		final GpuBuffer cached = buffer;
		if (cached != null) return cached;
		if (RenderSystem.getDevice() == null) return null;

		final List<Variant> table = variants;
		final int header = table.size() + 1;
		int texels = 0;
		for (Variant variant : table) texels += variant.texels();

		final ByteBuffer data = MemoryUtil.memAlloc((header + texels) * TEXEL_BYTES);
		try {
			putTexel(data, 0F, 0F, 0F, 0F);
			int base = header;
			for (Variant variant : table) {
				putTexel(data, variant.materialCase(), base, variant.rectangles().size(), variant.parameters().length);
				base += variant.texels();
			}

			for (Variant variant : table) {
				for (BlockTextureSlots.Slot slot : variant.rectangles()) {
					if (slot == null) {
						putTexel(data, 0F, 0F, 0F, 0F);
					} else {
						putTexel(data, slot.u0(), slot.u1(), slot.v0(), slot.v1());
					}
				}

				final float[] parameters = variant.parameters();
				for (int at = 0; at < parameters.length; at += 4) {
					putTexel(data, parameters[at], component(parameters, at + 1), component(parameters, at + 2), component(parameters, at + 3));
				}
			}

			data.flip();
			buffer = RenderSystem.getDevice().createBuffer(() -> "Glowtone block texture table", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER, data);
		} finally {
			MemoryUtil.memFree(data);
		}

		return buffer;
	}

	private static float component(float[] parameters, int at) {
		return at < parameters.length ? parameters[at] : 0F;
	}

	private static void putTexel(ByteBuffer data, float x, float y, float z, float w) {
		data.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
	}

	private MaterialBlockTextures() {}
}
