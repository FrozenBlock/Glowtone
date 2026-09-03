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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@ClientOnly
@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {

	// The way I've set up these injects may seem counterintuitive.
	// The reason I'm doing it this way is so we'll receive an error if the name of one of these pipelines changes.

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "CONSTANT",
				args = "stringValue=pipeline/solid_terrain"
			)
		)
	)
	private static RenderPipeline glowtone$patchSolidTerrain(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
		instance.withShaderDefine(EmissiveShaderPatcher.SHADED_TERRAIN_DEFINE);
		instance.withShaderDefine(EmissiveShaderPatcher.OPAQUE_TERRAIN_DEFINE);
		return original.call(instance);
	}

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "CONSTANT",
				args = "stringValue=pipeline/cutout_terrain"
			)
		)
	)
	private static RenderPipeline glowtone$patchCutoutTerrain(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
		instance.withShaderDefine(EmissiveShaderPatcher.SHADED_TERRAIN_DEFINE);
		instance.withShaderDefine(EmissiveShaderPatcher.OPAQUE_TERRAIN_DEFINE);
		return original.call(instance);
	}

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "CONSTANT",
				args = "stringValue=pipeline/translucent_terrain"
			)
		)
	)
	private static RenderPipeline glowtone$patchTranslucentTerrain(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
		instance.withShaderDefine(EmissiveShaderPatcher.SHADED_TERRAIN_DEFINE);
		instance.withShaderDefine(EmissiveShaderPatcher.TRANSLUCENT_TERRAIN_DEFINE);
		return original.call(instance);
	}
}
