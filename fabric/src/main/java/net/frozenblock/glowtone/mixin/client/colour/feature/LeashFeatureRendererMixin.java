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

package net.frozenblock.glowtone.mixin.client.colour.feature;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(LeashFeatureRenderer.class)
public class LeashFeatureRendererMixin {

	@Inject(
		method = "addVertexPair",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/LightCoordsUtil;pack(II)I"
		)
	)
	private static void glowtone$beginLeash(
		VertexConsumer builder,
		Matrix4fc pose,
		float dx,
		float dy,
		float dz,
		float fudge,
		float dxOff,
		float dzOff,
		int k,
		boolean backwards,
		EntityRenderState.LeashState state,
		CallbackInfo info,
		@Local(name = "progress") float progress,
		@Share("glowtone$pushedTint") LocalBooleanRef pushedTint
	) {
		final int startTint = state.glowtone$blockLightTintA();
		final int endTint = state.glowtone$blockLightTintB();
		final int color = ARGB.srgbLerp(progress, startTint, endTint);
		ChromaFold.pushSubmitTint(color);
		pushedTint.set(true);
	}

	@Inject(method = "addVertexPair", at = @At("RETURN"))
	private static void glowtone$endLeash(
		CallbackInfo info,
		@Share("glowtone$pushedTint") LocalBooleanRef pushedTint
	) {
		if (pushedTint.get()) ChromaFold.popSubmitTint();
	}
}
