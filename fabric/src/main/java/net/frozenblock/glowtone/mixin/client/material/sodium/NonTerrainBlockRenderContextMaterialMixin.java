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

package net.frozenblock.glowtone.mixin.client.material.sodium;

import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(NonTerrainBlockRenderContext.class)
public class NonTerrainBlockRenderContextMaterialMixin {

	@Inject(method = "processQuad", at = @At("RETURN"))
	private void glowtone$markNonTerrainMaterial(MutableQuadViewImpl quad, CallbackInfo info) {
		if (!BlockMaterials.anyShaders()) return;

		final int index = BlockMaterials.renderedShaderIndex();
		if (index == BlockMaterials.NO_SHADER) return;

		for (int vertex = 0; vertex < 4; vertex++) {
			quad.setLight(vertex, BlockMaterials.markShaderIndex(quad.getLight(vertex)));
		}
	}
}
