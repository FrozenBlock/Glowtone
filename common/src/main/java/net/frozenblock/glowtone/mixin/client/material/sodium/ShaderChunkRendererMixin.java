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

package net.frozenblock.glowtone.mixin.client.material.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.frozenblock.glowtone.material.MaterialBlockTextures;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@ClientOnly
@Mixin(ShaderChunkRenderer.class)
public class ShaderChunkRendererMixin {

	@WrapOperation(
		method = "createShader",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
		)
	)
	private RenderPipeline glowtone$declareMaterialSamplers(RenderPipeline.Builder instance, Operation<RenderPipeline> original) {
		instance.withBindGroupLayout(MaterialSamplers.LAYOUT);
		instance.withBindGroupLayout(MaterialBlockTextures.LAYOUT);
		instance.withBindGroupLayout(BindGroupLayouts.GLOBALS);
		return original.call(instance);
	}
}
