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

package net.frozenblock.glowtone.animation;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.RenderPipelines;

@Environment(EnvType.CLIENT)
public final class GlowtoneAnimationPipelines {
	public static final RenderPipeline.Snippet TERRAIN_FOLIAGE_SNIPPET = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
		.withVertexShader(GlowtoneAnimationShaders.createTerrainAnimationShaderId("foliage"))
		.buildSnippet();
	public static final RenderPipeline CUTOUT_TERRAIN_FOLIAGE = RenderPipelines.register(
		RenderPipeline.builder(TERRAIN_FOLIAGE_SNIPPET)
			.withLocation(GlowtoneConstants.id("pipeline/animation/cutout_terrain_foliage"))
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build()
	);

	public static final RenderPipeline.Snippet TERRAIN_FIRE_SNIPPET = RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
		.withVertexShader(GlowtoneAnimationShaders.createTerrainAnimationShaderId("fire"))
		.buildSnippet();
	public static final RenderPipeline CUTOUT_TERRAIN_FIRE = RenderPipelines.register(
		RenderPipeline.builder(TERRAIN_FIRE_SNIPPET)
			.withLocation(GlowtoneConstants.id("pipeline/animation/cutout_terrain_fire"))
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build()
	);
}
