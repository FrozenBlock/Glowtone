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

package net.frozenblock.glowtone.mixin.client.lighting;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.frozenblock.glowtone.lighting.GlowtoneLighting;
import net.frozenblock.glowtone.lighting.GlowtoneLightmap;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(Lightmap.class)
public class LightmapMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void glowtone$renderGradedLightmap(LightmapRenderState state, CallbackInfo info) {
		if (!GlowtoneLighting.gradingActive()) return;

		if (state.needsUpdate) GlowtoneLighting.renderGradedLightmap(state);
		info.cancel();
	}

	@Inject(method = "getTextureView", at = @At("RETURN"), cancellable = true)
	private void glowtone$useGradedLightmap(CallbackInfoReturnable<GpuTextureView> info) {
		if (GlowtoneLighting.gradingActive()) info.setReturnValue(GlowtoneLightmap.textureView());
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void glowtone$closeGradedLightmap(CallbackInfo info) {
		GlowtoneLightmap.close();
	}
}
