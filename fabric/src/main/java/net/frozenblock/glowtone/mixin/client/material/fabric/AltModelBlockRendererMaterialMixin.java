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

package net.frozenblock.glowtone.mixin.client.material.fabric;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// Indigo replaces vanilla's block renderer, so terrain never reaches ModelBlockRenderer, BlockModelLighter or QuadInstance I guess.
@Pseudo
@ClientOnly
@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererMaterialMixin {

	@ModifyReturnValue(method = "transform", at = @At("RETURN"))
	private boolean glowtone$markMaterialQuad(boolean original, MutableQuadView quad) {
		if (!original || !BlockMaterialRenderer.anyShaders()) return original;

		final float u = (quad.u(0) + quad.u(1) + quad.u(2) + quad.u(3)) * 0.25F;
		final float v = (quad.v(0) + quad.v(1) + quad.v(2) + quad.v(3)) * 0.25F;

		final int shaderIndex = BlockMaterialRenderer.indexForAtlasCoord(u, v);
		if (shaderIndex == BlockMaterialRenderer.NO_SHADER) return original;

		for (int vertex = 0; vertex < 4; vertex++) {
			quad.lightmap(vertex, BlockMaterialRenderer.markShaderIndex(quad.lightmap(vertex), shaderIndex));
		}

		return original;
	}
}
