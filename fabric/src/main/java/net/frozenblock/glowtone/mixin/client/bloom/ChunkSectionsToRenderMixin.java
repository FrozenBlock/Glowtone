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

package net.frozenblock.glowtone.mixin.client.bloom;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.frozenblock.glowtone.bloom.GlowtoneBloomRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

	@WrapOperation(
		method = "renderGroup",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
		)
	)
	private RenderPass glowtone$attachEmissiveTarget(
		CommandEncoder encoder,
		Supplier<String> label,
		GpuTextureView colorTexture,
		Optional<Vector4fc> clearColor,
		GpuTextureView depthTexture,
		OptionalDouble clearDepth,
		Operation<RenderPass> original
	) {
		final RenderPass emissivePass = GlowtoneBloomRenderer.createEmissiveRenderPass(encoder, label, colorTexture, clearColor, depthTexture, clearDepth);
		return emissivePass != null ? emissivePass : original.call(encoder, label, colorTexture, clearColor, depthTexture, clearDepth);
	}

	@WrapOperation(
		method = "renderGroup",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
		)
	)
	private void glowtone$useEmissivePipeline(RenderPass instance, RenderPipeline pipeline, Operation<Void> original) {
		original.call(instance, GlowtoneBloomRenderer.pipelineFor(pipeline));
	}
}
