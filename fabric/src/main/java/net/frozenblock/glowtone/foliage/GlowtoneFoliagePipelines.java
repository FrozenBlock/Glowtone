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

package net.frozenblock.glowtone.foliage;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;

@Environment(EnvType.CLIENT)
public final class GlowtoneFoliagePipelines {
	public static final RenderPipeline.Snippet TERRAIN_SNIPPET = RenderPipeline.builder(RenderPipelines.GENERIC_BLOCKS_SNIPPET)
		.withBindGroupLayout(BindGroupLayouts.PROJECTION)
		.withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
		.withVertexShader(GlowtoneConstants.id("core/terrain_foliage"))
		.withFragmentShader("core/terrain")
		.buildSnippet();

	public static final RenderPipeline CUTOUT_TERRAIN = RenderPipelines.register(
		RenderPipeline.builder(TERRAIN_SNIPPET)
			.withLocation(GlowtoneConstants.id("pipeline/cutout_terrain_glowtone_foliage"))
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build()
	);
}
