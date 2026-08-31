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

package net.frozenblock.glowtone.mixin.client.animation;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.animation.impl.AnimationStatePartialTickExtension;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@ClientOnly
@Mixin(SpriteContents.AnimationState.class)
public class AnimationStateMixin implements AnimationStatePartialTickExtension {
	@Shadow
	@Final
	private SpriteContents.AnimatedTexture animationInfo;

	@Unique
	private float glowtone$partialTick;

	@Unique
	private int glowtone$frameTime = 1;

	@Unique
	@Override
	public void glowtone$setPartialTick(float partialTick) {
		this.glowtone$partialTick = partialTick;
	}

	@ModifyExpressionValue(
		method = "drawToAtlas(Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/texture/SpriteContents$FrameInfo;time:I",
			opcode = Opcodes.GETFIELD
		)
	)
	private int glowtone$captureFrameTime(int time) {
		this.glowtone$frameTime = time;
		return time;
	}

	@ModifyVariable(
		method = "drawToAtlas(Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
		at = @At("STORE"),
		name = "frameProgress"
	)
	private float glowtone$smoothFrameProgress(float frameProgress) {
		if (!GlowtoneConfig.smoothAnimation() || !this.animationInfo.interpolateFrames || this.glowtone$frameTime <= 0) return frameProgress;
		return Math.min(1F, frameProgress + this.glowtone$partialTick / this.glowtone$frameTime);
	}
}
