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
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@ClientOnly
public final class MaterialBlockTextures {
	public static final String TABLE = "GlowtoneBlockTexTable";
	public static final int HEADER = BlockMaterialRenderer.MAX_SHADER_INDEX + 1;
	private static final int TEXEL_BYTES = 16;

	public static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
		.withUniform(TABLE, UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
		.build();

	public record Variant(int materialCase, List<BlockTextureSlots.Slot> rectangles) {}

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
		int rectangles = 0;
		for (Variant variant : table) rectangles += variant.rectangles().size();

		final ByteBuffer data = MemoryUtil.memAlloc((HEADER + rectangles) * TEXEL_BYTES);
		try {
			int base = HEADER;
			for (int index = 0; index < HEADER; index++) {
				if (index < 1 || index > table.size()) {
					putTexel(data, 0F, 0F, 0F, 0F);
					continue;
				}

				final Variant variant = table.get(index - 1);
				putTexel(data, variant.materialCase(), base, variant.rectangles().size(), 0F);
				base += variant.rectangles().size();
			}

			for (Variant variant : table) {
				for (BlockTextureSlots.Slot slot : variant.rectangles()) {
					if (slot == null) {
						putTexel(data, 0F, 0F, 0F, 0F);
					} else {
						putTexel(data, slot.u0(), slot.u1(), slot.v0(), slot.v1());
					}
				}
			}

			data.flip();
			buffer = RenderSystem.getDevice().createBuffer(() -> "Glowtone block texture table", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER, data);
		} finally {
			MemoryUtil.memFree(data);
		}

		return buffer;
	}

	private static void putTexel(ByteBuffer data, float x, float y, float z, float w) {
		data.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
	}

	private MaterialBlockTextures() {}
}
