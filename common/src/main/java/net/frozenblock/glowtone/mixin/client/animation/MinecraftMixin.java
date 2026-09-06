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

import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@Inject(
		method = "renderFrame",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;extract(Lnet/minecraft/client/DeltaTracker;Z)V"
		)
	)
	private void glowtone$smoothInterpolatedAnimations(boolean advanceGameTime, CallbackInfo info) {
		if (!GlowtoneConfig.smoothAnimation()) return;

		final Minecraft minecraft = Minecraft.class.cast(this);
		final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		minecraft.getAtlasManager().forEach((id, atlas) -> glowtone$refreshAtlas(atlas, partialTick));
	}

	@Unique
	private static void glowtone$refreshAtlas(TextureAtlas atlas, float partialTick) {
		boolean interpolatedAny = false;
		for (final SpriteContents.AnimationState state : atlas.animatedTexturesStates) {
			if (!state.animationInfo.interpolateFrames) continue;

			state.glowtone$setPartialTick(partialTick);
			interpolatedAny = true;
		}
		if (!interpolatedAny) return;

		((TextureAtlasInvoker)atlas).glowtone$uploadAnimationFrames();
	}
}
