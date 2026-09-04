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

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

@ClientOnly
public final class MaterialBlockTextures {
	public static final int SLOTS = 16;
	public static final String BLOCK = "GlowtoneBlockTextures";
	public static final String ARRAY = "GlowtoneBlockTex";

	public static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
		.withUniform(BLOCK, UniformType.UNIFORM_BUFFER)
		.build();

	private static final int SIZE = size();

	private static volatile List<String> names = List.of();
	private static @Nullable GpuBuffer buffer;

	private static int size() {
		final Std140SizeCalculator calculator = new Std140SizeCalculator();
		for (int slot = 0; slot < SLOTS; slot++) calculator.putVec4();
		return calculator.get();
	}

	public static void apply(List<String> assigned) {
		names = assigned.stream().distinct().sorted().limit(SLOTS).toList();
		invalidate();
	}

	public static int indexOf(String name) {
		return names.indexOf(name);
	}

	public static String declarations() {
		return "layout(std140) uniform " + BLOCK + " {" + System.lineSeparator()
			+ "    vec4 " + ARRAY + "[" + SLOTS + "];" + System.lineSeparator()
			+ "};" + System.lineSeparator() + System.lineSeparator();
	}

	public static void invalidate() {
		final GpuBuffer stale = buffer;
		buffer = null;
		if (stale != null) stale.close();
	}

	public static void bind(RenderPassBackend pass) {
		final GpuBuffer bound = buffer();
		if (bound != null) pass.setUniform(BLOCK, bound);
	}

	private static @Nullable GpuBuffer buffer() {
		final GpuBuffer cached = buffer;
		if (cached != null) return cached;
		if (RenderSystem.getDevice() == null) return null;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final Std140Builder data = Std140Builder.onStack(stack, SIZE);
			for (int slot = 0; slot < SLOTS; slot++) {
				final BlockTextureSlots.Slot bounds = slot < names.size() ? BlockTextureSlots.get(names.get(slot)) : null;
				if (bounds == null) {
					data.putVec4(0F, 0F, 0F, 0F);
				} else {
					data.putVec4(bounds.u0(), bounds.u1(), bounds.v0(), bounds.v1());
				}
			}

			buffer = RenderSystem.getDevice().createBuffer(() -> "Glowtone block textures", GpuBuffer.USAGE_UNIFORM, data.get());
		}

		return buffer;
	}

	private MaterialBlockTextures() {}
}
