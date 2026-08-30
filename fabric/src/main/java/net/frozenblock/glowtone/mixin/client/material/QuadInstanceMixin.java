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

package net.frozenblock.glowtone.mixin.client.material;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(QuadInstance.class)
public class QuadInstanceMixin {

	@ModifyReturnValue(method = "getLightCoords(I)I", at = @At("RETURN"))
	private int glowtone$markLightCoords(int lightCoords) {
		return BlockMaterialRenderer.markQuad(lightCoords);
	}

	@ModifyReturnValue(method = "getLightCoordsWithEmission(II)I", at = @At("RETURN"))
	private int glowtone$markLightCoordsWithEmission(int lightCoords) {
		return BlockMaterialRenderer.markQuad(lightCoords);
	}
}
