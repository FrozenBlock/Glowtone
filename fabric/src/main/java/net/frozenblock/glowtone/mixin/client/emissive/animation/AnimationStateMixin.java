/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client.emissive.animation;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.render.animation.AnimationStatePartialTickExtension;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@ClientOnly
@Mixin(SpriteContents.AnimationState.class)
public class AnimationStateMixin implements AnimationStatePartialTickExtension {
	@Unique
	private float glowtone$partialTick;

	@Unique
	private boolean glowtone$interpolating;

	@Unique
	private int glowtone$frameTime = 1;

	@Unique
	@Override
	public void glowtone$setPartialTick(float partialTick) {
		this.glowtone$partialTick = partialTick;
	}

	@Unique
	@Override
	public boolean glowtone$isInterpolating() {
		return this.glowtone$interpolating;
	}

	@ModifyExpressionValue(
		method = "needsToDraw",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/texture/SpriteContents$AnimatedTexture;interpolateFrames:Z"
		)
	)
	private boolean glowtone$captureInterpolating(boolean interpolateFrames) {
		this.glowtone$interpolating = interpolateFrames;
		return interpolateFrames;
	}

	@ModifyExpressionValue(
		method = "drawToAtlas(Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/texture/SpriteContents$FrameInfo;time:I"
		)
	)
	private int glowtone$captureFrameTime(int time) {
		this.glowtone$frameTime = time;
		return time;
	}

	@ModifyVariable(
		method = "drawToAtlas(Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
		at = @At("STORE"),
		ordinal = 0
	)
	private float glowtone$smoothFrameProgress(float frameProgress) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !this.glowtone$interpolating || this.glowtone$frameTime <= 0) {
			return frameProgress;
		}

		return Math.min(1F, frameProgress + this.glowtone$partialTick / this.glowtone$frameTime);
	}
}
