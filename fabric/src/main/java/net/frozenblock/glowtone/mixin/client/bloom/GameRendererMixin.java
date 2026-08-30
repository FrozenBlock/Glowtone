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

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.frozenblock.glowtone.bloom.GlowtoneBloomRenderer;
import net.frozenblock.glowtone.light.edge.render.EdgeRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Shadow
	@Final
	private RenderTarget mainRenderTarget;

	@Inject(
		method = "renderLevel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",
			shift = At.Shift.AFTER
		)
	)
	private void glowtone$compositeBloom(CallbackInfo info) {
		if (!GlowtoneBloomRenderer.isEnabled()) return;
		GlowtoneBloomRenderer.render(this.mainRenderTarget);
	}

	/** The next call clears depth for the held item. */
	@Inject(
		method = "renderLevel",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"
		)
	)
	private void glowtone$screenSpaceEdges(CallbackInfo info) {
		EdgeRenderer.render(this.mainRenderTarget);
	}
}
