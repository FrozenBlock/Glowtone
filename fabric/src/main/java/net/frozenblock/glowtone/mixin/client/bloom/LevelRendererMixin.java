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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.bloom.GlowtoneBloomRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@Shadow
	@Final
	private GameRenderer gameRenderer;

	@Inject(method = "render", at = @At("HEAD"))
	private void glowtone$prepareBloom(CallbackInfo info) {
		final RenderTarget mainTarget = this.gameRenderer.mainRenderTarget();
		GlowtoneBloomRenderer.beginFrame(mainTarget.width, mainTarget.height);
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void glowtone$closeBloom(CallbackInfo info) {
		GlowtoneBloomRenderer.free();
	}
}
