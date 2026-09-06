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

package net.frozenblock.glowtone.mixin.client.material.shader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import java.util.Optional;
import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.frozenblock.glowtone.material.MaterialBlockTextures;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
		)
	)
	private static RenderPipeline glowtone$declareMaterialSamplers(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
		final Optional<Identifier> fragment = instance.fragmentShader;
		if (fragment != null && fragment.isPresent() && EmissiveShaderPatcher.usesMaterialSamplers(fragment.get())) {
			instance.withBindGroupLayout(MaterialSamplers.LAYOUT);
			instance.withBindGroupLayout(MaterialBlockTextures.LAYOUT);
			final Optional<List<BindGroupLayout>> layouts = instance.bindGroupLayouts;
			if (layouts == null || layouts.isEmpty() || !layouts.get().contains(BindGroupLayouts.GLOBALS)) {
				instance.withBindGroupLayout(BindGroupLayouts.GLOBALS);
			}
		}

		return original.call(instance);
	}
}
