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

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import java.util.List;

@ClientOnly
public final class MaterialSamplers {
	public static final int SLOTS = 4;
	private static final String PREFIX = "GlowtoneMaterialTex";

	public static final BindGroupLayout LAYOUT = layout();

	private static volatile List<Identifier> textures = List.of();

	// Rebuilt only by apply(), which every resource reload runs, restitched atlas cannot go stale.
	private static @Nullable GpuTextureView[] resolved;

	private static BindGroupLayout layout() {
		final BindGroupLayout.Builder builder = BindGroupLayout.builder();
		for (int slot = 0; slot < SLOTS; slot++) builder.withSampler(name(slot));
		return builder.build();
	}

	public static String name(int slot) {
		return PREFIX + slot;
	}

	public static void apply(List<Identifier> assigned) {
		textures = List.copyOf(assigned);
		resolved = null;
	}

	public static List<Identifier> textures() {
		return textures;
	}

	public static String declarations() {
		final StringBuilder builder = new StringBuilder();
		for (int slot = 0; slot < SLOTS; slot++) {
			builder.append("uniform sampler2D ").append(name(slot)).append(";\n");
		}

		builder.append("\nfloat glowtone_keepSamplers() {\n\treturn 1.0e-20 * float(0");
		for (int slot = 0; slot < SLOTS; slot++) {
			builder.append(" + textureSize(").append(name(slot)).append(", 0).x");
		}
		builder.append(");\n}\n\n");

		return builder.toString();
	}

	public static void bind(RenderPassBackend pass) {
		final GpuTextureView[] views = resolve();
		if (views == null) return;

		final GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
		for (int slot = 0; slot < SLOTS; slot++) pass.bindTexture(name(slot), views[slot], sampler);
	}

	private static @Nullable GpuTextureView[] resolve() {
		final GpuTextureView[] cached = resolved;
		if (cached != null) return cached;

		final GpuTextureView fallback = view(TextureAtlas.LOCATION_BLOCKS);
		if (fallback == null) return null;

		final List<Identifier> assigned = textures;
		final GpuTextureView[] views = new GpuTextureView[SLOTS];
		for (int slot = 0; slot < SLOTS; slot++) {
			final GpuTextureView view = slot < assigned.size() ? view(assigned.get(slot)) : null;
			views[slot] = view != null ? view : fallback;
		}

		resolved = views;
		return views;
	}

	private static @Nullable GpuTextureView view(Identifier texture) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getTextureManager() == null) return null;

		try {
			final AbstractTexture loaded = minecraft.getTextureManager().getTexture(texture);
			return loaded == null ? null : loaded.getTextureView();
		} catch (Exception e) {
			return null;
		}
	}

	private MaterialSamplers() {}
}
