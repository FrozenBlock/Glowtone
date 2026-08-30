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

package net.frozenblock.glowtone.mixin.client.material;

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@ClientOnly
@Mixin(MovingBlockFeatureRenderer.class)
public class MovingBlockFeatureRendererMaterialMixin {

	@Inject(method = "buildGroup", at = @At("HEAD"))
	private void glowtone$openMovingBlockMaterial(
		FeatureFrameContext context,
		List<MovingBlockFeatureRenderer.Submit> submits,
		CallbackInfo info
	) {
		BlockMaterials.beginShaderIndex(BlockMaterials.NO_SHADER);
	}

	@Inject(
		method = "buildGroup",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/MovingBlockFeatureRenderer$Submit;movingBlockRenderState()Lnet/minecraft/client/renderer/block/MovingBlockRenderState;",
			shift = At.Shift.AFTER
		)
	)
	private void glowtone$beginMovingBlockMaterial(
		FeatureFrameContext context,
		List<MovingBlockFeatureRenderer.Submit> submits,
		CallbackInfo info,
		@Local(name = "submit") MovingBlockFeatureRenderer.Submit submit
	) {
		BlockMaterials.setShaderIndex(BlockMaterials.shaderIndexFor(submit.movingBlockRenderState().blockState));
	}

	@Inject(
		method = "putBakedQuad",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
		)
	)
	private void glowtone$traceMovingBlockQuad(
		CallbackInfo info,
		@Local(argsOnly = true) QuadInstance instance
	) {
	}

	@Inject(method = "buildGroup", at = @At("RETURN"))
	private void glowtone$endMovingBlockMaterial(CallbackInfo info) {
		BlockMaterials.endBlock();
	}
}
