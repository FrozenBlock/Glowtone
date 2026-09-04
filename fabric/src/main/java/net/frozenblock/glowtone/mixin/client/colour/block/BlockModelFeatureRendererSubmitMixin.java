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

package net.frozenblock.glowtone.mixin.client.colour.block;

import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.color.render.impl.BlockLightTinted;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(BlockModelFeatureRenderer.Submit.class)
public class BlockModelFeatureRendererSubmitMixin implements BlockLightTinted {
	@Unique
	private int glowtone$blockLightTint;

	@Unique
	@Override
	public int glowtone$blockLightTint() {
		return this.glowtone$blockLightTint;
	}

	@Unique
	@Override
	public void glowtone$setBlockLightTint(int tint) {
		this.glowtone$blockLightTint = tint;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowtone$captureBlockLightTint(CallbackInfo info) {
		this.glowtone$blockLightTint = ChromaFold.currentTint();
	}
}
